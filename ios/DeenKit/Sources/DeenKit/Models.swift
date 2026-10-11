import Foundation

/// The five prayers and sunrise (which is a time, not a prayer).
public enum Prayer: String, CaseIterable, Codable, Sendable, Hashable {
    case fajr, sunrise, dhuhr, asr, maghrib, isha

    public var isPrayer: Bool { self != .sunrise }

    public static let prayers: [Prayer] = [.fajr, .dhuhr, .asr, .maghrib, .isha]
}

/// Calculation methods offered in Deen (the same list as Android's core:model).
public enum CalculationMethod: String, CaseIterable, Codable, Sendable {
    case ummAlQura, muslimWorldLeague, northAmerica, egyptian, karachi, dubai, kuwait, qatar, singapore,
         moonsightingCommittee, turkey
}

public enum AsrMethod: String, CaseIterable, Codable, Sendable {
    /// Shāfiʿī, Mālikī, Ḥanbalī: shadow equals the object's length.
    case standard
    /// Ḥanafī: twice the length, so ʿAsr begins later.
    case hanafi
}

public enum HighLatitudeRule: String, CaseIterable, Codable, Sendable {
    /// Middle of the night below 48°, a seventh of the night above (decided per location).
    case auto
    case middleOfTheNight, seventhOfTheNight, twilightAngle
}

public struct PrayerSettings: Codable, Equatable, Sendable {
    public var method: CalculationMethod
    public var asrMethod: AsrMethod
    public var highLatitudeRule: HighLatitudeRule
    /// Minutes added to (or taken from) each time, e.g. to match the local mosque.
    public var offsetMinutes: [Prayer: Int]
    /// Umm al-Qura's Isha is 30 minutes later in Ramadan.
    public var ramadanIshaExtension: Bool
    /// Correction for local moon sighting, usually -1...+1.
    public var hijriOffsetDays: Int

    public init(
        method: CalculationMethod = .muslimWorldLeague,
        asrMethod: AsrMethod = .standard,
        highLatitudeRule: HighLatitudeRule = .auto,
        offsetMinutes: [Prayer: Int] = [:],
        ramadanIshaExtension: Bool = true,
        hijriOffsetDays: Int = 0
    ) {
        self.method = method
        self.asrMethod = asrMethod
        self.highLatitudeRule = highLatitudeRule
        self.offsetMinutes = offsetMinutes
        self.ramadanIshaExtension = ramadanIshaExtension
        self.hijriOffsetDays = hijriOffsetDays
    }
}

public struct Coordinates: Codable, Hashable, Sendable {
    public var latitude: Double
    public var longitude: Double

    public init(latitude: Double, longitude: Double) {
        self.latitude = latitude
        self.longitude = longitude
    }
}

/// Where the user is, as saved in settings. It never leaves the phone.
public struct SavedLocation: Codable, Equatable, Sendable {
    public var name: String
    public var arabicName: String?
    public var region: String?
    public var countryCode: String?
    public var coordinates: Coordinates
    public var timeZoneID: String

    public init(name: String, arabicName: String? = nil, region: String? = nil, countryCode: String? = nil,
                coordinates: Coordinates, timeZoneID: String) {
        self.name = name
        self.arabicName = arabicName
        self.region = region
        self.countryCode = countryCode
        self.coordinates = coordinates
        self.timeZoneID = timeZoneID
    }

    public var timeZone: TimeZone { TimeZone(identifier: timeZoneID) ?? .current }

    public func displayName(arabic: Bool) -> String { arabic ? (arabicName ?? name) : name }
}

/// A place from the bundled GeoNames list (see tools/geonames/build_cities.py).
public struct City: Hashable, Identifiable, Sendable {
    public let id: Int
    public let name: String
    public let arabicName: String?
    public let countryCode: String
    public let region: String
    public let coordinates: Coordinates
    public let population: Int
    public let timeZoneID: String

    public var asLocation: SavedLocation {
        SavedLocation(name: name, arabicName: arabicName, region: region, countryCode: countryCode,
                      coordinates: coordinates, timeZoneID: timeZoneID)
    }
}
