import Foundation

/// Reciters streamed verse by verse from everyayah.com/data/<id>/, with bundled word timings
/// (quran-align, CC BY 4.0) so the word being recited can be lit. Same list as Android.
public enum Reciter: String, CaseIterable, Codable, Sendable, Identifiable {
    case alafasy = "Alafasy_128kbps"
    case sudais = "Abdurrahmaan_As-Sudais_192kbps"
    case shuraim = "Saood_ash-Shuraym_128kbps"
    case shatri = "Abu_Bakr_Ash-Shaatree_128kbps"
    case husary = "Husary_64kbps"
    case minshawi = "Minshawy_Murattal_128kbps"
    case abdulBasit = "Abdul_Basit_Murattal_64kbps"

    public var id: String { rawValue }

    public func name(arabic: Bool) -> String {
        switch self {
        case .alafasy: return arabic ? "مشاري راشد العفاسي" : "Mishary Alafasy"
        case .sudais: return arabic ? "عبد الرحمن السديس" : "Abdul Rahman Al-Sudais"
        case .shuraim: return arabic ? "سعود الشريم" : "Saud Al-Shuraim"
        case .shatri: return arabic ? "أبو بكر الشاطري" : "Abu Bakr Al-Shatri"
        case .husary: return arabic ? "محمود خليل الحصري" : "Mahmoud Khalil Al-Husary"
        case .minshawi: return arabic ? "محمد صديق المنشاوي" : "Mohamed Siddiq Al-Minshawi"
        case .abdulBasit: return arabic ? "عبد الباسط عبد الصمد" : "Abdul Basit Abdul Samad"
        }
    }

    /// The recording of one verse (verse 1:1 is also every surah's Bismillah).
    public func url(surah: Int, ayah: Int) -> URL {
        URL(string: String(format: "https://everyayah.com/data/%@/%03d%03d.mp3", rawValue, surah, ayah))!
    }
}

/// Tafsir texts: al-Jalalayn is bundled; the longer ones are fetched per surah once and kept.
public enum TafsirSource: String, CaseIterable, Codable, Sendable, Identifiable {
    case jalalayn = "jalalayn"
    case ibnKathir = "ar-tafsir-ibn-kathir"
    case baghawi = "ar-tafsir-al-baghawi"
    case qurtubi = "ar-tafseer-al-qurtubi"

    public var id: String { rawValue }
    public var bundled: Bool { self == .jalalayn }

    public func name(arabic: Bool) -> String {
        switch self {
        case .jalalayn: return arabic ? "تفسير الجلالين" : "Tafsir al-Jalalayn"
        case .ibnKathir: return arabic ? "تفسير ابن كثير" : "Tafsir Ibn Kathir"
        case .baghawi: return arabic ? "تفسير البغوي" : "Tafsir al-Baghawi"
        case .qurtubi: return arabic ? "تفسير القرطبي" : "Tafsir al-Qurtubi"
        }
    }
}

/// One verse's tafsir; [coveredBy] when it was explained together with another verse.
public struct TafsirEntry: Hashable, Sendable {
    public let ayah: Int
    public let text: String
    public let coveredBy: Int?
}

/// One word's timing: words [firstWord]..<[endWord] from [startMs] to [endMs].
public struct WordTiming: Hashable, Sendable {
    public let firstWord: Int
    public let endWord: Int
    public let startMs: Int
    public let endMs: Int
}

/// `surah<TAB>ayah<TAB>first,end,startMs,endMs;…` per line, keyed surah*1000+ayah.
public func parseTimings(_ text: String) -> [Int: [WordTiming]] {
    var table: [Int: [WordTiming]] = [:]
    text.enumerateLines { line, _ in
        let parts = line.split(separator: "\t", omittingEmptySubsequences: false)
        guard parts.count >= 3, let surah = Int(parts[0]), let ayah = Int(parts[1]) else { return }
        table[surah * 1000 + ayah] = parts[2].split(separator: ";").compactMap { segment in
            let n = segment.split(separator: ",").compactMap { Int($0) }
            return n.count == 4 ? WordTiming(firstWord: n[0], endWord: n[1], startMs: n[2], endMs: n[3]) : nil
        }
    }
    return table
}

extension Array where Element == WordTiming {
    /// The word being recited at [positionMs]; between two words the previous one stays lit.
    public func wordAt(_ positionMs: Int) -> Int? {
        var current: Int?
        for timing in self {
            if timing.startMs > positionMs { break }
            current = timing.firstWord
        }
        return current
    }
}

/// A bundled verse table (`surah<TAB>ayah<TAB>text`, newlines written as `\n`): Jalalayn, Pickthall.
public func parseVerseTable(_ text: String) -> [Int: [Int: String]] {
    var table: [Int: [Int: String]] = [:]
    text.enumerateLines { line, _ in
        let parts = line.split(separator: "\t", maxSplits: 2, omittingEmptySubsequences: false)
        guard parts.count == 3, let surah = Int(parts[0]), let ayah = Int(parts[1]) else { return }
        table[surah, default: [:]][ayah] = parts[2].replacingOccurrences(of: "\\n", with: "\n")
    }
    return table
}

/// Strips the HTML the remote tafsirs carry.
public func cleanHTML(_ text: String) -> String {
    text.replacingOccurrences(of: "<br>", with: "\n")
        .replacingOccurrences(of: "</p>", with: "\n\n")
        .replacingOccurrences(of: "<[^>]+>", with: "", options: .regularExpression)
        .replacingOccurrences(of: "&nbsp;", with: " ")
        .replacingOccurrences(of: "\n{3,}", with: "\n\n", options: .regularExpression)
        .trimmingCharacters(in: .whitespacesAndNewlines)
}

/// Verse-by-verse tafsir for a surah: a verse missing from [texts] points to the passage that explains it
/// (the one before, or for an opening verse such as 42:1 the first one after).
public func tafsirEntries(_ texts: [Int: String], ayahCount: Int) -> [TafsirEntry] {
    let explained = (1...max(ayahCount, 1)).filter { !(texts[$0] ?? "").trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
    return (1...max(ayahCount, 1)).map { ayah in
        if explained.contains(ayah) { return TafsirEntry(ayah: ayah, text: texts[ayah] ?? "", coveredBy: nil) }
        let covering = explained.last { $0 < ayah } ?? explained.first { $0 > ayah }
        return TafsirEntry(ayah: ayah, text: covering.flatMap { texts[$0] } ?? "", coveredBy: covering)
    }
}

/// Downloads a remote tafsir for one surah (spa5k/tafsir_api via jsDelivr, MIT) and keeps it in [cache].
public enum RemoteTafsir {
    public static func fetch(_ source: TafsirSource, surah: Int, cache: URL) async -> [Int: String]? {
        let file = cache.appendingPathComponent("tafsir/\(source.rawValue)/\(surah).json")
        var data = try? Data(contentsOf: file)
        if data == nil {
            guard let url = URL(string: "https://cdn.jsdelivr.net/gh/spa5k/tafsir_api@main/tafsir/\(source.rawValue)/\(surah).json") else { return nil }
            var request = URLRequest(url: url, timeoutInterval: 15)
            request.setValue("Deen", forHTTPHeaderField: "User-Agent")
            guard let (body, response) = try? await URLSession.shared.data(for: request),
                  (response as? HTTPURLResponse)?.statusCode == 200 else { return nil }
            try? FileManager.default.createDirectory(at: file.deletingLastPathComponent(), withIntermediateDirectories: true)
            try? body.write(to: file)
            data = body
        }
        struct Verse: Decodable {
            let ayah: Int
            let text: String?
        }
        guard let data, let verses = try? JSONDecoder().decode([Verse].self, from: data) else {
            try? FileManager.default.removeItem(at: file)
            return nil
        }
        return Dictionary(verses.map { ($0.ayah, cleanHTML($0.text ?? "")) }, uniquingKeysWith: { a, _ in a })
    }
}

/// The days of a month for the timetable, Gregorian or Hijri (Umm al-Qura, with the user's correction).
public enum MonthCalendar {
    public enum Kind: String, Codable, Sendable { case gregorian, hijri }

    public struct Day: Hashable, Sendable {
        public let year: Int
        public let month: Int
        public let day: Int
        public let hijri: HijriDate
    }

    /// [year]/[month] in [kind]'s calendar.
    public static func days(year: Int, month: Int, kind: Kind, hijriOffset: Int = 0) -> [Day] {
        let utc = TimeZone(identifier: "UTC")!
        var gregorian = Calendar(identifier: .gregorian)
        gregorian.timeZone = utc
        var source = Calendar(identifier: kind == .gregorian ? .gregorian : .islamicUmmAlQura)
        source.timeZone = utc
        guard let first = source.date(from: DateComponents(year: year, month: month, day: 1, hour: 12)),
              let range = source.range(of: .day, in: .month, for: first) else { return [] }
        // A Hijri day shown with the correction is that many days off its Gregorian date.
        let shift = kind == .hijri ? -Double(hijriOffset) * 86_400 : 0
        return range.map { offset -> Day in
            let date = first.addingTimeInterval(Double(offset - 1) * 86_400 + shift)
            let g = gregorian.dateComponents([.year, .month, .day], from: date)
            let y = g.year ?? 2000, m = g.month ?? 1, d = g.day ?? 1
            return Day(year: y, month: m, day: d, hijri: HijriCalendar.hijri(year: y, month: m, day: d, offsetDays: hijriOffset))
        }
    }
}
