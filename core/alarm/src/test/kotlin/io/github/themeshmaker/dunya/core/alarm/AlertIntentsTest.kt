/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.core.alarm

import io.github.themeshmaker.dunya.core.model.AlertKind
import io.github.themeshmaker.dunya.core.model.Prayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertIntentsTest {

    @Test
    fun everySlotHasItsOwnRequestCode() {
        val codes = AlertIntents.allSlots.map { (prayer, kind) -> AlertIntents.requestCode(prayer, kind) }
        assertEquals("request codes collide: $codes", codes.size, codes.toSet().size)
    }

    @Test
    fun adhanAndNotificationShareASlotSoSwitchingModeReplacesTheAlarm() {
        Prayer.entries.forEach { prayer ->
            assertEquals(AlertIntents.requestCode(prayer, AlertKind.ADHAN), AlertIntents.requestCode(prayer, AlertKind.NOTIFICATION))
        }
    }

    @Test
    fun remindersNeverReplaceTheAdhan() {
        Prayer.entries.forEach { prayer ->
            assertTrue(AlertIntents.requestCode(prayer, AlertKind.ADHAN) != AlertIntents.requestCode(prayer, AlertKind.REMINDER))
        }
    }
}
