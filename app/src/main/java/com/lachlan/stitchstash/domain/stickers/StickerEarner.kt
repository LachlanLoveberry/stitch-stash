package com.lachlan.stitchstash.domain.stickers

import com.lachlan.stitchstash.data.db.entities.Completion
import com.lachlan.stitchstash.data.db.entities.Sticker
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Snapshot of everything the earning rules need, computed once by the caller. */
data class StickerContext(
    val newCompletion: Completion,
    val patternId: Long,
    val isFirstEver: Boolean,
    val isFirstOfThisPattern: Boolean,
    val patternFullyCompletedNow: Boolean,
    val totalCount: Int,
    val previousTotalCount: Int,
    val daysSincePreviousCompletion: Long?,
    val cameFromRibblr: Boolean,
    val firstRibblrAlreadyEarned: Boolean,
    val welcomeBackAlreadyEarnedForGap: Boolean,
)

/** Pure rules — easy to unit test. */
object StickerEarner {

    private const val WELCOME_BACK_THRESHOLD_DAYS = 7L

    fun evaluate(ctx: StickerContext): List<Sticker> {
        val earned = mutableListOf<Sticker>()

        if (ctx.isFirstEver) {
            earned += Sticker(type = StickerCatalog.FIRST_EVER, relatedCompletionId = ctx.newCompletion.id)
        }

        if (ctx.isFirstOfThisPattern) {
            earned += Sticker(
                type = StickerCatalog.FIRST_OF_PATTERN,
                relatedCompletionId = ctx.newCompletion.id,
                relatedPatternId = ctx.patternId,
            )
        }

        if (ctx.patternFullyCompletedNow) {
            earned += Sticker(
                type = StickerCatalog.PATTERN_COMPLETE,
                relatedCompletionId = ctx.newCompletion.id,
                relatedPatternId = ctx.patternId,
            )
        }

        // Milestone — award when crossing each threshold from below
        listOf(
            5 to StickerCatalog.MILESTONE_FIFTH,
            10 to StickerCatalog.MILESTONE_TENTH,
            25 to StickerCatalog.MILESTONE_TWENTY_FIFTH,
        ).forEach { (threshold, type) ->
            if (ctx.previousTotalCount < threshold && ctx.totalCount >= threshold) {
                earned += Sticker(type = type, relatedCompletionId = ctx.newCompletion.id)
            }
        }

        if (ctx.cameFromRibblr && !ctx.firstRibblrAlreadyEarned) {
            earned += Sticker(
                type = StickerCatalog.FIRST_RIBBLR,
                relatedCompletionId = ctx.newCompletion.id,
                relatedPatternId = ctx.patternId,
            )
        }

        val gapDays = ctx.daysSincePreviousCompletion
        if (gapDays != null && gapDays >= WELCOME_BACK_THRESHOLD_DAYS && !ctx.welcomeBackAlreadyEarnedForGap) {
            earned += Sticker(
                type = StickerCatalog.WELCOME_BACK,
                relatedCompletionId = ctx.newCompletion.id,
            )
        }

        return earned
    }

    fun daysBetween(earlier: LocalDate, later: LocalDate): Long =
        ChronoUnit.DAYS.between(earlier, later)
}
