import DeenKit
import Foundation
import WidgetKit

/// What the home-screen widget needs from the app, shared through the app group.
struct WidgetSnapshot: Codable, Equatable {
    var location: SavedLocation
    var prayer: PrayerSettings
    var theme: String
    var customStart: UInt32
    var customEnd: UInt32

    static let appGroup = "group.io.github.themeshmaker.dunya"
    private static let key = "deen.widget"

    static func load() -> WidgetSnapshot? {
        guard let data = UserDefaults(suiteName: appGroup)?.data(forKey: key) else { return nil }
        return try? JSONDecoder().decode(WidgetSnapshot.self, from: data)
    }

    func save() {
        guard let data = try? JSONEncoder().encode(self) else { return }
        UserDefaults(suiteName: Self.appGroup)?.set(data, forKey: Self.key)
        WidgetCenter.shared.reloadAllTimelines()
    }

    /// Shown in the widget gallery before the app is set up.
    static let sample = WidgetSnapshot(
        location: SavedLocation(name: "Makkah", arabicName: "مكة المكرمة", countryCode: "SA",
                                coordinates: Coordinates(latitude: 21.4266, longitude: 39.8256), timeZoneID: "Asia/Riyadh"),
        prayer: PrayerSettings(method: .ummAlQura),
        theme: "emerald",
        customStart: 0xFF1D2671,
        customEnd: 0xFF2BB3A7
    )
}
