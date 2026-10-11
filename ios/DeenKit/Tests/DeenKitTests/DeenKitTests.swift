import Foundation
import Testing
@testable import DeenKit

/// The repository root, found from this file (the tests read Android's fixtures and assets directly).
private let repoRoot = URL(fileURLWithPath: #filePath)
    .deletingLastPathComponent() // DeenKitTests
    .deletingLastPathComponent() // Tests
    .deletingLastPathComponent() // DeenKit
    .deletingLastPathComponent() // ios
    .deletingLastPathComponent()

/// Same published timetables as Android's OfficialTimetableTest (collected by the adhan project, MIT).
@Test func officialTimetablesMatch() throws {
    let folder = repoRoot.appendingPathComponent("core/prayer/src/test/resources/times")
    let files = try FileManager.default.contentsOfDirectory(at: folder, includingPropertiesForKeys: nil)
        .filter { $0.pathExtension == "json" }
    #expect(files.count >= 7)
    var failures: [String] = []
    for file in files {
        let json = try JSONSerialization.jsonObject(with: Data(contentsOf: file)) as! [String: Any]
        let params = json["params"] as! [String: Any]
        let variance = json["variance"] as? Int ?? 0
        let zone = TimeZone(identifier: params["timezone"] as! String)!
        let coordinates = Coordinates(latitude: number(params["latitude"]), longitude: number(params["longitude"]))
        let settings = PrayerSettings(
            method: method(params["method"] as! String),
            asrMethod: params["madhab"] as? String == "Hanafi" ? .hanafi : .standard,
            highLatitudeRule: {
                switch params["highLatitudeRule"] as? String {
                case "SeventhOfTheNight": return .seventhOfTheNight
                case "TwilightAngle": return .twilightAngle
                default: return .middleOfTheNight
                }
            }()
        )
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = zone
        for entry in json["times"] as! [[String: Any]] {
            let parts = (entry["date"] as! String).split(separator: "-").compactMap { Int($0) }
            let day = try #require(PrayerEngine.day(year: parts[0], month: parts[1], day: parts[2], at: coordinates, settings: settings))
            for prayer in Prayer.allCases {
                let expected = minutesOfDay(entry[prayer.rawValue] as! String)
                let c = calendar.dateComponents([.hour, .minute], from: day[prayer])
                let actual = (c.hour ?? 0) * 60 + (c.minute ?? 0)
                let diff = min(abs(actual - expected), 1440 - abs(actual - expected))
                if diff > variance {
                    failures.append("\(file.lastPathComponent) \(entry["date"]!) \(prayer): expected \(expected) got \(actual)")
                }
            }
        }
    }
    #expect(failures.isEmpty, "\(failures.joined(separator: "\n"))")
}

private func number(_ value: Any?) -> Double {
    if let d = value as? Double { return d }
    if let s = value as? String { return Double(s) ?? 0 }
    return (value as? NSNumber)?.doubleValue ?? 0
}

/// "5:43 am" → minutes after midnight.
private func minutesOfDay(_ text: String) -> Int {
    let lower = text.lowercased().trimmingCharacters(in: .whitespaces)
    let pm = lower.hasSuffix("pm")
    let clock = lower.replacingOccurrences(of: "am", with: "").replacingOccurrences(of: "pm", with: "").trimmingCharacters(in: .whitespaces)
    let hm = clock.split(separator: ":").compactMap { Int($0) }
    var hour = hm[0] % 12
    if pm { hour += 12 }
    return hour * 60 + hm[1]
}

private func method(_ name: String) -> CalculationMethod {
    switch name {
    case "UmmAlQura": return .ummAlQura
    case "MoonsightingCommittee": return .moonsightingCommittee
    case "Dubai": return .dubai
    case "Kuwait": return .kuwait
    case "Qatar": return .qatar
    case "Singapore": return .singapore
    case "Turkey": return .turkey
    case "MuslimWorldLeague": return .muslimWorldLeague
    case "Egyptian": return .egyptian
    case "Karachi": return .karachi
    case "NorthAmerica": return .northAmerica
    default: fatalError("Unknown method \(name)")
    }
}

@Test func hijriFollowsUmmAlQura() {
    // 1 Ramadan 1446 was 1 March 2025 in the Umm al-Qura calendar.
    let ramadan = HijriCalendar.hijri(year: 2025, month: 3, day: 1)
    #expect(ramadan == HijriDate(year: 1446, month: 9, day: 1))
    #expect(ramadan.isRamadan)
    #expect(HijriCalendar.hijri(year: 2025, month: 3, day: 1, offsetDays: -1).month == 8)
}

@Test func nextPrayerCrossesMidnight() throws {
    let makkah = SavedLocation(name: "Makkah", coordinates: Coordinates(latitude: 21.4225, longitude: 39.8262), timeZoneID: "Asia/Riyadh")
    let settings = PrayerSettings(method: .ummAlQura)
    var calendar = Calendar(identifier: .gregorian)
    calendar.timeZone = makkah.timeZone
    let lateNight = calendar.date(from: DateComponents(year: 2026, month: 1, day: 10, hour: 23, minute: 30))!
    let result = try #require(PrayerEngine.today(now: lateNight, location: makkah, settings: settings))
    #expect(result.next.prayer == .fajr)
    #expect(result.next.previous == .isha)
    #expect(result.next.at > lateNight)
    #expect((0...1).contains(result.next.progress(now: lateNight)))
}

@Test func citySearchFindsLatinAndArabicNames() throws {
    let index = try CityIndex.load(from: repoRoot.appendingPathComponent("core/location/src/main/assets/cities.tsv"))
    #expect(index.cities.count > 60_000)
    #expect(index.search("riy").first?.name == "Riyadh")
    #expect(index.search("الرياض").first?.name == "Riyadh")
    #expect(index.search("رياض").first?.name == "Riyadh")
    #expect(index.search("makkah").contains { $0.countryCode == "SA" })
    let near = try #require(index.nearest(to: Coordinates(latitude: 21.42, longitude: 39.83)))
    #expect(near.city.countryCode == "SA")
    #expect(near.km < 20)
}

@Test func suggestionsByCountry() {
    #expect(MethodSuggestions.method(countryCode: "sa") == .ummAlQura)
    #expect(MethodSuggestions.method(countryCode: "FR") == .muslimWorldLeague)
    #expect(MethodSuggestions.asrMethod(countryCode: "PK") == .hanafi)
}

@Test func qiblaFromLondon() {
    let bearing = QiblaFinder.bearing(from: Coordinates(latitude: 51.5072, longitude: -0.1276))
    #expect(abs(bearing - 118.99) < 0.5)
    #expect(abs(QiblaFinder.distanceKm(from: Coordinates(latitude: 51.5072, longitude: -0.1276)) - 4790) < 60)
}

@Test func quranLoadsEveryVerseAndSplitsTheBismillah() throws {
    let base = repoRoot.appendingPathComponent("core/content/src/main/assets/quran")
    let quran = try Quran.load(text: base.appendingPathComponent("quran-uthmani.txt"), meta: base.appendingPathComponent("quran-meta.json"))
    #expect(quran.surahs.count == 114)
    #expect(quran.surahs.reduce(0) { $0 + quran.ayahs(of: $1.number).count } == 6236)
    #expect(quran.surahs.allSatisfy { quran.ayahs(of: $0.number).count == $0.ayahCount })
    #expect(quran.ayahs(of: 1).first?.bismillah == nil)
    #expect(quran.ayahs(of: 2).first?.bismillah?.split(separator: " ").count == 4)
    #expect(quran.ayahs(of: 9).first?.bismillah == nil)
    #expect(quran.search("baqara").first?.number == 2)
    #expect(quran.search("البقرة").first?.number == 2)
    #expect(quran.search("36").first?.number == 36)
    #expect(quran.juz.count == 30)
}

@Test func alertPlanMatchesTheSettings() throws {
    let makkah = SavedLocation(name: "Makkah", coordinates: Coordinates(latitude: 21.4225, longitude: 39.8262), timeZoneID: "Asia/Riyadh")
    let settings = PrayerSettings(method: .ummAlQura)
    var calendar = Calendar(identifier: .gregorian)
    calendar.timeZone = makkah.timeZone
    let morning = calendar.date(from: DateComponents(year: 2026, month: 1, day: 10, hour: 8))!

    // Defaults: an adhan for the five prayers, nothing at sunrise, no reminders.
    let plain = AlertPlanner.plan(now: morning, location: makkah, prayer: settings, alerts: AlertSettings(), days: 2)
    #expect(plain.allSatisfy { $0.kind == .adhan && $0.prayer.isPrayer })
    #expect(plain.first?.prayer == .dhuhr)
    #expect(plain == plain.sorted { $0.at < $1.at })

    var alerts = AlertSettings(reminderMinutes: 10, afterAdhanMinutes: [.dhuhr: 15])
    alerts.modes[.isha] = .notification
    alerts.modes[.asr] = .silent
    let plan = AlertPlanner.plan(now: morning, location: makkah, prayer: settings, alerts: alerts, days: 1)
    let dhuhr = try #require(plan.first { $0.prayer == .dhuhr && $0.kind == .adhan })
    let before = try #require(plan.first { $0.prayer == .dhuhr && $0.kind == .reminder })
    let after = try #require(plan.first { $0.prayer == .dhuhr && $0.kind == .afterAdhan })
    #expect(before.at == dhuhr.at.addingTimeInterval(-600))
    #expect(after.at == dhuhr.at.addingTimeInterval(900))
    #expect(plan.contains { $0.prayer == .isha && $0.kind == .notification })
    #expect(!plan.contains { $0.prayer == .asr })
    #expect(AlertSettings(modes: [.sunrise: .adhan]).mode(for: .sunrise) == .notification)
}
