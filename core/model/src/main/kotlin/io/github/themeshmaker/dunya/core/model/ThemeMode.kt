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
