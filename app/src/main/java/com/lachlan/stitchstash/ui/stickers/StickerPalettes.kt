package com.lachlan.stitchstash.ui.stickers

import androidx.compose.ui.graphics.Color
import com.lachlan.stitchstash.domain.stickers.StickerCatalog

data class StickerPalette(
    val center: Color,
    val edge: Color,
    val trim: Color,
    val highlight: Color,
    val accent: Color,
)

/**
 * Per-sticker colour palette. Each is a 3-colour radial gradient + trim accent.
 * Picked from the app's themed colour family so they all feel related but distinct.
 */
object StickerPalettes {

    private val ROSE = StickerPalette(
        center = Color(0xFFFFC9D9),
        edge = Color(0xFFE84A8C),
        trim = Color(0xFFFFD9E5),
        highlight = Color(0xFFFFFFFF).copy(alpha = 0.5f),
        accent = Color(0xFFFF8AB0),
    )
    private val LAVENDER = StickerPalette(
        center = Color(0xFFE6D6FF),
        edge = Color(0xFFA675E0),
        trim = Color(0xFFEDE0FF),
        highlight = Color(0xFFFFFFFF).copy(alpha = 0.5f),
        accent = Color(0xFFC4A0FF),
    )
    private val SUNSHINE = StickerPalette(
        center = Color(0xFFFFE9B8),
        edge = Color(0xFFFFB627),
        trim = Color(0xFFFFEFCC),
        highlight = Color(0xFFFFFFFF).copy(alpha = 0.5f),
        accent = Color(0xFFFFD56B),
    )
    private val CORAL = StickerPalette(
        center = Color(0xFFFFD3C4),
        edge = Color(0xFFFF8A65),
        trim = Color(0xFFFFE0D2),
        highlight = Color(0xFFFFFFFF).copy(alpha = 0.5f),
        accent = Color(0xFFFFA88A),
    )
    private val MINT = StickerPalette(
        center = Color(0xFFC4E8DB),
        edge = Color(0xFF7BD3B5),
        trim = Color(0xFFD8F1E5),
        highlight = Color(0xFFFFFFFF).copy(alpha = 0.5f),
        accent = Color(0xFFA0DEC4),
    )
    private val SKY = StickerPalette(
        center = Color(0xFFCDE7F7),
        edge = Color(0xFF7CC4F0),
        trim = Color(0xFFDEF0FA),
        highlight = Color(0xFFFFFFFF).copy(alpha = 0.5f),
        accent = Color(0xFFA8D6F3),
    )
    private val GOLD = StickerPalette(
        center = Color(0xFFFFE89B),
        edge = Color(0xFFE5B873),
        trim = Color(0xFFFFEFB5),
        highlight = Color(0xFFFFFFFF).copy(alpha = 0.6f),
        accent = Color(0xFFFFD06B),
    )

    fun forType(type: String): StickerPalette = when (type) {
        StickerCatalog.FIRST_EVER -> ROSE
        StickerCatalog.FIRST_OF_PATTERN -> LAVENDER
        StickerCatalog.PATTERN_COMPLETE -> SUNSHINE
        StickerCatalog.MILESTONE_FIFTH -> CORAL
        StickerCatalog.MILESTONE_TENTH -> MINT
        StickerCatalog.MILESTONE_TWENTY_FIFTH -> GOLD
        StickerCatalog.PERSONAL_WEEKLY_BEST -> SKY
        StickerCatalog.WELCOME_BACK -> ROSE
        StickerCatalog.FIRST_RIBBLR -> LAVENDER
        else -> ROSE
    }
}
