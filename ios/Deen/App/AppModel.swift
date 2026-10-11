import DeenKit
import Foundation
import Observation
import SwiftUI

enum AppearanceMode: String, Codable, CaseIterable {
    case system, light, dark

    var colorScheme: ColorScheme? {
        switch self {
        case .system: return nil
        case .light: return .light
        case .dark: return .dark
        }
    }

    var title: String {
        switch self {
        case .system: return L("feature_settings_mode_system")
        case .light: return L("feature_settings_mode_light")
        case .dark: return L("feature_settings_mode_dark")
        }
    }
}

/// Everything the user chose. Stored as JSON on the phone only.
struct AppSettings: Codable, Equatable {
    var onboardingComplete = false
    var location: SavedLocation?
    var prayer: PrayerSettings?
    var theme: ThemeStyle = .emerald
    var custom = CustomColors()
    var appearance: AppearanceMode = .system

    var isSetUp: Bool { onboardingComplete && location != nil && prayer != nil }
}

/// The app's state: settings (saved on every change) and the city list (loaded when first needed).
@MainActor
@Observable
final class AppModel {
    private(set) var settings: AppSettings
    private(set) var cities: CityIndex?
    /// Screenshot mode (`-DeenDemo today|more|settings|onboarding`): fixed sample data, nothing saved.
    let demo: String?

    private static let storageKey = "deen.settings"

    init() {
        let defaults = UserDefaults.standard
        demo = defaults.string(forKey: "DeenDemo")
        if let demo {
            settings = Self.demoSettings(screen: demo, theme: defaults.string(forKey: "DeenTheme"))
        } else if let data = defaults.data(forKey: Self.storageKey),
                  let saved = try? JSONDecoder().decode(AppSettings.self, from: data) {
            settings = saved
        } else {
            settings = AppSettings()
        }
    }

    func update(_ change: (inout AppSettings) -> Void) {
        var next = settings
        change(&next)
        guard next != settings else { return }
        settings = next
        if demo == nil, let data = try? JSONEncoder().encode(next) {
            UserDefaults.standard.set(data, forKey: Self.storageKey)
        }
    }

    func binding<Value>(_ keyPath: WritableKeyPath<AppSettings, Value>) -> Binding<Value> {
        Binding(get: { self.settings[keyPath: keyPath] }, set: { value in self.update { $0[keyPath: keyPath] = value } })
    }

    /// The bundled GeoNames list (64k places, ~5 MB): parsed once, off the main thread.
    func loadCities() async -> CityIndex? {
        if let cities { return cities }
        guard let url = Bundle.main.url(forResource: "cities", withExtension: "tsv") else { return nil }
        let loaded = await Task.detached(priority: .userInitiated) { try? CityIndex.load(from: url) }.value
        cities = loaded
        return loaded
    }

    private static func demoSettings(screen: String, theme: String?) -> AppSettings {
        var s = AppSettings()
        s.theme = theme.flatMap(ThemeStyle.init(rawValue:)) ?? .emerald
        guard screen != "onboarding" else { return s }
        s.onboardingComplete = true
        s.location = SavedLocation(
            name: "Makkah", arabicName: "مكة المكرمة", region: "Mecca Region", countryCode: "SA",
            coordinates: Coordinates(latitude: 21.4266, longitude: 39.8256), timeZoneID: "Asia/Riyadh"
        )
        s.prayer = PrayerSettings(method: .ummAlQura)
        return s
    }
}
