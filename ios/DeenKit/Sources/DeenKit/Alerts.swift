import Foundation

/// What happens at a prayer's time. Sunrise can only notify (an adhan at sunrise would be wrong).
public enum AlertMode: String, Codable, CaseIterable, Sendable {
    case adhan, notification, silent
}

/// The adhan and reminder choices (the same as Android's AlertSettings).
public struct AlertSettings: Codable, Equatable, Sendable {
    public var modes: [Prayer: AlertMode]
    /// A quiet heads-up this many minutes before each prayer that has an alert; 0 = off.
    public var reminderMinutes: Int
    /// Optional "have you prayed?" this many minutes after each prayer's adhan; missing or 0 = off.
    public var afterAdhanMinutes: [Prayer: Int]
    /// Adhan recording: "default", "traditional" or "aaqib".
    public var voice: String
    /// A different recording for Fajr, or nil to use [voice].
    public var fajrVoice: String?

    public static let voices = ["default", "traditional", "aaqib"]
    public static let reminderChoices = [0, 5, 10, 15, 20, 30]
    public static let afterAdhanChoices = [0, 5, 10, 15, 20]
    public static let afterAdhanDefault = 10

    public init(modes: [Prayer: AlertMode] = [:], reminderMinutes: Int = 0, afterAdhanMinutes: [Prayer: Int] = [:],
                voice: String = "default", fajrVoice: String? = nil) {
        self.modes = modes
        self.reminderMinutes = reminderMinutes
        self.afterAdhanMinutes = afterAdhanMinutes
        self.voice = voice
        self.fajrVoice = fajrVoice
    }

    public func mode(for prayer: Prayer) -> AlertMode {
        let mode = modes[prayer] ?? (prayer == .sunrise ? .silent : .adhan)
        return prayer == .sunrise && mode == .adhan ? .notification : mode
    }

    public func voice(for prayer: Prayer) -> String { prayer == .fajr ? (fajrVoice ?? voice) : voice }

    public func afterAdhan(for prayer: Prayer) -> Int { prayer.isPrayer ? afterAdhanMinutes[prayer] ?? 0 : 0 }

    public var afterAdhanOn: Bool { Prayer.prayers.contains { afterAdhan(for: $0) > 0 } }

    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        modes = (try? c.decodeIfPresent([Prayer: AlertMode].self, forKey: .modes)) ?? [:]
        reminderMinutes = (try? c.decodeIfPresent(Int.self, forKey: .reminderMinutes)) ?? 0
        afterAdhanMinutes = (try? c.decodeIfPresent([Prayer: Int].self, forKey: .afterAdhanMinutes)) ?? [:]
        voice = (try? c.decodeIfPresent(String.self, forKey: .voice)) ?? "default"
        fajrVoice = try? c.decodeIfPresent(String.self, forKey: .fajrVoice)
    }
}

public enum AlertKind: String, Codable, Sendable {
    /// The adhan itself (an AlarmKit alarm on iOS).
    case adhan
    /// A plain notification at the prayer's time.
    case notification
    /// "Maghrib in 10 minutes".
    case reminder
    /// "Have you prayed Dhuhr?" some minutes after the adhan.
    case afterAdhan
}

/// One alert the system should deliver. [prayerAt] differs from [at] for reminders.
public struct PlannedAlert: Hashable, Sendable {
    public let prayer: Prayer
    public let kind: AlertKind
    public let at: Date
    public let prayerAt: Date
}

/// Every alert for the next [days] days, soonest first. iOS can't re-plan after each alert like
/// Android, so the app schedules several days ahead and refreshes whenever it runs.
public enum AlertPlanner {
    public static func plan(now: Date, location: SavedLocation, prayer settings: PrayerSettings, alerts: AlertSettings,
                            days: Int = 7) -> [PlannedAlert] {
        var result: [PlannedAlert] = []
        for offset in 0..<days {
            let d = PrayerEngine.localDate(of: now.addingTimeInterval(Double(offset) * 86_400), in: location.timeZone)
            guard let day = PrayerEngine.day(year: d.year, month: d.month, day: d.day, at: location.coordinates, settings: settings) else { continue }
            for prayer in Prayer.allCases {
                let at = day[prayer]
                let mode = alerts.mode(for: prayer)
                if mode != .silent {
                    result.append(PlannedAlert(prayer: prayer, kind: mode == .adhan ? .adhan : .notification, at: at, prayerAt: at))
                    if alerts.reminderMinutes > 0, prayer.isPrayer {
                        result.append(PlannedAlert(prayer: prayer, kind: .reminder,
                                                   at: at.addingTimeInterval(-Double(alerts.reminderMinutes) * 60), prayerAt: at))
                    }
                }
                let after = alerts.afterAdhan(for: prayer)
                if after > 0 {
                    result.append(PlannedAlert(prayer: prayer, kind: .afterAdhan, at: at.addingTimeInterval(Double(after) * 60), prayerAt: at))
                }
            }
        }
        // Several days are computed from "now" in steps of 24 h; drop anything already past or doubled.
        return Array(Set(result.filter { $0.at > now })).sorted { $0.at < $1.at }
    }
}
