package io.github.themeshmaker.dunya.core.ui

import androidx.annotation.StringRes
import io.github.themeshmaker.dunya.core.model.AsrMethod
import io.github.themeshmaker.dunya.core.model.CalculationMethod
import io.github.themeshmaker.dunya.core.model.Prayer

/** On Fridays Dhuhr is shown as Jumu'ah. */
@StringRes
fun Prayer.labelRes(isFriday: Boolean = false): Int = when (this) {
    Prayer.FAJR -> R.string.core_ui_prayer_fajr
    Prayer.SUNRISE -> R.string.core_ui_prayer_sunrise
    Prayer.DHUHR -> if (isFriday) R.string.core_ui_prayer_jumuah else R.string.core_ui_prayer_dhuhr
    Prayer.ASR -> R.string.core_ui_prayer_asr
    Prayer.MAGHRIB -> R.string.core_ui_prayer_maghrib
    Prayer.ISHA -> R.string.core_ui_prayer_isha
}

@StringRes
fun CalculationMethod.labelRes(): Int = when (this) {
    CalculationMethod.UMM_AL_QURA -> R.string.core_ui_method_umm_al_qura
    CalculationMethod.MUSLIM_WORLD_LEAGUE -> R.string.core_ui_method_mwl
    CalculationMethod.NORTH_AMERICA -> R.string.core_ui_method_isna
    CalculationMethod.EGYPTIAN -> R.string.core_ui_method_egyptian
    CalculationMethod.KARACHI -> R.string.core_ui_method_karachi
    CalculationMethod.DUBAI -> R.string.core_ui_method_dubai
    CalculationMethod.KUWAIT -> R.string.core_ui_method_kuwait
    CalculationMethod.QATAR -> R.string.core_ui_method_qatar
    CalculationMethod.SINGAPORE -> R.string.core_ui_method_singapore
    CalculationMethod.MOONSIGHTING_COMMITTEE -> R.string.core_ui_method_moonsighting
    CalculationMethod.TURKEY -> R.string.core_ui_method_turkey
}

/** Where a method is typically used, shown under its name. */
@StringRes
fun CalculationMethod.regionRes(): Int = when (this) {
    CalculationMethod.UMM_AL_QURA -> R.string.core_ui_method_umm_al_qura_region
    CalculationMethod.MUSLIM_WORLD_LEAGUE -> R.string.core_ui_method_mwl_region
    CalculationMethod.NORTH_AMERICA -> R.string.core_ui_method_isna_region
    CalculationMethod.EGYPTIAN -> R.string.core_ui_method_egyptian_region
    CalculationMethod.KARACHI -> R.string.core_ui_method_karachi_region
    CalculationMethod.DUBAI -> R.string.core_ui_method_dubai_region
    CalculationMethod.KUWAIT -> R.string.core_ui_method_kuwait_region
    CalculationMethod.QATAR -> R.string.core_ui_method_qatar_region
    CalculationMethod.SINGAPORE -> R.string.core_ui_method_singapore_region
    CalculationMethod.MOONSIGHTING_COMMITTEE -> R.string.core_ui_method_moonsighting_region
    CalculationMethod.TURKEY -> R.string.core_ui_method_turkey_region
}

@StringRes
fun AsrMethod.labelRes(): Int = when (this) {
    AsrMethod.STANDARD -> R.string.core_ui_asr_standard
    AsrMethod.HANAFI -> R.string.core_ui_asr_hanafi
}

@StringRes
fun AsrMethod.shortLabelRes(): Int = when (this) {
    AsrMethod.STANDARD -> R.string.core_ui_asr_standard_short
    AsrMethod.HANAFI -> R.string.core_ui_asr_hanafi_short
}
