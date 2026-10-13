package io.github.themeshmaker.dunya.core.designsystem.theme

import android.os.Build
import androidx.annotation.VisibleForTesting

/** What the phone maker's own interface is, for styles that match it (only ColorOS so far). */
object DeviceSkin {
    /** Tests can pretend to be a ColorOS phone. */
    @VisibleForTesting
    var colorOsOverride: Boolean? = null

    /**
     * ColorOS 17 or newer (OPPO; OnePlus's OxygenOS shares it): `ro.build.version.oplusrom` reads
     * "V17.0.0" there. Falls back to the build's display id ("PLP120_17.0.0.108(CN01)") on OPPO-made
     * phones if the property can't be read.
     */
    val isColorOs17: Boolean get() = colorOsOverride ?: detected

    private val detected: Boolean by lazy { detect() }

    private fun detect(): Boolean {
        val rom = runCatching {
            Class.forName(
                "android.os.SystemProperties"
            ).getMethod("get", String::class.java).invoke(null, "ro.build.version.oplusrom") as? String
        }.getOrNull().orEmpty()
        val major = Regex("""(\d+)""").find(rom)?.value?.toIntOrNull()
        if (major != null) return major >= COLOROS_17
        val oplus = Build.MANUFACTURER.lowercase() in setOf("oppo", "oneplus", "realme")
        val display = Regex("""_(\d+)\.\d+\.\d+""").find(Build.DISPLAY)?.groupValues?.get(1)?.toIntOrNull()
        return oplus && display != null && display >= COLOROS_17
    }

    private const val COLOROS_17 = 17
}
