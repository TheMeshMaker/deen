import Adhan
import Foundation

public struct HijriDate: Equatable, Sendable {
    public let year: Int
    public let month: Int
    public let day: Int

    public var isRamadan: Bool { month == 9 }
}

/// The Umm al-Qura calendar (the same tables Android's java.time HijrahDate uses).
public enum HijriCalendar {
    private static let utc = TimeZone(identifier: "UTC")!

    public static func hijri(year: Int, month: Int, day: Int, offsetDays: Int = 0) -> HijriDate {
        var gregorian = Calendar(identifier: .gregorian)
        gregorian.timeZone = utc
        let noon = gregorian.date(from: DateComponents(year: year, month: month, day: day, hour: 12)) ?? Date()
        let shifted = noon.addingTimeInterval(Double(offsetDays) * 86_400)
        var islamic = Calendar(identifier: .islamicUmmAlQura)
        islamic.timeZone = utc
        let c = islamic.dateComponents([.year, .month, .day], from: shifted)
        return HijriDate(year: c.year ?? 1447, month: c.month ?? 1, day: c.day ?? 1)
    }
}

/// Direction of the Kaaba and how far away it is.
public enum QiblaFinder {
    public static let kaaba = Coordinates(latitude: 21.4225241, longitude: 39.8261818)

    /// Degrees clockwise from true north, 0..<360.
    public static func bearing(from point: Coordinates) -> Double {
        Adhan.Qibla(coordinates: Adhan.Coordinates(latitude: point.latitude, longitude: point.longitude)).direction
    }

    public static func distanceKm(from point: Coordinates) -> Double {
        let lat1 = point.latitude * .pi / 180
        let lat2 = kaaba.latitude * .pi / 180
        let dLat = lat2 - lat1
        let dLon = (kaaba.longitude - point.longitude) * .pi / 180
        let a = pow(sin(dLat / 2), 2) + cos(lat1) * cos(lat2) * pow(sin(dLon / 2), 2)
        return 2 * 6371.0 * asin(sqrt(a))
    }
}

/// Suggested defaults by country (ISO 3166-1 alpha-2); the user always confirms them.
public enum MethodSuggestions {
    private static let methodByCountry: [String: CalculationMethod] = {
        var map: [String: CalculationMethod] = [:]
        ["SA", "YE"].forEach { map[$0] = .ummAlQura }
        map["AE"] = .dubai
        map["KW"] = .kuwait
        map["QA"] = .qatar
        map["EG"] = .egyptian
        ["PK", "IN", "BD", "AF"].forEach { map[$0] = .karachi }
        ["US", "CA"].forEach { map[$0] = .northAmerica }
        ["GB", "IE"].forEach { map[$0] = .moonsightingCommittee }
        ["SG", "MY", "ID", "BN"].forEach { map[$0] = .singapore }
        map["TR"] = .turkey
        return map
    }()

    /// Countries where most Muslims follow the Hanafi school.
    private static let hanafiCountries: Set<String> = [
        "PK", "IN", "BD", "AF", "TR", "UZ", "TJ", "KZ", "KG", "TM", "BA", "AL", "XK", "MK"
    ]

    public static func method(countryCode: String?) -> CalculationMethod {
        countryCode.flatMap { methodByCountry[$0.uppercased()] } ?? .muslimWorldLeague
    }

    public static func asrMethod(countryCode: String?) -> AsrMethod {
        hanafiCountries.contains(countryCode?.uppercased() ?? "") ? .hanafi : .standard
    }
}
