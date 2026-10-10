package io.github.themeshmaker.dunya.core.alarm.reliability

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.themeshmaker.dunya.core.alarm.AlertScheduler
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Phone families whose battery managers kill background apps in their own ways (dontkillmyapp.com). */
internal enum class PhoneBrand {
    /** OPPO, OnePlus, realme: ColorOS / OxygenOS / realme UI. */
    OPPO_FAMILY,
    XIAOMI,
    VIVO,
    SAMSUNG,
    HUAWEI_HONOR,
    OTHER
    ;

    companion object {
        fun detect(manufacturer: String = Build.MANUFACTURER, brand: String = Build.BRAND): PhoneBrand {
            val names = "${manufacturer.lowercase(Locale.ROOT)} ${brand.lowercase(Locale.ROOT)}"
            return when {
                listOf("oppo", "oneplus", "realme").any { it in names } -> OPPO_FAMILY
                listOf("xiaomi", "redmi", "poco").any { it in names } -> XIAOMI
                listOf("vivo", "iqoo").any { it in names } -> VIVO
                "samsung" in names -> SAMSUNG
                listOf("huawei", "honor").any { it in names } -> HUAWEI_HONOR
                else -> OTHER
            }
        }
    }
}

data class ReliabilityStatus(val notificationsAllowed: Boolean, val exactAlarmsAllowed: Boolean, val batteryUnrestricted: Boolean)

/**
 * Live checks for the reliability checklist (docs/BLUEPRINT.md §10), plus the settings screens
 * to fix each one. OEM screens are tried in order and fall back to App info; launch with
 * try/catch because ROMs rename or lock these activities between versions.
 */
@Singleton
class ReliabilityChecker @Inject constructor(@ApplicationContext private val context: Context, private val scheduler: AlertScheduler) {
    /** Only picks which settings screen to open; the UI never names the phone maker. */
    private val brand: PhoneBrand = PhoneBrand.detect()

    fun status() = ReliabilityStatus(
        notificationsAllowed = NotificationManagerCompat.from(context).areNotificationsEnabled(),
        exactAlarmsAllowed = scheduler.canScheduleExact(),
        batteryUnrestricted = context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)
    )

    private val packageUri: Uri get() = Uri.parse("package:${context.packageName}")

    fun notificationSettingsIntents(): List<Intent> = listOf(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
        appDetails()
    )

    fun exactAlarmIntents(): List<Intent> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri))
        add(appDetails())
    }

    @android.annotation.SuppressLint("BatteryLife") // Prayer alarms are the app's core function (BLUEPRINT §10.2).
    fun batteryIntents(): List<Intent> = listOf(
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri),
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    )

    /** The phone maker's own background / auto-start screen. */
    fun backgroundIntents(): List<Intent> = when (brand) {
        // ColorOS 17 keeps "Allow background activity" under App info → Battery usage; its own
        // page (com.oplus.battery …PowerControlActivity) refuses outside callers.
        PhoneBrand.OPPO_FAMILY -> listOf(
            component("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
            appDetails()
        )

        PhoneBrand.XIAOMI -> listOf(
            component("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
            appDetails()
        )

        PhoneBrand.VIVO -> listOf(
            component("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
            component("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"),
            appDetails()
        )

        PhoneBrand.HUAWEI_HONOR -> listOf(
            component("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
            component("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"),
            appDetails()
        )

        PhoneBrand.SAMSUNG, PhoneBrand.OTHER -> listOf(
            Intent("android.settings.VIEW_ADVANCED_POWER_USAGE_DETAIL", packageUri),
            appDetails()
        )
    }

    private fun appDetails() = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)

    private fun component(pkg: String, cls: String) = Intent().setComponent(ComponentName(pkg, cls))
}
