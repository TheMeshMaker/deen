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

@Test func adhkarAndHadithLoad() throws {
    let assets = repoRoot.appendingPathComponent("core/content/src/main/assets")
    let adhkar = try AdhkarLibrary.load(from: assets.appendingPathComponent("adhkar.json"))
    #expect(adhkar.count >= 9)
    #expect(adhkar.first?.id == "morning")
    #expect(adhkar.flatMap(\.items).allSatisfy { $0.virtue != nil || $0.sunnah != nil })
    #expect(adhkar.flatMap(\.items).allSatisfy { $0.text != nil || $0.isQuran })

    let library = try HadithLibrary.load(hadithFolder: assets.appendingPathComponent("hadith"),
                                         facts: assets.appendingPathComponent("prophet-facts.json"))
    #expect(library.nawawi.count == 42)
    #expect(library.facts.count > 50)
    let bukhari = try #require(library.collections.first { $0.id == "bukhari" })
    #expect(bukhari.count > 7000)
    let book = try #require(bukhari.books.first)
    let entries = library.hadiths(collection: "bukhari", book: book.book)
    #expect(entries.count == book.count)
    #expect(entries.first?.text.isEmpty == false)
    // Same day, same hadith for everyone; consecutive days differ.
    let a = DailyContent.pick(library.nawawi, year: 2026, month: 10, day: 11)
    let b = DailyContent.pick(library.nawawi, year: 2026, month: 10, day: 12)
    #expect(a != nil && a != b)
}

@Test func wordTimingsCoverEveryVerse() throws {
    let text = try String(contentsOf: repoRoot.appendingPathComponent("core/content/src/main/assets/audio/timings/Alafasy_128kbps.tsv"), encoding: .utf8)
    let table = parseTimings(text)
    #expect(table.count == 6236)
    let kursi = try #require(table[2 * 1000 + 255])
    #expect(kursi.count > 40)
    #expect(kursi.wordAt(-1) == nil)
    #expect(kursi.wordAt(kursi[0].startMs) == kursi[0].firstWord)
    // Between two words the previous one stays lit.
    #expect(kursi.wordAt(kursi[1].startMs - 1) == kursi[0].firstWord)
}

@Test func tafsirTablesAndGrouping() throws {
    let jalalayn = parseVerseTable(try String(contentsOf: repoRoot.appendingPathComponent("core/content/src/main/assets/tafsir/jalalayn.tsv"), encoding: .utf8))
    #expect(jalalayn.count == 114)
    #expect(jalalayn[2]?[255]?.isEmpty == false)
    let grouped = tafsirEntries([1: "", 2: "a", 4: "b"], ayahCount: 4)
    #expect(grouped[0].coveredBy == 2) // an opening verse points forward
    #expect(grouped[2].coveredBy == 2)
    #expect(grouped[3].coveredBy == nil)
    #expect(cleanHTML("<p>one</p><p>two<br>three</p>") == "one\n\ntwo\nthree")
}

@Test func monthDays() {
    let october = MonthCalendar.days(year: 2026, month: 10, kind: .gregorian)
    #expect(october.count == 31)
    #expect(october.first.map { [$0.year, $0.month, $0.day] } == [2026, 10, 1])
    let ramadan = MonthCalendar.days(year: 1446, month: 9, kind: .hijri)
    #expect((29...30).contains(ramadan.count))
    #expect(ramadan.first.map { [$0.year, $0.month, $0.day] } == [2025, 3, 1])
    #expect(ramadan.allSatisfy { $0.hijri.month == 9 })
}

@Test func islamicDaysAreOnTheirHijriDates() throws {
    let today = LocalDay(year: 2026, month: 10, day: 13)
    let events = IslamicEvents.upcoming(today: today)
    #expect(Set(events.map(\.event)) == Set(IslamicEvent.allCases))
    #expect(events.map(\.start) == events.map(\.start).sorted())
    for e in events {
        #expect(e.start.hijri.day == e.event.day)
        #expect(e.end >= today)
        if e.event != .whiteDays { #expect(e.hijri.month == e.event.month) }
    }
    let byEvent = Dictionary(uniqueKeysWithValues: events.map { ($0.event, $0) })
    let ramadan = try #require(byEvent[.ramadan])
    // 1 Ramadan 1448 is 8 February 2027 on the Umm al-Qura calendar (as on Android).
    #expect(ramadan.start == LocalDay(year: 2027, month: 2, day: 8))
    #expect(ramadan.daysAway == 118)
    #expect(byEvent[.arafah]!.start.adding(days: 1) == byEvent[.eidAlAdha]!.start)
    // A moon-sighting correction moves the days the other way.
    #expect(IslamicEvents.upcoming(today: today, offsetDays: 1).first { $0.event == .ramadan }?.start == ramadan.start.adding(days: -1))
}

@Test func nightTimesSplitTheNight() throws {
    let makkah = SavedLocation(name: "Makkah", coordinates: Coordinates(latitude: 21.4225, longitude: 39.8262), timeZoneID: "Asia/Riyadh")
    let settings = PrayerSettings(method: .ummAlQura)
    var calendar = Calendar(identifier: .gregorian)
    calendar.timeZone = makkah.timeZone
    let evening = calendar.date(from: DateComponents(year: 2026, month: 10, day: 13, hour: 21))!
    let night = try #require(NightTimes.of(now: evening, location: makkah, settings: settings))
    #expect(night.maghrib < evening && evening < night.fajr)
    #expect(abs(night.midnight.timeIntervalSince(night.maghrib) - night.fajr.timeIntervalSince(night.midnight)) < 1)
    #expect(abs(night.lastThird.timeIntervalSince(night.maghrib) - night.fajr.timeIntervalSince(night.maghrib) * 2 / 3) < 1)
    let early = calendar.date(from: DateComponents(year: 2026, month: 10, day: 14, hour: 2))!
    #expect(NightTimes.of(now: early, location: makkah, settings: settings)?.fajr == night.fajr)
}

@Test func trackerStreaks() {
    let today = LocalDay(year: 2026, month: 10, day: 13)
    let all = Set(Prayer.prayers)
    var prayed: [LocalDay: Set<Prayer>] = [
        today.adding(days: -1): all, today.adding(days: -2): all, today.adding(days: -3): all.subtracting([.asr]),
        today: [.fajr, .sunrise]
    ]
    #expect(PrayerTracker.count(prayed, today) == 1)
    #expect(PrayerTracker.streak(prayed, today: today) == 2)
    prayed[today] = all
    #expect(PrayerTracker.streak(prayed, today: today) == 3)
    #expect(PrayerTracker.bestStreak(prayed) == 3)
    let totals = PrayerTracker.monthTotals(prayed, year: 2026, month: 10, today: today)
    #expect(totals.due == 13 * 5)
    #expect(totals.done == 5 + 5 + 5 + 4)
}

@Test func khatmahPortions() throws {
    let base = repoRoot.appendingPathComponent("core/content/src/main/assets/quran")
    let quran = try Quran.load(text: base.appendingPathComponent("quran-uthmani.txt"), meta: base.appendingPathComponent("quran-meta.json"))
    let pages = quran.pages
    #expect(pages.count == 604)
    #expect(pages.page(of: QuranPosition(surah: 1, ayah: 1)) == 1)
    #expect(pages.page(of: QuranPosition(surah: 2, ayah: 142)) == 22)
    #expect(pages.page(of: QuranPosition(surah: 114, ayah: 6)) == 604)
    let start = LocalDay(year: 2026, month: 10, day: 1)
    let plan = KhatmahPlan(startedOn: start, days: 30)
    let first = KhatmahProgress.of(plan, lastRead: nil, pages: pages, today: start)
    #expect(first.todayFrom == 1 && first.todayTo == 21 && first.pagesLeftToday == 21)
    let later = KhatmahProgress.of(plan, lastRead: QuranPosition(surah: 2, ayah: 142), pages: pages, today: start.adding(days: 1))
    #expect(later.pagesRead == 21)
    #expect(later.todayTo == 41)
    #expect(KhatmahProgress.of(plan, lastRead: QuranPosition(surah: 114, ayah: 6), pages: pages, today: start).complete)
}

@Test func kahfReminderOnFridayMorning() throws {
    let makkah = SavedLocation(name: "Makkah", coordinates: Coordinates(latitude: 21.4225, longitude: 39.8262), timeZoneID: "Asia/Riyadh")
    let settings = PrayerSettings(method: .ummAlQura)
    var calendar = Calendar(identifier: .gregorian)
    calendar.timeZone = makkah.timeZone
    let monday = calendar.date(from: DateComponents(year: 2026, month: 10, day: 12, hour: 8))!
    let plan = AlertPlanner.plan(now: monday, location: makkah, prayer: settings, alerts: AlertSettings(kahfReminder: true), days: 7)
    let kahf = try #require(plan.first { $0.kind == .kahf })
    #expect(calendar.component(.weekday, from: kahf.at) == 6)
    let friday = try #require(PrayerEngine.day(year: 2026, month: 10, day: 16, at: makkah.coordinates, settings: settings))
    #expect(kahf.at == friday[.sunrise].addingTimeInterval(3600))
    #expect(!AlertPlanner.plan(now: monday, location: makkah, prayer: settings, alerts: AlertSettings(), days: 7).contains { $0.kind == .kahf })
}
