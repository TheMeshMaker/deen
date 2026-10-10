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
    DESERT
}

/**
 * How the app is laid out, independent of its colours: the same screens in two looks.
 * Classic keeps compact cards and a standard bottom bar; Sleek uses an immersive header,
 * floating cards, roomier type and a floating navigation bar.
 */
enum class UiStyle {
    CLASSIC,
    SLEEK
}

/** The app's language. SYSTEM follows the phone. */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    ENGLISH("en"),
    ARABIC("ar")
}

/** Which calendar the monthly prayer timetable pages through. */
enum class CalendarType {
    GREGORIAN,
    HIJRI
}
