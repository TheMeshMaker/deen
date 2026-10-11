import Foundation

public struct Surah: Identifiable, Hashable, Sendable {
    public let number: Int
    public let ayahCount: Int
    public let arabicName: String
    public let transliteration: String
    public let translation: String
    public let isMedinan: Bool

    public var id: Int { number }
}

public struct Ayah: Identifiable, Hashable, Sendable {
    public let surah: Int
    public let number: Int
    /// The verse text, verbatim from Tanzil (without the Bismillah header, see [bismillah]).
    public let text: String
    /// Verse 1's opening Bismillah, split off so the reader can show it as a header.
    public let bismillah: String?

    public var id: Int { number }
}

public struct JuzStart: Hashable, Sendable {
    public let juz: Int
    public let surah: Int
    public let ayah: Int
}

public struct QuranPosition: Codable, Hashable, Sendable {
    public let surah: Int
    public let ayah: Int

    public init(surah: Int, ayah: Int) {
        self.surah = surah
        self.ayah = ayah
    }
}

/// The Tanzil Uthmani text and metadata, read from the same asset files as Android
/// (core/content/src/main/assets/quran). The text is never altered (Tanzil licence).
public final class Quran: @unchecked Sendable {
    public let surahs: [Surah]
    public let juz: [JuzStart]
    private let verses: [Int: [Ayah]]

    private init(surahs: [Surah], juz: [JuzStart], verses: [Int: [Ayah]]) {
        self.surahs = surahs
        self.juz = juz
        self.verses = verses
    }

    public func surah(_ number: Int) -> Surah? { surahs.indices.contains(number - 1) ? surahs[number - 1] : nil }

    public func ayahs(of surah: Int) -> [Ayah] { verses[surah] ?? [] }

    /// Juz that start in [surah], by verse number.
    public func juzStarts(in surah: Int) -> [Int: Int] {
        Dictionary(juz.filter { $0.surah == surah }.map { ($0.ayah, $0.juz) }, uniquingKeysWith: { a, _ in a })
    }

    /// Matches the number, the Arabic name (diacritics ignored), the transliteration or the meaning.
    public func search(_ query: String) -> [Surah] {
        let q = query.trimmingCharacters(in: .whitespaces)
        guard !q.isEmpty else { return surahs }
        let number = Int(q)
        let plain = TextNormalizer.normalize(q).filter { $0.isLetter || $0.isNumber }
        return surahs.filter { s in
            s.number == number ||
                TextNormalizer.normalize(s.arabicName).contains(TextNormalizer.normalize(q)) ||
                TextNormalizer.normalize(s.transliteration).filter { $0.isLetter || $0.isNumber }.contains(plain) ||
                s.translation.lowercased().contains(q.lowercased())
        }
    }

    public static func load(text textURL: URL, meta metaURL: URL) throws -> Quran {
        let meta = try JSONDecoder().decode(MetaFile.self, from: Data(contentsOf: metaURL))
        var verses: [Int: [Ayah]] = [:]
        try String(contentsOf: textURL, encoding: .utf8).enumerateLines { line, _ in
            guard !line.isEmpty, !line.hasPrefix("#") else { return }
            let parts = line.split(separator: "|", maxSplits: 2, omittingEmptySubsequences: false)
            guard parts.count == 3, let surah = Int(parts[0]), let number = Int(parts[1]) else { return }
            verses[surah, default: []].append(splitBismillah(surah: surah, number: number, text: String(parts[2])))
        }
        return Quran(
            surahs: meta.surahs.map {
                Surah(number: $0.number, ayahCount: $0.ayahs, arabicName: $0.name, transliteration: $0.transliteration,
                      translation: $0.translation, isMedinan: $0.revelation == "MEDINAN")
            },
            juz: meta.juz.map { JuzStart(juz: $0.juz, surah: $0.surah, ayah: $0.ayah) },
            verses: verses
        )
    }

    /// Tanzil prefixes the Bismillah to verse 1 of every surah except al-Fatihah (where it *is* verse 1)
    /// and at-Tawbah (which has none). It is the first four words, exactly as written.
    static func splitBismillah(surah: Int, number: Int, text: String) -> Ayah {
        guard number == 1, surah != 1, surah != 9 else { return Ayah(surah: surah, number: number, text: text, bismillah: nil) }
        var end = text.startIndex
        for _ in 0..<4 {
            guard let space = text[text.index(after: end)...].firstIndex(of: " ") else {
                return Ayah(surah: surah, number: number, text: text, bismillah: nil)
            }
            end = space
        }
        return Ayah(surah: surah, number: number, text: String(text[text.index(after: end)...]), bismillah: String(text[..<end]))
    }

    private struct MetaFile: Decodable {
        struct SurahEntry: Decodable {
            let number: Int
            let ayahs: Int
            let name: String
            let transliteration: String
            let translation: String
            let revelation: String
        }

        struct JuzEntry: Decodable {
            let juz: Int
            let surah: Int
            let ayah: Int
        }

        let surahs: [SurahEntry]
        let juz: [JuzEntry]
    }
}
