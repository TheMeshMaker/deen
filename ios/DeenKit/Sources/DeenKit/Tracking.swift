import Foundation

private let utc = TimeZone(identifier: "UTC")!

private func calendar(_ id: Calendar.Identifier) -> Calendar {
    var c = Calendar(identifier: id)
    c.timeZone = utc
    return c
}

/// A calendar date without a time or zone: a day at the user's place.
public struct LocalDay: Hashable, Comparable, Codable, Sendable {
    public let year: Int
    public let month: Int
    public let day: Int

    public init(year: Int, month: Int, day: Int) {
        self.year = year
        self.month = month
        self.day = day
    }

    /// "2026-10-13".
    public init?(key: String) {
        let parts = key.split(separator: "-").compactMap { Int($0) }
        guard parts.count == 3 else { return nil }
        self.init(year: parts[0], month: parts[1], day: parts[2])
    }

    public var key: String { String(format: "%04d-%02d-%02d", year, month, day) }

    /// Noon UTC on this date: a stable instant for date arithmetic (and for formatting the date with a
    /// UTC formatter).
    public var noon: Date { calendar(.gregorian).date(from: DateComponents(year: year, month: month, day: day, hour: 12)) ?? Date() }

    init(_ date: Date) {
        let c = calendar(.gregorian).dateComponents([.year, .month, .day], from: date)
        self.init(year: c.year ?? 2000, month: c.month ?? 1, day: c.day ?? 1)
    }

    /// Today where the user is.
    public static func today(in zone: TimeZone, now: Date = Date()) -> LocalDay {
        let d = PrayerEngine.localDate(of: now, in: zone)
        return LocalDay(year: d.year, month: d.month, day: d.day)
    }

    public func adding(days: Int) -> LocalDay { LocalDay(noon.addingTimeInterval(Double(days) * 86_400)) }

    public func days(until other: LocalDay) -> Int { Int((other.noon.timeIntervalSince(noon) / 86_400).rounded()) }

    /// 1 = Sunday … 7 = Saturday.
    public var weekday: Int { calendar(.gregorian).component(.weekday, from: noon) }

    public var hijri: HijriDate { HijriCalendar.hijri(year: year, month: month, day: day) }

    public static func < (a: LocalDay, b: LocalDay) -> Bool { (a.year, a.month, a.day) < (b.year, b.month, b.day) }
}

// MARK: - Islamic days

/// Days of the Islamic year that most Muslims mark (Umm al-Qura calendar), as on Android. [days] is how
/// long each lasts (0 = the whole month); the white days (13th–15th) recur every month.
public enum IslamicEvent: String, CaseIterable, Sendable {
    case newYear, ashura, ramadan, lastTenNights, eidAlFitr, arafah, eidAlAdha, whiteDays

    public var month: Int {
        switch self {
        case .newYear, .ashura: return 1
        case .ramadan, .lastTenNights: return 9
        case .eidAlFitr: return 10
        case .arafah, .eidAlAdha: return 12
        case .whiteDays: return 0
        }
    }

    public var day: Int {
        switch self {
        case .newYear, .ramadan, .eidAlFitr: return 1
        case .ashura, .eidAlAdha: return 10
        case .lastTenNights: return 21
        case .arafah: return 9
        case .whiteDays: return 13
        }
    }

    public var days: Int {
        switch self {
        case .ramadan: return 0
        case .lastTenNights: return 10
        case .whiteDays: return 3
        default: return 1
        }
    }
}

/// One coming (or current) occurrence of an Islamic day; [daysAway] is 0 while it is on.
public struct UpcomingEvent: Hashable, Sendable {
    public let event: IslamicEvent
    public let start: LocalDay
    public let end: LocalDay
    public let hijri: HijriDate
    public let daysAway: Int

    public var ongoing: Bool { daysAway == 0 }
}

public enum IslamicEvents {
    /// The next occurrence of each day that hasn't ended by [today], soonest first.
    public static func upcoming(today: LocalDay, offsetDays: Int = 0) -> [UpcomingEvent] {
        let shifted = today.adding(days: offsetDays).hijri
        return IslamicEvent.allCases.compactMap { event -> UpcomingEvent? in
            let candidates: [(Int, Int)] = event == .whiteDays
                ? (0...13).map { step -> (Int, Int) in
                    let i = shifted.month - 1 + step
                    return (shifted.year + i / 12, i % 12 + 1)
                }
                : [(shifted.year, event.month), (shifted.year + 1, event.month)]
            for (year, month) in candidates {
                if let found = occurrence(event, year: year, month: month, today: today, offsetDays: offsetDays) { return found }
            }
            return nil
        }
        .sorted { ($0.start, IslamicEvent.allCases.firstIndex(of: $0.event)!) < ($1.start, IslamicEvent.allCases.firstIndex(of: $1.event)!) }
    }

    /// The one day to feature: the soonest, preferring the year's days to the monthly white days.
    public static func next(today: LocalDay, offsetDays: Int = 0) -> UpcomingEvent? {
        let all = upcoming(today: today, offsetDays: offsetDays)
        let yearly = all.first { $0.event != .whiteDays }
        let white = all.first { $0.event == .whiteDays }
        guard let yearly else { return white }
        if let white, white.ongoing || white.daysAway < 3, !yearly.ongoing, yearly.daysAway > 7 { return white }
        return yearly
    }

    private static func occurrence(_ event: IslamicEvent, year: Int, month: Int, today: LocalDay, offsetDays: Int) -> UpcomingEvent? {
        let islamic = calendar(.islamicUmmAlQura)
        guard let date = islamic.date(from: DateComponents(year: year, month: month, day: event.day, hour: 12)),
              islamic.component(.day, from: date) == event.day else { return nil }
        let length = event.days == 0 ? (islamic.range(of: .day, in: .month, for: date)?.count ?? 29) : event.days
        let start = LocalDay(date).adding(days: -offsetDays)
        let end = start.adding(days: length - 1)
        guard end >= today else { return nil }
        return UpcomingEvent(event: event, start: start, end: end, hijri: HijriDate(year: year, month: month, day: event.day),
                             daysAway: max(0, today.days(until: start)))
    }
}

// MARK: - The night

/// The middle of the night (the end of ʿIshāʾ's preferred time) and the start of its last third.
public struct NightTimes: Sendable {
    public let maghrib: Date
    public let fajr: Date
    public let midnight: Date
    public let lastThird: Date

    /// Tonight, or the night we are in if it is before today's Fajr.
    public static func of(now: Date, location: SavedLocation, settings: PrayerSettings) -> NightTimes? {
        let today = LocalDay.today(in: location.timeZone, now: now)
        func day(_ d: LocalDay) -> PrayerDay? { PrayerEngine.day(year: d.year, month: d.month, day: d.day, at: location.coordinates, settings: settings) }
        guard let current = day(today) else { return nil }
        let pair: (Date, Date)?
        if now < current[.fajr] {
            pair = day(today.adding(days: -1)).map { ($0[.maghrib], current[.fajr]) }
        } else {
            pair = day(today.adding(days: 1)).map { (current[.maghrib], $0[.fajr]) }
        }
        guard let pair else { return nil }
        let (start, end) = pair
        let length = end.timeIntervalSince(start)
        return NightTimes(maghrib: start, fajr: end, midnight: start.addingTimeInterval(length / 2), lastThird: start.addingTimeInterval(length * 2 / 3))
    }
}

// MARK: - Prayer tracker

/// Summaries of the prayers ticked off (kept on the phone only), as on Android.
public enum PrayerTracker {
    public static func count(_ prayed: [LocalDay: Set<Prayer>], _ day: LocalDay) -> Int { prayed[day]?.filter(\.isPrayer).count ?? 0 }

    public static func complete(_ prayed: [LocalDay: Set<Prayer>], _ day: LocalDay) -> Bool { count(prayed, day) == Prayer.prayers.count }

    /// Days in a row with all five, ending today (or yesterday while today is still going).
    public static func streak(_ prayed: [LocalDay: Set<Prayer>], today: LocalDay) -> Int {
        var day = complete(prayed, today) ? today : today.adding(days: -1)
        var n = 0
        while complete(prayed, day) {
            n += 1
            day = day.adding(days: -1)
        }
        return n
    }

    public static func bestStreak(_ prayed: [LocalDay: Set<Prayer>]) -> Int {
        var best = 0, run = 0
        var previous: LocalDay?
        for day in prayed.keys.filter({ complete(prayed, $0) }).sorted() {
            run = previous.map { $0.adding(days: 1) == day } == true ? run + 1 : 1
            best = max(best, run)
            previous = day
        }
        return best
    }

    /// Ticked off in [year]-[month] up to [today], out of how many were due.
    public static func monthTotals(_ prayed: [LocalDay: Set<Prayer>], year: Int, month: Int, today: LocalDay) -> (done: Int, due: Int) {
        let first = LocalDay(year: year, month: month, day: 1)
        guard first <= today else { return (0, 0) }
        var done = 0, days = 0
        var day = first
        while day.month == month, day <= today {
            done += count(prayed, day)
            days += 1
            day = day.adding(days: 1)
        }
        return (done, days * Prayer.prayers.count)
    }
}

// MARK: - Khatmah

/// Reading the whole Quran in [days] days from Madani page [startPage], begun on [startedOn].
public struct KhatmahPlan: Codable, Equatable, Sendable {
    public let startedOn: LocalDay
    public let days: Int
    public let startPage: Int

    public static let pages = 604
    public static let dayChoices = [7, 10, 15, 20, 30, 60]

    public init(startedOn: LocalDay, days: Int, startPage: Int = 1) {
        self.startedOn = startedOn
        self.days = days
        self.startPage = startPage
    }
}

/// The Madani page starts (from the Tanzil metadata) and lookups by verse.
public struct QuranPages: Sendable {
    public let starts: [QuranPosition]

    public init(starts: [QuranPosition]) { self.starts = starts }

    public var count: Int { starts.count }

    /// The page [position] is on, 1-based.
    public func page(of position: QuranPosition) -> Int {
        var low = 0, high = starts.count - 1
        while low < high {
            let mid = (low + high + 1) / 2
            let s = starts[mid]
            if (s.surah, s.ayah) > (position.surah, position.ayah) { high = mid - 1 } else { low = mid }
        }
        return low + 1
    }

    public func start(of page: Int) -> QuranPosition { starts[min(max(page - 1, 0), starts.count - 1)] }
}

/// Where a plan stands today: [todayFrom]...[todayTo] is what is left of today's portion.
public struct KhatmahProgress: Sendable {
    public let plan: KhatmahPlan
    public let day: Int
    public let pagesRead: Int
    public let totalPages: Int
    public let todayFrom: Int
    public let todayTo: Int
    public let pagesLeftToday: Int
    /// Positive: pages ahead of schedule; negative: behind.
    public let ahead: Int
    public let complete: Bool

    public var fraction: Double { totalPages == 0 ? 0 : Double(pagesRead) / Double(totalPages) }

    public static func of(_ plan: KhatmahPlan, lastRead: QuranPosition?, pages: QuranPages, today: LocalDay) -> KhatmahProgress {
        let total = pages.count - plan.startPage + 1
        let current = max(lastRead.map(pages.page(of:)) ?? plan.startPage, plan.startPage)
        let complete = lastRead.map { $0.surah == 114 && $0.ayah >= 6 } ?? false
        let read = complete ? total : min(max(current - plan.startPage, 0), total)
        let day = max(plan.startedOn.days(until: today) + 1, 1)
        let days = max(plan.days, 1)
        func due(by d: Int) -> Int { Int((Double(total) * Double(min(d, days)) / Double(days)).rounded(.up)) }
        let dueToday = due(by: day)
        return KhatmahProgress(
            plan: plan, day: day, pagesRead: read, totalPages: total,
            todayFrom: min(plan.startPage + read, pages.count), todayTo: min(plan.startPage + dueToday - 1, pages.count),
            pagesLeftToday: max(dueToday - read, 0), ahead: read - dueToday, complete: complete
        )
    }
}
