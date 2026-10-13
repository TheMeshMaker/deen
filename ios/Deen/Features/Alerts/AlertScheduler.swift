import ActivityKit
import AlarmKit
import AppIntents
import AVFoundation
import BackgroundTasks
import DeenKit
import SwiftUI
import UserNotifications

/// What an adhan alarm carries (shown by the system's alarm UI).
struct PrayerAlarm: AlarmMetadata {
    var prayer: String
}

/// Puts the plan from [AlertPlanner] into iOS: the adhan as AlarmKit alarms (they ring through
/// silent mode and Focus, with a full-screen Stop), everything else as local notifications. iOS
/// can't re-plan after each alert as Android does, so six days are scheduled at a time and the plan
/// is refreshed whenever the app runs (and once a day in the background).
enum AlertScheduler {
    static let refreshTaskID = "io.github.themeshmaker.dunya.refresh"
    /// iOS keeps at most 64 pending notifications per app.
    private static let maxNotifications = 60
    private static let days = 6

    static var alarmsAllowed: Bool { AlarmManager.shared.authorizationState == .authorized }
    static var alarmsDenied: Bool { AlarmManager.shared.authorizationState == .denied }

    /// Asked once, when setup finishes (and again from Settings if needed).
    static func requestPermissions() async {
        _ = try? await UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge])
        if AlarmManager.shared.authorizationState == .notDetermined {
            _ = try? await AlarmManager.shared.requestAuthorization()
        }
    }

    static func reschedule(_ settings: AppSettings) async {
        guard settings.isSetUp, let location = settings.location, let prayer = settings.prayer else { return }
        let now = Date()
        let plan = AlertPlanner.plan(now: now, location: location, prayer: prayer, alerts: settings.alerts, days: days)
        let manager = AlarmManager.shared
        for alarm in (try? manager.alarms) ?? [] {
            try? manager.cancel(id: alarm.id)
        }
        var notifications: [PlannedAlert] = []
        let useAlarms = alarmsAllowed
        for alert in plan {
            if alert.kind == .adhan, useAlarms {
                await scheduleAlarm(at: alert.at, prayer: alert.prayer, voice: settings.alerts.voice(for: alert.prayer),
                                    title: L("core_alarm_time_for", name(of: alert.prayer, at: alert.prayerAt, in: location.timeZone)))
            } else {
                notifications.append(alert)
            }
        }
        let center = UNUserNotificationCenter.current()
        center.removeAllPendingNotificationRequests()
        for alert in notifications.prefix(maxNotifications) {
            try? await center.add(request(for: alert, settings: settings, zone: location.timeZone, now: now))
        }
        scheduleBackgroundRefresh()
    }

    /// "Test adhan in 1 minute" (Settings): one alarm, so the user can lock the phone and listen.
    static func scheduleTest(voice: String) async {
        await requestPermissions()
        if alarmsAllowed {
            await scheduleAlarm(at: Date().addingTimeInterval(60), prayer: .fajr, voice: voice, title: L("core_alarm_test_title"))
        } else {
            let content = UNMutableNotificationContent()
            content.title = L("core_alarm_test_title")
            content.body = L("core_alarm_test_body")
            content.sound = UNNotificationSound(named: UNNotificationSoundName("adhan_\(voice)_alarm.caf"))
            try? await UNUserNotificationCenter.current().add(
                UNNotificationRequest(identifier: "test", content: content, trigger: UNTimeIntervalNotificationTrigger(timeInterval: 60, repeats: false)))
        }
    }

    private static func scheduleAlarm(at date: Date, prayer: Prayer, voice: String, title: String) async {
        let titleResource = LocalizedStringResource(stringLiteral: title)
        let fullAdhan = AlarmButton(text: LocalizedStringResource(stringLiteral: L("ios_full_adhan")), textColor: .white, systemImageName: "speaker.wave.2.fill")
        let alert: AlarmPresentation.Alert
        if #available(iOS 26.1, *) {
            alert = AlarmPresentation.Alert(title: titleResource, secondaryButton: fullAdhan, secondaryButtonBehavior: .custom)
        } else {
            let stop = AlarmButton(text: LocalizedStringResource(stringLiteral: L("core_alarm_stop")), textColor: .white, systemImageName: "stop.fill")
            alert = AlarmPresentation.Alert(title: titleResource, stopButton: stop, secondaryButton: fullAdhan, secondaryButtonBehavior: .custom)
        }
        let attributes = AlarmAttributes<PrayerAlarm>(
            presentation: AlarmPresentation(alert: alert),
            metadata: PrayerAlarm(prayer: prayer.rawValue),
            tintColor: Color(argb: 0xFF0E8A62)
        )
        let configuration = AlarmManager.AlarmConfiguration<PrayerAlarm>.alarm(
            schedule: .fixed(date),
            attributes: attributes,
            stopIntent: nil,
            secondaryIntent: PlayFullAdhanIntent(voice: voice),
            sound: .named("adhan_\(voice)_alarm.caf")
        )
        _ = try? await AlarmManager.shared.schedule(id: UUID(), configuration: configuration)
    }

    private static func request(for alert: PlannedAlert, settings: AppSettings, zone: TimeZone, now: Date) -> UNNotificationRequest {
        let content = UNMutableNotificationContent()
        let prayerName = name(of: alert.prayer, at: alert.prayerAt, in: zone)
        let time = Formats.time(alert.prayerAt, in: zone)
        switch alert.kind {
        case .adhan:
            // Alarms not allowed: the adhan's first 30 seconds as the notification sound.
            content.title = L("core_alarm_title", prayerName, time)
            content.body = L("core_alarm_time_for", prayerName)
            content.sound = UNNotificationSound(named: UNNotificationSoundName("adhan_\(settings.alerts.voice(for: alert.prayer))_alarm.caf"))
        case .notification:
            content.title = L("core_alarm_title", prayerName, time)
            content.body = L("core_alarm_time_for", prayerName)
            content.sound = .default
        case .reminder:
            content.title = L("core_alarm_reminder_title.other", prayerName, settings.alerts.reminderMinutes)
            content.body = L("core_alarm_reminder_body", time)
            content.sound = .default
        case .afterAdhan:
            content.title = L("core_alarm_after_title", prayerName)
            content.body = LP("core_alarm_after_body", Int(alert.at.timeIntervalSince(alert.prayerAt) / 60))
            content.sound = .default
        case .kahf:
            content.title = L("core_alarm_kahf_title")
            content.body = L("core_alarm_kahf_body")
            content.sound = .default
            content.userInfo = [NotificationRouter.openSurahKey: 18]
        }
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: max(1, alert.at.timeIntervalSince(now)), repeats: false)
        return UNNotificationRequest(identifier: "\(alert.kind.rawValue)-\(alert.prayer.rawValue)-\(Int(alert.at.timeIntervalSince1970))",
                                     content: content, trigger: trigger)
    }

    /// Dhuhr is Jumuʿah on Fridays (where the user is).
    static func name(of prayer: Prayer, at date: Date, in zone: TimeZone) -> String {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = zone
        return prayer.name(friday: calendar.component(.weekday, from: date) == 6)
    }

    private static func scheduleBackgroundRefresh() {
        let request = BGAppRefreshTaskRequest(identifier: refreshTaskID)
        request.earliestBeginDate = Date().addingTimeInterval(12 * 3600)
        try? BGTaskScheduler.shared.submit(request)
    }
}

/// Taps on Deen's notifications: the Friday reminder opens Surah Al-Kahf (through the Quran tab).
final class NotificationRouter: NSObject, UNUserNotificationCenterDelegate, @unchecked Sendable {
    static let shared = NotificationRouter()
    static let openSurahKey = "openSurah"

    @MainActor weak var model: AppModel?

    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse) async {
        guard let surah = response.notification.request.content.userInfo[Self.openSurahKey] as? Int else { return }
        await MainActor.run { model?.pendingReader = QuranPosition(surah: surah, ayah: 1) }
    }

    /// Shown as a banner even while Deen is open.
    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification) async -> UNNotificationPresentationOptions {
        [.banner, .sound, .list]
    }
}

/// The alarm's "Full adhan" button: opens Deen and plays the whole recording.
struct PlayFullAdhanIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "Full adhan"
    static var openAppWhenRun = true

    @Parameter(title: "Voice")
    var voice: String

    init() {}

    init(voice: String) {
        self.voice = voice
    }

    func perform() async throws -> some IntentResult {
        await AdhanPlayer.shared.play(voice: voice)
        return .result()
    }
}

/// Plays a whole adhan recording inside the app (also used to preview voices in Settings).
@MainActor
@Observable
final class AdhanPlayer {
    static let shared = AdhanPlayer()

    private(set) var playing: String?
    private var player: AVAudioPlayer?

    func play(voice: String) {
        stop()
        guard let url = Bundle.main.url(forResource: "adhan_\(voice)", withExtension: "m4a") else { return }
        try? AVAudioSession.sharedInstance().setCategory(.playback, mode: .default)
        try? AVAudioSession.sharedInstance().setActive(true)
        player = try? AVAudioPlayer(contentsOf: url)
        player?.play()
        playing = voice
    }

    func stop() {
        player?.stop()
        player = nil
        playing = nil
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
    }
}
