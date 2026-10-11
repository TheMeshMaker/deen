import Foundation

/// Text in Arabic and English (either may be missing).
public struct LocalizedText: Codable, Hashable, Sendable {
    public let ar: String?
    public let en: String?

    public func text(arabic: Bool) -> String { (arabic ? ar : en) ?? en ?? ar ?? "" }
}

// MARK: - Adhkar

public struct QuranRange: Hashable, Sendable {
    public let surah: Int
    public let from: Int
    public let to: Int
}

public struct Dhikr: Identifiable, Hashable, Sendable {
    public let id: Int
    /// Said before a Quran passage, e.g. seeking refuge before Ayat al-Kursi.
    public let preface: String?
    /// The supplication's Arabic; nil when the dhikr is only Quran passages.
    public let text: String?
    public let quran: [QuranRange]
    public let count: Int
    /// When or how to say it.
    public let instruction: LocalizedText?
    public let translation: String
    /// The reward stated in the hadith.
    public let virtue: LocalizedText?
    /// When no reward is stated: how the Prophet ﷺ said or taught it.
    public let sunnah: LocalizedText?
    public let reference: LocalizedText

    public var isQuran: Bool { !quran.isEmpty }
}

public struct AdhkarCategory: Identifiable, Hashable, Sendable {
    public let id: String
    public let title: LocalizedText
    public let items: [Dhikr]
}

public enum AdhkarLibrary {
    /// core/content/src/main/assets/adhkar.json (built by tools/content/build_adhkar.py).
    public static func load(from url: URL) throws -> [AdhkarCategory] {
        try JSONDecoder().decode(File.self, from: Data(contentsOf: url)).categories.map { c in
            AdhkarCategory(id: c.id, title: c.title, items: c.items.map { i in
                Dhikr(id: i.id, preface: i.preface, text: i.text,
                      quran: (i.quran ?? []).compactMap { $0.count == 3 ? QuranRange(surah: $0[0], from: $0[1], to: $0[2]) : nil },
                      count: max(i.count ?? 1, 1), instruction: i.instruction, translation: i.translation,
                      virtue: i.virtue, sunnah: i.sunnah, reference: i.reference)
            })
        }
    }

    private struct File: Decodable {
        struct Category: Decodable {
            let id: String
            let title: LocalizedText
            let items: [Item]
        }

        struct Item: Decodable {
            let id: Int
            let preface: String?
            let text: String?
            let quran: [[Int]]?
            let count: Int?
            let instruction: LocalizedText?
            let translation: String
            let virtue: LocalizedText?
            let sunnah: LocalizedText?
            let reference: LocalizedText
        }

        let categories: [Category]
    }
}

// MARK: - Hadith

public struct NawawiHadith: Identifiable, Hashable, Sendable {
    public let number: Int
    public let title: LocalizedText
    public let arabic: String
    public let english: String
    public let source: LocalizedText

    public var id: Int { number }
}

public struct ProphetFact: Identifiable, Hashable, Sendable {
    public let id: Int
    public let text: LocalizedText
    public let source: LocalizedText
}

public enum GradeKind: String, Sendable {
    case authentic = "AUTHENTIC", good = "GOOD", weak = "WEAK", fabricated = "FABRICATED", other = "OTHER"
}

public struct HadithGrade: Hashable, Sendable {
    public let grader: LocalizedText
    public let label: LocalizedText
    public let kind: GradeKind
}

public struct HadithBook: Identifiable, Hashable, Sendable {
    public let book: Int
    public let title: LocalizedText
    public let count: Int
    public let first: String
    public let last: String

    public var id: Int { book }
}

public struct HadithCollection: Identifiable, Hashable, Sendable {
    public let id: String
    public let name: LocalizedText
    public let author: LocalizedText?
    public let allAuthentic: Bool
    public let count: Int
    public let books: [HadithBook]
}

public struct HadithEntry: Identifiable, Hashable, Sendable {
    public let collectionID: String
    public let book: Int
    public let number: String
    public let numberInBook: Int
    public let text: String
    public let grade: HadithGrade?

    public var id: String { "\(collectionID)/\(book)/\(number)" }
}

/// The full collections in Arabic (hadith/collections.json and hadith/<id>/<book>.tsv), as on Android.
public final class HadithLibrary: @unchecked Sendable {
    public let collections: [HadithCollection]
    public let nawawi: [NawawiHadith]
    public let facts: [ProphetFact]
    private let folder: URL
    private let graders: [LocalizedText]
    private let grades: [(label: LocalizedText, kind: GradeKind)]

    private init(folder: URL, collections: [HadithCollection], graders: [LocalizedText],
                 grades: [(LocalizedText, GradeKind)], nawawi: [NawawiHadith], facts: [ProphetFact]) {
        self.folder = folder
        self.collections = collections
        self.graders = graders
        self.grades = grades
        self.nawawi = nawawi
        self.facts = facts
    }

    /// [hadithFolder] holds collections.json, nawawi40.json and the per-book files; [facts] is prophet-facts.json.
    public static func load(hadithFolder: URL, facts factsURL: URL) throws -> HadithLibrary {
        let index = try JSONDecoder().decode(IndexFile.self, from: Data(contentsOf: hadithFolder.appendingPathComponent("collections.json")))
        let nawawi = try JSONDecoder().decode(NawawiFile.self, from: Data(contentsOf: hadithFolder.appendingPathComponent("nawawi40.json")))
        let facts = try JSONDecoder().decode(FactsFile.self, from: Data(contentsOf: factsURL))
        return HadithLibrary(
            folder: hadithFolder,
            collections: index.collections.map { c in
                HadithCollection(id: c.id, name: c.name, author: c.author, allAuthentic: c.allAuthentic ?? false, count: c.count,
                                 books: c.books.map { HadithBook(book: $0.book, title: $0.title, count: $0.count, first: $0.first, last: $0.last) })
            },
            graders: index.graders,
            grades: index.grades.map { (LocalizedText(ar: $0.ar, en: $0.en), GradeKind(rawValue: $0.class) ?? .other) },
            nawawi: nawawi.hadiths.map { NawawiHadith(number: $0.number, title: $0.title, arabic: $0.arabic, english: $0.english, source: $0.source) },
            facts: facts.facts.map { ProphetFact(id: $0.id, text: $0.text, source: $0.source) }
        )
    }

    /// One book: number, number in book, "grader:grade" code, text.
    public func hadiths(collection: String, book: Int) -> [HadithEntry] {
        let url = folder.appendingPathComponent(collection).appendingPathComponent("\(book).tsv")
        guard let text = try? String(contentsOf: url, encoding: .utf8) else { return [] }
        var out: [HadithEntry] = []
        text.enumerateLines { line, _ in
            let parts = line.split(separator: "\t", maxSplits: 3, omittingEmptySubsequences: false)
            guard parts.count == 4 else { return }
            out.append(HadithEntry(collectionID: collection, book: book, number: String(parts[0]), numberInBook: Int(parts[1]) ?? 0,
                                   text: String(parts[3]), grade: self.grade(String(parts[2]))))
        }
        return out
    }

    /// Hadith whose Arabic contains [query] (diacritics ignored), or whose number is [query].
    public func search(_ query: String, in collection: HadithCollection, limit: Int = 200) -> [HadithEntry] {
        let q = query.trimmingCharacters(in: .whitespaces)
        guard !q.isEmpty else { return [] }
        let needle = TextNormalizer.normalize(q)
        var found: [HadithEntry] = []
        for book in collection.books {
            for entry in hadiths(collection: collection.id, book: book.book)
            where entry.number == q || TextNormalizer.normalize(entry.text).contains(needle) {
                found.append(entry)
                if found.count == limit { return found }
            }
        }
        return found
    }

    private func grade(_ code: String) -> HadithGrade? {
        let parts = code.split(separator: ":")
        guard parts.count == 2, let g = Int(parts[0]), let l = Int(parts[1]), graders.indices.contains(g), grades.indices.contains(l) else { return nil }
        return HadithGrade(grader: graders[g], label: grades[l].label, kind: grades[l].kind)
    }

    private struct IndexFile: Decodable {
        struct Grade: Decodable {
            let en: String
            let ar: String
            let `class`: String
        }

        struct Book: Decodable {
            let book: Int
            let title: LocalizedText
            let count: Int
            let first: String
            let last: String
        }

        struct Collection: Decodable {
            let id: String
            let name: LocalizedText
            let author: LocalizedText?
            let allAuthentic: Bool?
            let count: Int
            let books: [Book]
        }

        let graders: [LocalizedText]
        let grades: [Grade]
        let collections: [Collection]
    }

    private struct NawawiFile: Decodable {
        struct Item: Decodable {
            let number: Int
            let title: LocalizedText
            let arabic: String
            let english: String
            let source: LocalizedText
        }

        let hadiths: [Item]
    }

    private struct FactsFile: Decodable {
        struct Item: Decodable {
            let id: Int
            let text: LocalizedText
            let source: LocalizedText
        }

        let facts: [Item]
    }
}

/// The "of the day" items: the same for everyone on a date, walking the whole list before repeating
/// (Android's DailyContent).
public enum DailyContent {
    public static let hadithSalt = 0
    public static let factSalt = 17

    public static func pick<T>(_ items: [T], year: Int, month: Int, day: Int, salt: Int = 0) -> T? {
        guard !items.isEmpty else { return nil }
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "UTC")!
        let date = calendar.date(from: DateComponents(year: year, month: month, day: day)) ?? Date()
        let epochDay = Int((date.timeIntervalSince1970 / 86_400).rounded(.down))
        let index = ((epochDay + salt) % items.count + items.count) % items.count
        return items[index]
    }
}
