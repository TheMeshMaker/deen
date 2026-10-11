import Adhan
import Foundation

/// One calendar day's times at a place (instants; show them in the place's time zone).
public struct PrayerDay: Sendable {
    public let year: Int
    public let month: Int
    public let day: Int
    public let times: [Prayer: Date]

    public subscript(prayer: Prayer) -> Date { times[prayer] ?? .distantFuture }
}

/// The prayer that comes next, and the one whose time we are in.
public struct NextPrayer: Sendable, Equatable {
    public let prayer: Prayer
    public let at: Date
    public let previous: Prayer
    public let previousAt: Date

    /// 0...1: how far we are from [previousAt] to [at].
    public func progress(now: Date) -> Double {
        let total = at.timeIntervalSince(previousAt)
        guard total > 0 else { return 1 }
        return min(max(now.timeIntervalSince(previousAt) / total, 0), 1)
    }
}

/// Offline prayer times, exactly as on Android (core:prayer's PrayerTimeCalculator).
public enum PrayerEngine {
    static let ramadanIshaExtensionMinutes = 30
    static let highLatitudeThreshold = 48.0

    /// Times for the Gregorian date [year]-[month]-[day] at [coordinates].
    public static func day(year: Int, month: Int, day: Int, at coordinates: Coordinates, settings: PrayerSettings) -> PrayerDay? {
        var parameters = settings.method.adhanMethod.params
        parameters.madhab = settings.asrMethod == .hanafi ? .hanafi : .shafi
        parameters.highLatitudeRule = settings.highLatitudeRule.adhanRule(latitude: coordinates.latitude)
        parameters.adjustments = adjustments(year: year, month: month, day: day, settings: settings)
        let date = DateComponents(year: year, month: month, day: day)
        guard let times = Adhan.PrayerTimes(
            coordinates: Adhan.Coordinates(latitude: coordinates.latitude, longitude: coordinates.longitude),
            date: date,
            calculationParameters: parameters
        ) else { return nil }
        return PrayerDay(year: year, month: month, day: day, times: [
            .fajr: times.fajr, .sunrise: times.sunrise, .dhuhr: times.dhuhr,
            .asr: times.asr, .maghrib: times.maghrib, .isha: times.isha
        ])
    }

    /// Today's times where the user is, plus the next prayer (across midnight too).
    public static func today(now: Date, location: SavedLocation, settings: PrayerSettings) -> (today: PrayerDay, next: NextPrayer)? {
        let zone = location.timeZone
        let days = (-1...1).compactMap { offset -> PrayerDay? in
            let d = localDate(of: now.addingTimeInterval(Double(offset) * 86_400), in: zone)
            return day(year: d.year, month: d.month, day: d.day, at: location.coordinates, settings: settings)
        }
        guard days.count == 3 else { return nil }
        let events = days.flatMap { day in Prayer.prayers.map { ($0, day[$0]) } }
        let found = events.firstIndex { $0.1 > now } ?? events.count - 1
        let index = max(found, 1)
        let next = NextPrayer(prayer: events[index].0, at: events[index].1, previous: events[index - 1].0, previousAt: events[index - 1].1)
        return (days[1], next)
    }

    /// The calendar date of [instant] where the user is.
    public static func localDate(of instant: Date, in zone: TimeZone) -> (year: Int, month: Int, day: Int) {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = zone
        let c = calendar.dateComponents([.year, .month, .day], from: instant)
        return (c.year ?? 2000, c.month ?? 1, c.day ?? 1)
    }

    private static func adjustments(year: Int, month: Int, day: Int, settings: PrayerSettings) -> PrayerAdjustments {
        let offsets = settings.offsetMinutes
        let ramadan = settings.method == .ummAlQura && settings.ramadanIshaExtension &&
            HijriCalendar.hijri(year: year, month: month, day: day, offsetDays: settings.hijriOffsetDays).isRamadan
        return PrayerAdjustments(
            fajr: offsets[.fajr] ?? 0,
            sunrise: offsets[.sunrise] ?? 0,
            dhuhr: offsets[.dhuhr] ?? 0,
            asr: offsets[.asr] ?? 0,
            maghrib: offsets[.maghrib] ?? 0,
            isha: (offsets[.isha] ?? 0) + (ramadan ? ramadanIshaExtensionMinutes : 0)
        )
    }
}

extension CalculationMethod {
    var adhanMethod: Adhan.CalculationMethod {
        switch self {
        case .ummAlQura: return .ummAlQura
        case .muslimWorldLeague: return .muslimWorldLeague
        case .northAmerica: return .northAmerica
        case .egyptian: return .egyptian
        case .karachi: return .karachi
        case .dubai: return .dubai
        case .kuwait: return .kuwait
        case .qatar: return .qatar
        case .singapore: return .singapore
        case .moonsightingCommittee: return .moonsightingCommittee
        case .turkey: return .turkey
        }
    }
}

extension HighLatitudeRule {
    /// adhan's own "recommended" rule only looks at northern latitudes, so AUTO is resolved here.
    func adhanRule(latitude: Double) -> Adhan.HighLatitudeRule {
        switch self {
        case .auto: return abs(latitude) > PrayerEngine.highLatitudeThreshold ? .seventhOfTheNight : .middleOfTheNight
        case .middleOfTheNight: return .middleOfTheNight
        case .seventhOfTheNight: return .seventhOfTheNight
        case .twilightAngle: return .twilightAngle
        }
    }
}
