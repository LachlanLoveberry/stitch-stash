package com.lachlan.stitchstash.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.lachlan.stitchstash.data.db.StitchStashDatabase
import com.lachlan.stitchstash.data.db.entities.AppSettings
import com.lachlan.stitchstash.data.db.entities.Colourway
import com.lachlan.stitchstash.data.db.entities.Completion
import com.lachlan.stitchstash.data.db.entities.Pattern
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StitchRepositoryTest {

    private lateinit var db: StitchStashDatabase
    private lateinit var repo: StitchRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, StitchStashDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = StitchRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun insertPatternAndColourway(targetCount: Int = 5): Pair<Long, Long> {
        val patternId = repo.addPattern(Pattern(name = "Amigurumi Fox"))
        val colourwayId = repo.addColourway(
            Colourway(patternId = patternId, name = "Classic", targetCount = targetCount),
        )
        return patternId to colourwayId
    }

    // ---- addPattern ----------------------------------------------------

    @Test(expected = IllegalArgumentException::class)
    fun `addPattern with blank name throws`() = runTest {
        repo.addPattern(Pattern(name = "   "))
    }

    // ---- addColourway ----------------------------------------------------

    @Test(expected = IllegalArgumentException::class)
    fun `addColourway with blank name throws`() = runTest {
        val patternId = repo.addPattern(Pattern(name = "Fox"))
        repo.addColourway(Colourway(patternId = patternId, name = " "))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `addColourway with negative targetCount throws`() = runTest {
        val patternId = repo.addPattern(Pattern(name = "Fox"))
        repo.addColourway(Colourway(patternId = patternId, name = "Classic", targetCount = -1))
    }

    // ---- updateSettings ----------------------------------------------------

    @Test(expected = IllegalArgumentException::class)
    fun `updateSettings with NaN weeklyHours throws`() = runTest {
        repo.updateSettings(AppSettings(weeklyHours = Float.NaN))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `updateSettings with infinite weeklyHours throws`() = runTest {
        repo.updateSettings(AppSettings(weeklyHours = Float.POSITIVE_INFINITY))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `updateSettings with negative targetPieces throws`() = runTest {
        repo.updateSettings(AppSettings(targetPieces = -1))
    }

    // ---- addCompletionAndEarnStickers ----------------------------------------------------

    @Test
    fun `addCompletionAndEarnStickers happy path earns first-ever sticker and persists`() = runTest {
        val (_, colourwayId) = insertPatternAndColourway()

        val result = repo.addCompletionAndEarnStickers(
            Completion(colourwayId = colourwayId, completedAtEpochDay = 19000L),
        )

        assertThat(result.completion.id).isGreaterThan(0L)
        assertThat(result.earned.map { it.type }).containsAtLeast(
            "first_ever",
            "first_of_pattern",
        )

        val persistedCompletion = db.completionDao().getById(result.completion.id)
        assertThat(persistedCompletion).isNotNull()

        val persistedStickers = db.stickerDao().observeAll()
        // Confirm the stickers were actually inserted via a direct count query.
        assertThat(db.stickerDao().countOfType("first_ever")).isEqualTo(1)
        assertThat(db.stickerDao().countOfType("first_of_pattern")).isEqualTo(1)
    }

    @Test(expected = android.database.sqlite.SQLiteConstraintException::class)
    fun `addCompletionAndEarnStickers with nonexistent colourwayId is rejected by the FK constraint`() = runTest {
        // Completion.colourwayId has a Room ForeignKey to Colourway, so an orphaned
        // reference is rejected at the DB layer before the repository's sticker logic runs.
        repo.addCompletionAndEarnStickers(
            Completion(colourwayId = 999_999L, completedAtEpochDay = 19000L),
        )
    }

    @Test
    fun `addCompletionAndEarnStickers failure leaves no orphan sticker rows (transactional)`() = runTest {
        runCatching {
            repo.addCompletionAndEarnStickers(
                Completion(colourwayId = 999_999L, completedAtEpochDay = 19000L),
            )
        }

        assertThat(db.completionDao().observeAll().first()).isEmpty()
        assertThat(db.stickerDao().countOfType("first_ever")).isEqualTo(0)
    }
}
