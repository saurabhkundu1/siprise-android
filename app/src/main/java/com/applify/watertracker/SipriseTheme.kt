package com.applify.watertracker

import androidx.compose.ui.graphics.Color
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

enum class ThemeMode(val id: Int, val label: String) {
    Light(0, "Light"),
    Dark(1, "Dark"),
    System(2, "System");

    companion object {
        fun fromId(id: Int): ThemeMode = entries.firstOrNull { it.id == id } ?: System
    }
}

enum class VolumeUnit(val key: String, val label: String) {
    ML("ml", "ml"),
    FL_OZ("fl_oz", "fl oz");

    companion object {
        fun fromKey(key: String?): VolumeUnit = entries.firstOrNull { it.key == key } ?: ML
    }
}

fun formatVolume(ml: Int, unit: VolumeUnit, numberFormat: NumberFormat = NumberFormat.getIntegerInstance()): String {
    return when (unit) {
        VolumeUnit.ML -> "${numberFormat.format(ml)} ml"
        VolumeUnit.FL_OZ -> {
            val oz = ml / 29.5735
            val rounded = (oz * 10.0).roundToInt() / 10.0
            val text = if (rounded % 1.0 == 0.0) {
                rounded.toInt().toString()
            } else {
                String.format(Locale.getDefault(), "%.1f", rounded)
            }
            "$text fl oz"
        }
    }
}

fun formatVolumeNumber(ml: Int, unit: VolumeUnit, numberFormat: NumberFormat = NumberFormat.getIntegerInstance()): String {
    return when (unit) {
        VolumeUnit.ML -> numberFormat.format(ml)
        VolumeUnit.FL_OZ -> {
            val oz = ml / 29.5735
            val rounded = (oz * 10.0).roundToInt() / 10.0
            if (rounded % 1.0 == 0.0) {
                rounded.toInt().toString()
            } else {
                String.format(Locale.getDefault(), "%.1f", rounded)
            }
        }
    }
}

data class SiprisePalette(
    val page: Color,
    val card: Color,
    val primary: Color,
    val onPrimary: Color,
    val softChip: Color,
    val text: Color,
    val muted: Color,
    val track: Color,
    val barMuted: Color,
    val divider: Color,
    val goalBannerBg: Color,
    val isDark: Boolean,
)

enum class SipriseColorScheme(
    val key: String,
    val label: String,
    val light: SiprisePalette,
    val dark: SiprisePalette,
) {
    SAGE_MINT(
        key = "sage_mint",
        label = "Sage Mint",
        light = SiprisePalette(
            page = Color(0xFFF4F9F7),
            card = Color(0xFFFFFFFF),
            primary = Color(0xFF0F766E),
            onPrimary = Color.White,
            softChip = Color(0xFFE4F2EE),
            text = Color(0xFF112A26),
            muted = Color(0xFF5C746F),
            track = Color(0xFFE4F0EC),
            barMuted = Color(0xFF74BEB0),
            divider = Color(0xFFEEF3F1),
            goalBannerBg = Color(0xFFD4EDE4),
            isDark = false,
        ),
        dark = SiprisePalette(
            page = Color(0xFF0E1715),
            card = Color(0xFF15211E),
            primary = Color(0xFF34B5A7),
            onPrimary = Color(0xFF072320),
            softChip = Color(0xFF172B27),
            text = Color(0xFFE2F0EC),
            muted = Color(0xFF95ACA7),
            track = Color(0xFF213430),
            barMuted = Color(0xFF296E65),
            divider = Color(0xFF21302C),
            goalBannerBg = Color(0xFF1A3832),
            isDark = true,
        ),
    ),
    OCEAN_MIST(
        key = "ocean_mist",
        label = "Ocean Mist",
        light = SiprisePalette(
            page = Color(0xFFF2F7FB),
            card = Color(0xFFFFFFFF),
            primary = Color(0xFF1D6FA5),
            onPrimary = Color.White,
            softChip = Color(0xFFE0EFFA),
            text = Color(0xFF0F2537),
            muted = Color(0xFF587084),
            track = Color(0xFFE1ECF4),
            barMuted = Color(0xFF7CB2D6),
            divider = Color(0xFFEDF2F7),
            goalBannerBg = Color(0xFFD5E9F8),
            isDark = false,
        ),
        dark = SiprisePalette(
            page = Color(0xFF0D151C),
            card = Color(0xFF14202B),
            primary = Color(0xFF5AA8DC),
            onPrimary = Color(0xFF072033),
            softChip = Color(0xFF172836),
            text = Color(0xFFE2ECF5),
            muted = Color(0xFF93A8B9),
            track = Color(0xFF1F3242),
            barMuted = Color(0xFF2D678F),
            divider = Color(0xFF1E2D3B),
            goalBannerBg = Color(0xFF193247),
            isDark = true,
        ),
    ),
    MIDNIGHT_INDIGO(
        key = "midnight_indigo",
        label = "Midnight Indigo",
        light = SiprisePalette(
            page = Color(0xFFF4F5FB),
            card = Color(0xFFFFFFFF),
            primary = Color(0xFF3949AB),
            onPrimary = Color.White,
            softChip = Color(0xFFE4E7F8),
            text = Color(0xFF161B33),
            muted = Color(0xFF5E6480),
            track = Color(0xFFE5E8F5),
            barMuted = Color(0xFF8592D6),
            divider = Color(0xFFEEF0F8),
            goalBannerBg = Color(0xFFDCE0F7),
            isDark = false,
        ),
        dark = SiprisePalette(
            page = Color(0xFF10121E),
            card = Color(0xFF171B2C),
            primary = Color(0xFF7986CB),
            onPrimary = Color(0xFF0E132E),
            softChip = Color(0xFF1D223A),
            text = Color(0xFFE5E7F5),
            muted = Color(0xFF989EB8),
            track = Color(0xFF242A42),
            barMuted = Color(0xFF424F96),
            divider = Color(0xFF22283E),
            goalBannerBg = Color(0xFF21284A),
            isDark = true,
        ),
    ),
    SUNSET_CORAL(
        key = "sunset_coral",
        label = "Sunset Coral",
        light = SiprisePalette(
            page = Color(0xFFFDF6F4),
            card = Color(0xFFFFFFFF),
            primary = Color(0xFFD95D39),
            onPrimary = Color.White,
            softChip = Color(0xFFFAE6E0),
            text = Color(0xFF331912),
            muted = Color(0xFF7D625A),
            track = Color(0xFFF5E6E1),
            barMuted = Color(0xFFE89D86),
            divider = Color(0xFFF7ECE8),
            goalBannerBg = Color(0xFFF7DDD5),
            isDark = false,
        ),
        dark = SiprisePalette(
            page = Color(0xFF1B1210),
            card = Color(0xFF251916),
            primary = Color(0xFFF08A6C),
            onPrimary = Color(0xFF331309),
            softChip = Color(0xFF331F19),
            text = Color(0xFFF6E8E4),
            muted = Color(0xFFB89D95),
            track = Color(0xFF382621),
            barMuted = Color(0xFF9E4930),
            divider = Color(0xFF33221D),
            goalBannerBg = Color(0xFF45271E),
            isDark = true,
        ),
    ),
    WARM_AMBER(
        key = "warm_amber",
        label = "Warm Amber",
        light = SiprisePalette(
            page = Color(0xFFFCF9F2),
            card = Color(0xFFFFFFFF),
            primary = Color(0xFFB45309),
            onPrimary = Color.White,
            softChip = Color(0xFFF8ECD6),
            text = Color(0xFF2E1E0F),
            muted = Color(0xFF7A6855),
            track = Color(0xFFF2EAD9),
            barMuted = Color(0xFFDDA35D),
            divider = Color(0xFFF5EFE2),
            goalBannerBg = Color(0xFFF5E4C6),
            isDark = false,
        ),
        dark = SiprisePalette(
            page = Color(0xFF1A150E),
            card = Color(0xFF241D14),
            primary = Color(0xFFF59E0B),
            onPrimary = Color(0xFF2E1A03),
            softChip = Color(0xFF312515),
            text = Color(0xFFF5EDE0),
            muted = Color(0xFFB5A38C),
            track = Color(0xFF362B1D),
            barMuted = Color(0xFF9C6214),
            divider = Color(0xFF33281A),
            goalBannerBg = Color(0xFF423015),
            isDark = true,
        ),
    ),
    LAVENDER_CALM(
        key = "lavender_calm",
        label = "Lavender Calm",
        light = SiprisePalette(
            page = Color(0xFFF7F5FB),
            card = Color(0xFFFFFFFF),
            primary = Color(0xFF6D4C9E),
            onPrimary = Color.White,
            softChip = Color(0xFFEAE3F6),
            text = Color(0xFF211730),
            muted = Color(0xFF6C617D),
            track = Color(0xFFECE6F5),
            barMuted = Color(0xFFA38BC9),
            divider = Color(0xFFF1ECF8),
            goalBannerBg = Color(0xFFE3D9F5),
            isDark = false,
        ),
        dark = SiprisePalette(
            page = Color(0xFF15111D),
            card = Color(0xFF1E1829),
            primary = Color(0xFFA78BFA),
            onPrimary = Color(0xFF1E1038),
            softChip = Color(0xFF271E38),
            text = Color(0xFFECE7F5),
            muted = Color(0xFFA69BB8),
            track = Color(0xFF2D253D),
            barMuted = Color(0xFF5C438A),
            divider = Color(0xFF2B223B),
            goalBannerBg = Color(0xFF34264F),
            isDark = true,
        ),
    );

    fun palette(isDark: Boolean): SiprisePalette = if (isDark) dark else light

    companion object {
        fun fromKey(key: String?): SipriseColorScheme =
            entries.firstOrNull { it.key == key } ?: SAGE_MINT
    }
}
