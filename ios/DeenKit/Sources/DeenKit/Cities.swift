import Foundation

/// Folds text for forgiving city search in Latin and Arabic script: case, accents, Arabic
/// diacritics, hamza seats (أ إ آ → ا), ة/ه, ى/ي, tatweel and punctuation are ignored.
public enum TextNormalizer {
    public static func normalize(_ text: String) -> String {
        let decomposed = text.lowercased().decomposedStringWithCanonicalMapping
        var scalars = String.UnicodeScalarView()
        for scalar in decomposed.unicodeScalars where scalar.properties.generalCategory != .nonspacingMark {
            scalars.append(scalar)
        }
        var out = String(scalars)
            .replacingOccurrences(of: "ٱ", with: "ا")
            .replacingOccurrences(of: "ة", with: "ه")
            .replacingOccurrences(of: "ى", with: "ي")
            .replacingOccurrences(of: "ـ", with: "")
        out = out.replacingOccurrences(of: "[\\s\\-_'’ʻʼʿʾ`.,()]+", with: " ", options: .regularExpression)
        return out.trimmingCharacters(in: .whitespaces)
    }

    /// One searchable string per city; Arabic words are also indexed without the article "ال".
    public static func searchKey(name: String, arabicName: String?) -> String {
        var key = normalize(name)
        if let arabicName, !arabicName.isEmpty {
            let arabic = normalize(arabicName)
            key += " " + arabic
            for word in arabic.split(separator: " ") where word.hasPrefix("ال") && word.count > 3 {
                key += " " + word.dropFirst(2)
            }
        }
        return key
    }
}

/// In-memory index over the bundled GeoNames cities (rows sorted by population, biggest first).
public final class CityIndex: @unchecked Sendable {
    public let cities: [City]
    private let keys: [String]

    private init(cities: [City], keys: [String]) {
        self.cities = cities
        self.keys = keys
    }

    /// Parses cities.tsv: id, name, Arabic name, country, region, latitude, longitude, population, time zone.
    public static func load(from url: URL) throws -> CityIndex {
        let text = try String(contentsOf: url, encoding: .utf8)
        var cities: [City] = []
        var keys: [String] = []
        cities.reserveCapacity(70_000)
        keys.reserveCapacity(70_000)
        text.enumerateLines { line, _ in
            let c = line.split(separator: "\t", omittingEmptySubsequences: false).map(String.init)
            guard c.count >= 9, let id = Int(c[0]), let lat = Double(c[5]), let lon = Double(c[6]) else { return }
            let arabic = c[2].isEmpty ? nil : c[2]
            cities.append(City(
                id: id, name: c[1], arabicName: arabic, countryCode: c[3], region: c[4],
                coordinates: Coordinates(latitude: lat, longitude: lon), population: Int(c[7]) ?? 0, timeZoneID: c[8]
            ))
            keys.append(TextNormalizer.searchKey(name: c[1], arabicName: arabic))
        }
        return CityIndex(cities: cities, keys: keys)
    }

    /// Word-prefix matches first ("riy" → Riyadh), then matches anywhere in the name.
    public func search(_ query: String, limit: Int = 30) -> [City] {
        let needle = TextNormalizer.normalize(query)
        guard !needle.isEmpty else { return [] }
        var prefix: [City] = []
        var other: [City] = []
        for (i, key) in keys.enumerated() {
            guard let first = key.range(of: needle) else { continue }
            // Any occurrence at the start of a word counts ("رياض" is a word of "الرياض رياض").
            var atWordStart = false
            var range: Range<String.Index>? = first
            while let r = range {
                if r.lowerBound == key.startIndex || key[key.index(before: r.lowerBound)] == " " {
                    atWordStart = true
                    break
                }
                range = key.range(of: needle, range: key.index(after: r.lowerBound)..<key.endIndex)
            }
            if atWordStart {
                prefix.append(cities[i])
                if prefix.count == limit { break }
            } else if other.count < limit {
                other.append(cities[i])
            }
        }
        return Array((prefix + other).prefix(limit))
    }

    /// Closest city and its distance in km.
    public func nearest(to point: Coordinates) -> (city: City, km: Double)? {
        var best: City?
        var bestDistance = Double.greatestFiniteMagnitude
        for city in cities {
            let d = Self.distanceKm(point, city.coordinates)
            if d < bestDistance {
                bestDistance = d
                best = city
            }
        }
        return best.map { ($0, bestDistance) }
    }

    /// Equirectangular approximation: accurate to well under 1% at city distances.
    public static func distanceKm(_ a: Coordinates, _ b: Coordinates) -> Double {
        let meanLat = (a.latitude + b.latitude) / 2 * .pi / 180
        var dLon = b.longitude - a.longitude
        if dLon > 180 { dLon -= 360 }
        if dLon < -180 { dLon += 360 }
        let x = dLon * .pi / 180 * cos(meanLat)
        let y = (b.latitude - a.latitude) * .pi / 180
        return 6371.0 * sqrt(x * x + y * y)
    }
}
