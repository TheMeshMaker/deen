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
    var lastRead: QuranPosition?
    var bookmarks: [QuranPosition] = []
    var alerts = AlertSettings()
    var tasbih = TasbihState()
    var reciterID = Reciter.alafasy.rawValue
    var tafsirID = TafsirSource.jalalayn.rawValue
    var timetableKind: MonthCalendar.Kind = .gregorian
    /// Today's adhkar counts ("category/id" → times said); cleared on a new day.
    var adhkarDay = ""
    var adhkarCounts: [String: Int] = [:]

    var isSetUp: Bool { onboardingComplete && location != nil && prayer != nil }

    init() {}

    /// Every field is optional on disk, so settings saved by an older version keep loading.
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        onboardingComplete = try c.decodeIfPresent(Bool.self, forKey: .onboardingComplete) ?? false
        location = try c.decodeIfPresent(SavedLocation.self, forKey: .location)
        prayer = try c.decodeIfPresent(PrayerSettings.self, forKey: .prayer)
        theme = (try? c.decodeIfPresent(ThemeStyle.self, forKey: .theme)) ?? .emerald
        custom = (try? c.decodeIfPresent(CustomColors.self, forKey: .custom)) ?? CustomColors()
        appearance = (try? c.decodeIfPresent(AppearanceMode.self, forKey: .appearance)) ?? .system
        lastRead = try? c.decodeIfPresent(QuranPosition.self, forKey: .lastRead)
        bookmarks = (try? c.decodeIfPresent([QuranPosition].self, forKey: .bookmarks)) ?? []
        alerts = (try? c.decodeIfPresent(AlertSettings.self, forKey: .alerts)) ?? AlertSettings()
        tasbih = (try? c.decodeIfPresent(TasbihState.self, forKey: .tasbih)) ?? TasbihState()
        reciterID = (try? c.decodeIfPresent(String.self, forKey: .reciterID)) ?? Reciter.alafasy.rawValue
        tafsirID = (try? c.decodeIfPresent(String.self, forKey: .tafsirID)) ?? TafsirSource.jalalayn.rawValue
        timetableKind = (try? c.decodeIfPresent(MonthCalendar.Kind.self, forKey: .timetableKind)) ?? .gregorian
        adhkarDay = (try? c.decodeIfPresent(String.self, forKey: .adhkarDay)) ?? ""
        adhkarCounts = (try? c.decodeIfPresent([String: Int].self, forKey: .adhkarCounts)) ?? [:]
    }
}

/// The app's state: settings (saved on every change) and the city list (loaded when first needed).
@MainActor
@Observable
final class AppModel {
    private(set) var settings: AppSettings
    private(set) var cities: CityIndex?
    private(set) var quran: Quran?
    private(set) var adhkar: [AdhkarCategory]?
    private(set) var hadith: HadithLibrary?
    private var jalalayn: [Int: [Int: String]]?
    private var pickthall: [Int: [Int: String]]?
    /// Screenshot mode (`-DeenDemo today|more|settings|onboarding`): fixed sample data, nothing saved.
    let demo: String?

    nonisolated static let storageKey = "deen.settings"

    /// Settings as saved, for work done without the UI (the background refresh).
    nonisolated static func savedSettings() -> AppSettings? {
        guard let data = UserDefaults.standard.data(forKey: storageKey) else { return nil }
        return try? JSONDecoder().decode(AppSettings.self, from: data)
    }

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

    /// The Tanzil text and metadata (shared with Android), parsed once off the main thread.
    func loadQuran() async {
        guard quran == nil,
              let text = Bundle.main.url(forResource: "quran-uthmani", withExtension: "txt"),
              let meta = Bundle.main.url(forResource: "quran-meta", withExtension: "json") else { return }
        quran = await Task.detached(priority: .userInitiated) { try? Quran.load(text: text, meta: meta) }.value
    }

    func loadAdhkar() async {
        guard adhkar == nil, let url = Bundle.main.url(forResource: "adhkar", withExtension: "json") else { return }
        adhkar = await Task.detached(priority: .userInitiated) { try? AdhkarLibrary.load(from: url) }.value
    }

    /// The hadith folder (collections, books, an-Nawawi's Forty) and the facts about the Prophet ﷺ.
    func loadHadith() async {
        guard hadith == nil, let folder = Bundle.main.resourceURL?.appendingPathComponent("hadith"),
              let facts = Bundle.main.url(forResource: "prophet-facts", withExtension: "json") else { return }
        hadith = await Task.detached(priority: .userInitiated) { try? HadithLibrary.load(hadithFolder: folder, facts: facts) }.value
    }

    // MARK: Tafsir

    /// The surah's tafsir from the chosen source: al-Jalalayn from the app, the others downloaded once
    /// and kept. Nil when a download is needed and there is no internet.
    func tafsir(surah: Surah) async -> [TafsirEntry]? {
        let source = TafsirSource(rawValue: settings.tafsirID) ?? .jalalayn
        if source.bundled {
            if jalalayn == nil { jalalayn = await Self.table("jalalayn") }
            return tafsirEntries(jalalayn?[surah.number] ?? [:], ayahCount: surah.ayahCount)
        }
        let cache = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        guard let texts = await RemoteTafsir.fetch(source, surah: surah.number, cache: cache) else { return nil }
        return tafsirEntries(texts, ayahCount: surah.ayahCount)
    }

    /// Pickthall's English meaning (1930), shown with the tafsir outside Arabic.
    func meaning(surah: Int) async -> [Int: String] {
        if pickthall == nil { pickthall = await Self.table("pickthall") }
        return pickthall?[surah] ?? [:]
    }

    private static func table(_ name: String) async -> [Int: [Int: String]]? {
        guard let url = Bundle.main.url(forResource: name, withExtension: "tsv") else { return nil }
        return await Task.detached { (try? String(contentsOf: url, encoding: .utf8)).map(parseVerseTable) }.value
    }

    // MARK: Adhkar counts

    func adhkarCount(category: String, item: Int) -> Int {
        settings.adhkarDay == todayKey ? settings.adhkarCounts["\(category)/\(item)"] ?? 0 : 0
    }

    func adhkarDone(in category: AdhkarCategory) -> Int {
        category.items.filter { adhkarCount(category: category.id, item: $0.id) >= $0.count }.count
    }

    /// One more; returns true when this dhikr just reached its count.
    @discardableResult
    func countDhikr(category: String, item: Int, total: Int) -> Bool {
        let key = "\(category)/\(item)"
        var finished = false
        update { s in
            if s.adhkarDay != todayKey {
                s.adhkarDay = todayKey
                s.adhkarCounts = [:]
            }
            let next = min((s.adhkarCounts[key] ?? 0) + 1, total)
            s.adhkarCounts[key] = next
            finished = next == total
        }
        return finished
    }

    func resetAdhkar(category: String) {
        update { s in s.adhkarCounts = s.adhkarCounts.filter { !$0.key.hasPrefix("\(category)/") } }
    }

    func countTasbih() {
        update { s in
            if s.tasbih.day != todayKey {
                s.tasbih.day = todayKey
                s.tasbih.todayTotal = 0
            }
            s.tasbih.count += 1
            s.tasbih.todayTotal += 1
            s.tasbih.allTime += 1
        }
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
        s.lastRead = QuranPosition(surah: 18, ayah: 10)
        return s
    }
}
