/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.core.model

/**
 * Days of the Islamic year that most Muslims mark, on the Umm al-Qura calendar: only dates that
 * are widely agreed on. [days] is how long each lasts (0 = the whole month). [WHITE_DAYS], the
 * 13th to 15th (recommended fasting), recurs every month.
 */
enum class IslamicEvent(val month: Int, val day: Int, val days: Int = 1) {
    NEW_YEAR(1, 1),
    ASHURA(1, 10),
    RAMADAN(9, 1, days = 0),
    LAST_TEN_NIGHTS(9, 21, days = 10),
    EID_AL_FITR(10, 1),
    ARAFAH(12, 9),
    EID_AL_ADHA(12, 10),
    WHITE_DAYS(0, 13, days = 3)
}
