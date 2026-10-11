import DeenKit
import SwiftUI

/// The Hadith tab: today's hadith and fact, an-Nawawi's Forty, the facts, and the full collections.
struct HadithHomeView: View {
    @Environment(AppModel.self) private var model

    var body: some View {
        NavigationStack {
            Group {
                if let library = model.hadith {
                    List {
                        let today = PrayerEngine.localDate(of: Date(), in: .current)
                        if let hadith = DailyContent.pick(library.nawawi, year: today.year, month: today.month, day: today.day, salt: DailyContent.hadithSalt) {
                            Section(L("core_ui_daily_hadith")) {
                                NavigationLink { NawawiDetailView(hadith: hadith) } label: { DailyHadithRow(hadith: hadith) }
                            }
                        }
                        if let fact = DailyContent.pick(library.facts, year: today.year, month: today.month, day: today.day, salt: DailyContent.factSalt) {
                            Section(L("core_ui_daily_fact")) {
                                VStack(alignment: .leading, spacing: 6) {
                                    Text(fact.text.text(arabic: isArabicUI))
                                    Text(fact.source.text(arabic: isArabicUI)).font(.footnote).foregroundStyle(.secondary)
                                }
                                .padding(.vertical, 4)
                            }
                        }
                        Section {
                            NavigationLink { NawawiListView(hadiths: library.nawawi) } label: {
                                MenuLabel(L("feature_hadith_nawawi"), detail: L("feature_hadith_nawawi_body"), symbol: "text.book.closed.fill")
                            }
                            NavigationLink { FactsView(facts: library.facts) } label: {
                                MenuLabel(L("feature_hadith_facts"), detail: L("feature_hadith_facts_body"), symbol: "sparkles")
                            }
                        }
                        Section {
                            ForEach(library.collections) { collection in
                                NavigationLink { CollectionView(collection: collection) } label: {
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(collection.name.text(arabic: isArabicUI)).font(.body.weight(.medium))
                                        Text(LP("feature_hadith_count", collection.count)).font(.footnote).foregroundStyle(.secondary)
                                    }
                                }
                            }
                        } header: {
                            Text(L("feature_hadith_collections"))
                        } footer: {
                            Text(L("feature_hadith_arabic_note"))
                        }
                    }
                } else {
                    ProgressView()
                }
            }
            .navigationTitle(L("feature_hadith_title"))
            .task { await model.loadHadith() }
        }
    }
}

private struct DailyHadithRow: View {
    let hadith: NawawiHadith

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(hadith.title.text(arabic: isArabicUI)).font(.headline)
            Text(isArabicUI ? hadith.arabic : hadith.english)
                .font(isArabicUI ? .custom("Amiri-Regular", size: 19) : .body)
                .lineLimit(4)
            Text(hadith.source.text(arabic: isArabicUI)).font(.footnote).foregroundStyle(.secondary)
        }
        .padding(.vertical, 4)
    }
}

struct NawawiListView: View {
    let hadiths: [NawawiHadith]

    var body: some View {
        List(hadiths) { hadith in
            NavigationLink { NawawiDetailView(hadith: hadith) } label: {
                HStack(spacing: 14) {
                    StarBadge(text: Formats.localDigits("\(hadith.number)"))
                    Text(hadith.title.text(arabic: isArabicUI))
                }
            }
        }
        .navigationTitle(L("feature_hadith_nawawi"))
    }
}

struct NawawiDetailView: View {
    let hadith: NawawiHadith

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                Text(hadith.title.text(arabic: isArabicUI)).font(.title2.weight(.semibold))
                ArabicBlock(text: hadith.arabic)
                if !isArabicUI {
                    Text(hadith.english).font(.body)
                }
                Text(hadith.source.text(arabic: isArabicUI)).font(.footnote).foregroundStyle(.secondary)
                ShareLink(item: "\(hadith.arabic)\n\n\(hadith.source.text(arabic: true))") {
                    Label(L("core_ui_share"), systemImage: "square.and.arrow.up")
                }
                .buttonStyle(.glass)
            }
            .padding(20)
        }
        .navigationTitle(L("feature_hadith_number", Formats.localDigits("\(hadith.number)")))
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct FactsView: View {
    let facts: [ProphetFact]

    var body: some View {
        List {
            Section {
                ForEach(facts) { fact in
                    VStack(alignment: .leading, spacing: 6) {
                        Text(fact.text.text(arabic: isArabicUI))
                        Text(fact.source.text(arabic: isArabicUI)).font(.footnote).foregroundStyle(.secondary)
                    }
                    .padding(.vertical, 4)
                }
            } header: {
                Text(L("feature_hadith_facts_intro")).textCase(nil)
            }
        }
        .navigationTitle(L("feature_hadith_facts"))
    }
}

/// A collection: its books, or (while searching) matching hadith across the whole collection.
struct CollectionView: View {
    let collection: HadithCollection
    @Environment(AppModel.self) private var model
    @State private var query = ""
    @State private var results: [HadithEntry] = []
    @State private var searching = false

    var body: some View {
        List {
            if query.isEmpty {
                Section {
                    ForEach(collection.books) { book in
                        NavigationLink { BookView(collection: collection, book: book) } label: {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(book.title.text(arabic: isArabicUI)).font(.body)
                                Text("\(LP("feature_hadith_count", book.count)) · \(Formats.localDigits(book.first))–\(Formats.localDigits(book.last))")
                                    .font(.footnote).foregroundStyle(.secondary)
                            }
                        }
                    }
                } footer: {
                    Text(L(collection.allAuthentic ? "feature_hadith_all_authentic" : "feature_hadith_graded"))
                }
            } else {
                if searching { ProgressView() }
                Section(LP("feature_hadith_results", results.count)) {
                    ForEach(results) { HadithRow(entry: $0) }
                }
            }
        }
        .navigationTitle(collection.name.text(arabic: isArabicUI))
        .searchable(text: $query, prompt: L("feature_hadith_search"))
        .task(id: query) {
            guard !query.isEmpty, let library = model.hadith else { results = []; return }
            try? await Task.sleep(for: .milliseconds(350))
            searching = true
            let q = query
            results = await Task.detached { library.search(q, in: collection) }.value
            searching = false
        }
    }
}

struct BookView: View {
    let collection: HadithCollection
    let book: HadithBook
    @Environment(AppModel.self) private var model
    @State private var entries: [HadithEntry] = []

    var body: some View {
        List(entries) { HadithRow(entry: $0) }
            .navigationTitle(book.title.text(arabic: isArabicUI))
            .navigationBarTitleDisplayMode(.inline)
            .task {
                guard let library = model.hadith else { return }
                let id = collection.id, number = book.book
                entries = await Task.detached { library.hadiths(collection: id, book: number) }.value
            }
    }
}

private struct HadithRow: View {
    let entry: HadithEntry

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(L("feature_hadith_number", Formats.localDigits(entry.number))).font(.subheadline.weight(.semibold)).foregroundStyle(.tint)
                Spacer()
                if let grade = entry.grade {
                    Text(grade.label.text(arabic: isArabicUI))
                        .font(.caption.weight(.semibold))
                        .padding(.horizontal, 10).padding(.vertical, 3)
                        .foregroundStyle(grade.kind.color)
                        .background(grade.kind.color.opacity(0.12), in: .capsule)
                        .accessibilityLabel(L("feature_hadith_graded_by", "\(grade.label.text(arabic: isArabicUI)) (\(grade.grader.text(arabic: isArabicUI)))"))
                }
            }
            ArabicBlock(text: entry.text, size: 19)
        }
        .padding(.vertical, 6)
        .contextMenu {
            Button { UIPasteboard.general.string = entry.text } label: { Label(L("core_ui_copy"), systemImage: "doc.on.doc") }
            ShareLink(item: entry.text) { Label(L("core_ui_share"), systemImage: "square.and.arrow.up") }
        }
    }
}

extension GradeKind {
    /// Green authentic, blue good, amber weak, red fabricated (as on Android).
    var color: Color {
        switch self {
        case .authentic: return .green
        case .good: return .blue
        case .weak: return .orange
        case .fabricated: return .red
        case .other: return .secondary
        }
    }
}

/// Arabic religious text in Amiri, right to left.
struct ArabicBlock: View {
    let text: String
    var size: CGFloat = 22

    var body: some View {
        Text(text)
            .font(.custom("Amiri-Regular", size: size))
            .lineSpacing(size * 0.45)
            .multilineTextAlignment(.trailing)
            .frame(maxWidth: .infinity, alignment: .trailing)
            .environment(\.layoutDirection, .rightToLeft)
    }
}
