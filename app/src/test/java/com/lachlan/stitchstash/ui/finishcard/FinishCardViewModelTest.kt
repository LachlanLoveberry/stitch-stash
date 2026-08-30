package com.lachlan.stitchstash.ui.finishcard

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.lachlan.stitchstash.data.db.entities.AppSettings
import com.lachlan.stitchstash.data.db.entities.Colourway
import com.lachlan.stitchstash.data.db.entities.Completion
import com.lachlan.stitchstash.data.db.entities.FinishCard
import com.lachlan.stitchstash.data.db.entities.Pattern
import com.lachlan.stitchstash.data.repository.StitchRepository
import com.lachlan.stitchstash.domain.model.CompletionWithContext
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FinishCardViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private fun completionContext(completionId: Long = 1L) = CompletionWithContext(
        completion = Completion(id = completionId, colourwayId = 1L, completedAtEpochDay = 0L, photoUri = "photo.jpg"),
        pattern = Pattern(id = 1L, name = "Sunflower"),
        colourway = Colourway(id = 1L, patternId = 1L, name = "Yellow", targetCount = 5),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        mockkObject(FinishCardRenderer)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkObject(FinishCardRenderer)
    }

    private fun repoMock(settings: AppSettings = AppSettings()): StitchRepository {
        val repo = mockk<StitchRepository>(relaxed = true)
        val settingsFlow = MutableStateFlow(settings)
        every { repo.observeSettings() } returns settingsFlow
        every { repo.observeFinishCards() } returns flowOf(emptyList())
        every { repo.observeRecentCompletionsWithContext(limit = 50) } returns flowOf(listOf(completionContext()))
        every { repo.observeTotalCompletionCount() } returns flowOf(3)
        coEvery { repo.updateSettings(any()) } coAnswers {
            settingsFlow.value = firstArg()
        }
        return repo
    }

    @Test
    fun `savedCards starts empty when repo has zero completions`() = runTest(dispatcher) {
        val repo = mockk<StitchRepository>(relaxed = true)
        every { repo.observeFinishCards() } returns flowOf(emptyList())
        val vm = FinishCardViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()

        assertThat(vm.savedCards.value).isEmpty()
        assertThat(vm.preview.value).isEqualTo(CardPreviewState())
    }

    @Test
    fun `prepare loads context and defaults border from settings`() = runTest(dispatcher) {
        val repo = repoMock(settings = AppSettings(finishCardBorderStyle = "granny"))
        coEvery { FinishCardRenderer.render(any(), any()) } returns "/path/card.png"
        val context = mockk<Context>(relaxed = true)
        val vm = FinishCardViewModel(repo)

        vm.prepare(context, completionId = 1L)
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.preview.value
        assertThat(state.border).isEqualTo(BorderStyle.GRANNY)
        assertThat(state.patternName).isEqualTo("Sunflower")
        assertThat(state.colourwayName).isEqualTo("Yellow")
        assertThat(state.pieceNumber).isEqualTo(3)
        assertThat(state.photoPath).isEqualTo("photo.jpg")
        assertThat(state.imagePath).isEqualTo("/path/card.png")
        assertThat(state.rendering).isFalse()
    }

    @Test
    fun `changeBorder persists new border via repo updateSettings and re-renders`() = runTest(dispatcher) {
        val repo = repoMock(settings = AppSettings(finishCardBorderStyle = "floral"))
        coEvery { FinishCardRenderer.render(any(), any()) } returns "/path/card.png"
        val context = mockk<Context>(relaxed = true)
        val vm = FinishCardViewModel(repo)
        vm.prepare(context, completionId = 1L)
        dispatcher.scheduler.advanceUntilIdle()

        val settingsSlot = slot<AppSettings>()
        coEvery { repo.updateSettings(capture(settingsSlot)) } returns Unit

        vm.changeBorder(context, completionId = 1L, border = BorderStyle.SCALLOP)
        dispatcher.scheduler.advanceUntilIdle()

        assertThat(vm.preview.value.border).isEqualTo(BorderStyle.SCALLOP)
        assertThat(vm.preview.value.saved).isFalse()
        assertThat(settingsSlot.captured.finishCardBorderStyle).isEqualTo(BorderStyle.SCALLOP.key)
        coVerify { repo.updateSettings(match { it.finishCardBorderStyle == "scallop" }) }
    }

    @Test
    fun `saveCurrent does nothing when no image has been rendered yet`() = runTest(dispatcher) {
        val repo = repoMock()
        val vm = FinishCardViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()

        vm.saveCurrent(completionId = 1L)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { repo.addFinishCard(any()) }
        assertThat(vm.preview.value.saved).isFalse()
    }

    @Test
    fun `saveCurrent adds finish card once an image path exists`() = runTest(dispatcher) {
        val repo = repoMock()
        coEvery { FinishCardRenderer.render(any(), any()) } returns "/path/card.png"
        val context = mockk<Context>(relaxed = true)
        val vm = FinishCardViewModel(repo)
        vm.prepare(context, completionId = 1L)
        dispatcher.scheduler.advanceUntilIdle()

        vm.saveCurrent(completionId = 1L)
        dispatcher.scheduler.advanceUntilIdle()

        assertThat(vm.preview.value.saved).isTrue()
        coVerify {
            repo.addFinishCard(
                match<FinishCard> { it.completionId == 1L && it.imagePath == "/path/card.png" },
            )
        }
    }

    @Test
    fun `deleteCard delegates to repo`() = runTest(dispatcher) {
        val repo = repoMock()
        val vm = FinishCardViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()

        vm.deleteCard(42L)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { repo.deleteFinishCard(42L) }
    }
}
