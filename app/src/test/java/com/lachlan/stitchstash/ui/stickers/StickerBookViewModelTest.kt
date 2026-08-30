package com.lachlan.stitchstash.ui.stickers

import com.google.common.truth.Truth.assertThat
import com.lachlan.stitchstash.data.db.entities.Sticker
import com.lachlan.stitchstash.data.repository.StitchRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StickerBookViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `stickers starts empty when repo has no stickers`() = runTest(dispatcher) {
        val repo = mockk<StitchRepository>(relaxed = true)
        every { repo.observeStickers() } returns flowOf(emptyList())

        val vm = StickerBookViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()

        assertThat(vm.stickers.value).isEmpty()
    }

    @Test
    fun `stickers surfaces the list from repo observeStickers`() = runTest(dispatcher) {
        val stickers = listOf(
            Sticker(id = 1L, type = "starter"),
            Sticker(id = 2L, type = "streak"),
        )
        val repo = mockk<StitchRepository>(relaxed = true)
        every { repo.observeStickers() } returns flowOf(stickers)

        val vm = StickerBookViewModel(repo)
        val job = launch { vm.stickers.collect {} }
        dispatcher.scheduler.advanceUntilIdle()

        assertThat(vm.stickers.value).isEqualTo(stickers)
        job.cancel()
    }
}
