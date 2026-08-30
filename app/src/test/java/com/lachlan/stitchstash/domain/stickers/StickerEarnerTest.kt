package com.lachlan.stitchstash.domain.stickers

import com.google.common.truth.Truth.assertThat
import com.lachlan.stitchstash.data.db.entities.Completion
import org.junit.Test

class StickerEarnerTest {

    private val completion = Completion(id = 1L, colourwayId = 1L, completedAtEpochDay = 100L)

    private fun ctx(
        isFirstEver: Boolean = false,
        isFirstOfThisPattern: Boolean = false,
        patternFullyCompletedNow: Boolean = false,
        totalCount: Int = 1,
        previousTotalCount: Int = 0,
        daysSincePreviousCompletion: Long? = null,
        cameFromRibblr: Boolean = false,
        firstRibblrAlreadyEarned: Boolean = false,
    ) = StickerContext(
        newCompletion = completion,
        patternId = 1L,
        isFirstEver = isFirstEver,
        isFirstOfThisPattern = isFirstOfThisPattern,
        patternFullyCompletedNow = patternFullyCompletedNow,
        totalCount = totalCount,
        previousTotalCount = previousTotalCount,
        daysSincePreviousCompletion = daysSincePreviousCompletion,
        cameFromRibblr = cameFromRibblr,
        firstRibblrAlreadyEarned = firstRibblrAlreadyEarned,
        welcomeBackAlreadyEarnedForGap = false,
    )

    @Test
    fun `first ever completion earns first-ever sticker`() {
        val earned = StickerEarner.evaluate(ctx(isFirstEver = true))
        assertThat(earned.map { it.type }).contains(StickerCatalog.FIRST_EVER)
    }

    @Test
    fun `first of pattern earns first-of-pattern sticker`() {
        val earned = StickerEarner.evaluate(ctx(isFirstOfThisPattern = true))
        assertThat(earned.map { it.type }).contains(StickerCatalog.FIRST_OF_PATTERN)
    }

    @Test
    fun `pattern fully completed earns pattern-complete sticker`() {
        val earned = StickerEarner.evaluate(ctx(patternFullyCompletedNow = true))
        assertThat(earned.map { it.type }).contains(StickerCatalog.PATTERN_COMPLETE)
    }

    @Test
    fun `milestone at exact threshold is earned`() {
        val earned = StickerEarner.evaluate(ctx(previousTotalCount = 4, totalCount = 5))
        assertThat(earned.map { it.type }).contains(StickerCatalog.MILESTONE_FIFTH)
    }

    @Test
    fun `milestone crossed by skipping over it is still earned (jump 4 to 10)`() {
        val earned = StickerEarner.evaluate(ctx(previousTotalCount = 4, totalCount = 10))
        assertThat(earned.map { it.type }).containsAtLeast(
            StickerCatalog.MILESTONE_FIFTH,
            StickerCatalog.MILESTONE_TENTH,
        )
    }

    @Test
    fun `milestone not re-earned when already past it`() {
        val earned = StickerEarner.evaluate(ctx(previousTotalCount = 6, totalCount = 7))
        assertThat(earned.map { it.type }).doesNotContain(StickerCatalog.MILESTONE_FIFTH)
    }

    @Test
    fun `ribblr sticker earned first time only`() {
        val earnedFirst = StickerEarner.evaluate(ctx(cameFromRibblr = true, firstRibblrAlreadyEarned = false))
        assertThat(earnedFirst.map { it.type }).contains(StickerCatalog.FIRST_RIBBLR)

        val earnedAgain = StickerEarner.evaluate(ctx(cameFromRibblr = true, firstRibblrAlreadyEarned = true))
        assertThat(earnedAgain.map { it.type }).doesNotContain(StickerCatalog.FIRST_RIBBLR)
    }

    @Test
    fun `welcome back earned at exactly the threshold gap`() {
        val earned = StickerEarner.evaluate(ctx(daysSincePreviousCompletion = 7L))
        assertThat(earned.map { it.type }).contains(StickerCatalog.WELCOME_BACK)
    }

    @Test
    fun `welcome back not earned just under the threshold gap`() {
        val earned = StickerEarner.evaluate(ctx(daysSincePreviousCompletion = 6L))
        assertThat(earned.map { it.type }).doesNotContain(StickerCatalog.WELCOME_BACK)
    }

    @Test
    fun `welcome back not earned on null gap (no prior completion)`() {
        val earned = StickerEarner.evaluate(ctx(daysSincePreviousCompletion = null))
        assertThat(earned.map { it.type }).doesNotContain(StickerCatalog.WELCOME_BACK)
    }

    @Test
    fun `no stickers earned for an unremarkable completion`() {
        val earned = StickerEarner.evaluate(ctx())
        assertThat(earned).isEmpty()
    }

    @Test
    fun `daysBetween with reversed dates returns a negative value rather than throwing`() {
        val gap = StickerEarner.daysBetween(
            java.time.LocalDate.of(2026, 1, 10),
            java.time.LocalDate.of(2026, 1, 1),
        )
        assertThat(gap).isEqualTo(-9L)
    }
}
