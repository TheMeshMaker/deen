package io.github.themeshmaker.dunya.core.model

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,

    /** Pure black surfaces for OLED screens. */
    OLED_BLACK
}

/** Colour themes. All of them are free. */
enum class ThemeStyle {
    /** Deep green and white: the default. */
    EMERALD,

    /** Black and gold, after the Kiswah of the Kaaba. */
    KISWAH,

    /** Terracotta to warm sand. */
    DESERT,

    /** Cobalt and white, after the tiles of al-Andalus. */
    ANDALUS,

    /** Deep rose, after the roses of Taif. */
    ROSE,

    /** Night indigo with moonlight gold. */
    MIDNIGHT,

    /** The user's own two gradient colours ([CustomColors]). */
    CUSTOM
}

/** The custom theme's gradient, as ARGB colours; the more colourful one also seeds the colour scheme. */
data class CustomColors(val start: Int = DEFAULT_START, val end: Int = DEFAULT_END) {
    companion object {
        val DEFAULT_START: Int = 0xFF1D2671.toInt()
        val DEFAULT_END: Int = 0xFF2BB3A7.toInt()
    }
}

/**
 * How the app looks, independent of its colours: the same screens in four looks.
 * Classic keeps compact cards and a standard bottom bar; Sleek uses an immersive header,
 * floating cards, roomier type and a floating navigation bar; Glass is Sleek's layout with
 * see-through cards over a soft gradient; Heritage is Classic's layout dressed as a manuscript.
 */
enum class UiStyle {
    CLASSIC,
    SLEEK,
    GLASS,

    /** Like iOS 26: refracting, light-catching glass over a coloured backdrop. */
    LIQUID,
    HERITAGE,

    /**
     * Like the phone itself on ColorOS 17 (OPPO's "Fluid Fusion"): the system font, flat cards on a
     * light-grey page, a frosted floating tab bar. Offered only on ColorOS 17 and newer.
     */
    COLOROS;

    /** Full-bleed header, floating tab bar and large titles (Sleek, Glass, Liquid and ColorOS). */
    val immersive: Boolean get() = this == SLEEK || this == GLASS || this == LIQUID || this == COLOROS
}

/**
 * The app's language. SYSTEM follows the phone. [nativeName] is how the language names itself,
 * so people can find theirs whatever language the app is in. Arabic and English are written for
 * Deen; the others are translations awaiting review by native speakers.
 */
enum class AppLanguage(val tag: String?, val nativeName: String?) {
    SYSTEM(null, null),
    ARABIC("ar", "العربية"),
    ENGLISH("en", "English"),
    URDU("ur", "اردو"),
    INDONESIAN("id", "Bahasa Indonesia"),
    TURKISH("tr", "Türkçe"),
    BENGALI("bn", "বাংলা"),
    FRENCH("fr", "Français")
}

/** Which calendar the monthly prayer timetable pages through. */
enum class CalendarType {
    GREGORIAN,
    HIJRI
}
