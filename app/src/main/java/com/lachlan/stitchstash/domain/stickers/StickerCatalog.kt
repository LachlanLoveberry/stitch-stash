package com.lachlan.stitchstash.domain.stickers

/**
 * Single source of truth for sticker types. Swap the emoji values for custom
 * illustrations later without touching the earning rules.
 */
data class StickerDef(
    val type: String,
    val emoji: String,
    val title: String,
    val description: String,
)

object StickerCatalog {

    const val FIRST_EVER = "first_ever"
    const val FIRST_OF_PATTERN = "first_of_pattern"
    const val PATTERN_COMPLETE = "pattern_complete"
    const val MILESTONE_FIFTH = "milestone_fifth"
    const val MILESTONE_TENTH = "milestone_tenth"
    const val MILESTONE_TWENTY_FIFTH = "milestone_25"
    const val PERSONAL_WEEKLY_BEST = "weekly_best"
    const val WELCOME_BACK = "welcome_back"
    const val FIRST_RIBBLR = "first_ribblr"

    private val byType: Map<String, StickerDef> = listOf(
        StickerDef(FIRST_EVER, "", "First stitch", "Your very first piece logged."),
        StickerDef(FIRST_OF_PATTERN, "", "New pattern", "First of a fresh pattern."),
        StickerDef(PATTERN_COMPLETE, "", "Set complete", "Every colourway of a pattern done."),
        StickerDef(MILESTONE_FIFTH, "", "Five down", "Five pieces finished."),
        StickerDef(MILESTONE_TENTH, "", "Ten done", "Ten pieces finished."),
        StickerDef(MILESTONE_TWENTY_FIFTH, "", "Twenty-five", "Quarter of a hundred. "),
        StickerDef(PERSONAL_WEEKLY_BEST, "", "Personal best", "Most pieces in a week so far."),
        StickerDef(WELCOME_BACK, "", "Welcome back", "Picking it up again, no pressure."),
        StickerDef(FIRST_RIBBLR, "", "First Ribblr import", "Pattern brought in from Ribblr."),
    ).associateBy { it.type }

    fun get(type: String): StickerDef = byType[type] ?: StickerDef(type, "", type, "")

    fun all(): List<StickerDef> = byType.values.toList()
}
