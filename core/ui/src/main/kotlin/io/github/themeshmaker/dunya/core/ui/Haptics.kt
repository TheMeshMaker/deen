package io.github.themeshmaker.dunya.core.ui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Counting feedback for the tasbih and adhkar: a light tick for each count and a firmer
 * double pulse when a target is reached. Uses the vibrator directly, because the system's
 * "touch feedback" switch is often off and the tasbih is meant to be used without looking.
 */
class Haptics internal constructor(context: Context, private val enabled: Boolean) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }

    fun tick() = vibrate(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
        } else {
            VibrationEffect.createOneShot(TICK_MS, VibrationEffect.DEFAULT_AMPLITUDE)
        }
    )

    fun done() = vibrate(VibrationEffect.createWaveform(longArrayOf(0, 40, 80, 60), -1))

    @SuppressLint("MissingPermission") // VIBRATE is declared by :core:alarm and merged into the app.
    private fun vibrate(effect: VibrationEffect) {
        val device = vibrator ?: return
        if (!enabled || !device.hasVibrator()) return
        runCatching { device.vibrate(effect) }
    }

    private companion object {
        const val TICK_MS = 12L
    }
}

@Composable
fun rememberHaptics(enabled: Boolean): Haptics {
    val context = LocalContext.current
    return remember(context, enabled) { Haptics(context.applicationContext, enabled) }
}
