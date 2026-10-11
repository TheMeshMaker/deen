import DeenKit
import SwiftUI
import UIKit

/// The Quran tab: continue where you stopped, then every surah (or the juz, or your bookmarks).
struct QuranHomeView: View {
    private enum Section: Hashable { case surahs, juz, bookmarks }

    var openLastRead = false

    @Environment(AppModel.self) private var model
    @State private var query = ""
    @State private var section: Section = .surahs
    @State private var path: [QuranPosition] = []

    var body: some View {
        NavigationStack(path: $path) {
            Group {
                if let quran = model.quran {
                    list(quran)
                } else {
                    ProgressView().task { await model.loadQuran() }
                }
            }
            .navigationTitle(L("feature_quran_title"))
            .navigationDestination(for: QuranPosition.self) { position in
                QuranReaderView(start: position)
            }
        }
        .task {
            if openLastRead, path.isEmpty, let last = model.settings.lastRead { path = [last] }
        }
    }

    @ViewBuilder
    private func list(_ quran: Quran) -> some View {
        List {
            if query.isEmpty, let last = model.settings.lastRead, let surah = quran.surah(last.surah) {
                Button { path.append(last) } label: {
                    HStack(spacing: 14) {
                        Image(systemName: "book.pages.fill").font(.title2).frame(width: 44, height: 44).glassEffect(.regular.tint(.accentColor.opacity(0.3)), in: .circle)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(L("feature_quran_continue")).font(.headline)
                            Text(L("feature_quran_position", surah.title, Formats.localDigits("\(last.ayah)")))
                                .font(.subheadline).foregroundStyle(.secondary)
                        }
                        Spacer()
                        Image(systemName: "chevron.forward").foregroundStyle(.tertiary)
                    }
                }
                .foregroundStyle(.primary)
            }
            if query.isEmpty {
                Picker("", selection: $section) {
                    Text(L("feature_quran_tab_surahs")).tag(Section.surahs)
                    Text(L("feature_quran_tab_juz")).tag(Section.juz)
                    Text(L("feature_quran_tab_bookmarks")).tag(Section.bookmarks)
                }
                .pickerStyle(.segmented)
                .listRowBackground(Color.clear)
                .listRowInsets(EdgeInsets(top: 4, leading: 0, bottom: 4, trailing: 0))
            }
            switch query.isEmpty ? section : .surahs {
            case .surahs:
                ForEach(quran.search(query)) { surah in
                    Button { path.append(QuranPosition(surah: surah.number, ayah: 1)) } label: { SurahRow(surah: surah) }
                        .foregroundStyle(.primary)
                }
            case .juz:
                ForEach(quran.juz, id: \.juz) { start in
                    Button { path.append(QuranPosition(surah: start.surah, ayah: start.ayah)) } label: {
                        HStack(spacing: 14) {
                            StarBadge(text: Formats.localDigits("\(start.juz)"))
                            VStack(alignment: .leading, spacing: 2) {
                                Text(L("feature_quran_juz", Formats.localDigits("\(start.juz)")))
                                if let surah = quran.surah(start.surah) {
                                    Text(L("feature_quran_position", surah.title, Formats.localDigits("\(start.ayah)")))
                                        .font(.footnote).foregroundStyle(.secondary)
                                }
                            }
                        }
                    }
                    .foregroundStyle(.primary)
                }
            case .bookmarks:
                if model.settings.bookmarks.isEmpty {
                    Text(L("feature_quran_bookmarks_empty")).foregroundStyle(.secondary)
                }
                ForEach(model.settings.bookmarks, id: \.self) { mark in
                    if let surah = quran.surah(mark.surah) {
                        Button { path.append(mark) } label: {
                            Label(L("feature_quran_position", surah.title, Formats.localDigits("\(mark.ayah)")), systemImage: "bookmark.fill")
                        }
                        .foregroundStyle(.primary)
                    }
                }
                .onDelete { offsets in model.update { $0.bookmarks.remove(atOffsets: offsets) } }
            }
        }
        .searchable(text: $query, prompt: L("feature_quran_search"))
    }
}

private struct SurahRow: View {
    let surah: Surah

    var body: some View {
        HStack(spacing: 14) {
            StarBadge(text: Formats.localDigits("\(surah.number)"))
            VStack(alignment: .leading, spacing: 2) {
                Text(isArabicUI ? surah.arabicName : surah.transliteration).font(.body.weight(.medium))
                Text([isArabicUI ? nil : surah.translation, L(surah.isMedinan ? "feature_quran_medinan" : "feature_quran_meccan"),
                      LP("feature_quran_verses", surah.ayahCount)].compactMap { $0 }.joined(separator: " · "))
                    .font(.footnote).foregroundStyle(.secondary)
            }
            Spacer()
            if !isArabicUI {
                Text(surah.arabicName).font(.custom("Amiri-Regular", size: 22)).foregroundStyle(.tint)
            }
        }
    }
}

/// A number inside the eight-pointed star, as printed Mushafs mark surahs and verses.
struct StarBadge: View {
    let text: String
    var size: CGFloat = 40

    var body: some View {
        ZStack {
            GeometricPattern.octagram(radius: size * 0.46, rotation: 0)
                .offsetBy(dx: size / 2, dy: size / 2)
                .stroke(.tint, lineWidth: 1.5)
            Text(text).font(.system(size: text.count > 2 ? 11 : 13, weight: .semibold)).foregroundStyle(.tint)
        }
        .frame(width: size, height: size)
    }
}

extension Surah {
    var title: String { isArabicUI ? arabicName : transliteration }
}

/// The reader: the surah's verses in Amiri Quran, the Bismillah as a header. Remembers where you
/// stopped; long-press a verse to bookmark, copy or share it.
struct QuranReaderView: View {
    let start: QuranPosition
    @Environment(AppModel.self) private var model
    @State private var surahNumber: Int
    @State private var visible: Int?
    @State private var copied = false

    init(start: QuranPosition) {
        self.start = start
        _surahNumber = State(initialValue: start.surah)
    }

    var body: some View {
        if let quran = model.quran, let surah = quran.surah(surahNumber) {
            let ayahs = quran.ayahs(of: surah.number)
            let juz = quran.juzStarts(in: surah.number)
            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(spacing: 0) {
                        header(surah, bismillah: ayahs.first?.bismillah)
                        ForEach(ayahs) { ayah in
                            VerseView(surah: surah, ayah: ayah, juz: juz[ayah.number],
                                      bookmarked: model.settings.bookmarks.contains(QuranPosition(surah: surah.number, ayah: ayah.number)),
                                      onBookmark: { toggleBookmark(surah: surah.number, ayah: ayah.number) },
                                      onCopy: { copy(ayah, surah) })
                                .id(ayah.number)
                        }
                        surahNavigation(quran, surah, proxy: proxy)
                    }
                    .scrollTargetLayout()
                }
                .scrollPosition(id: $visible, anchor: .top)
                .onAppear {
                    if start.surah == surahNumber, start.ayah > 1 { proxy.scrollTo(start.ayah, anchor: .top) }
                }
            }
            .navigationTitle(surah.title)
            .navigationBarTitleDisplayMode(.inline)
            .task(id: visible) {
                // Remember the verse at the top, once scrolling has settled.
                guard let visible else { return }
                try? await Task.sleep(for: .milliseconds(600))
                model.update { $0.lastRead = QuranPosition(surah: surahNumber, ayah: visible) }
            }
            .overlay(alignment: .bottom) {
                if copied {
                    Text(L("core_ui_copied"))
                        .font(.subheadline.weight(.medium))
                        .padding(.horizontal, 18).padding(.vertical, 10)
                        .glassEffect(.regular, in: .capsule)
                        .padding(.bottom, 24)
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                }
            }
        } else {
            ProgressView().task { await model.loadQuran() }
        }
    }

    private func header(_ surah: Surah, bismillah: String?) -> some View {
        VStack(spacing: 10) {
            Text("سُورَةُ \(surah.arabicName)")
                .font(.custom("Amiri-Bold", size: 30))
                .padding(.horizontal, 28).padding(.vertical, 10)
                .glassEffect(.regular.tint(.accentColor.opacity(0.18)), in: .capsule)
            Text("\(L(surah.isMedinan ? "feature_quran_medinan" : "feature_quran_meccan")) · \(LP("feature_quran_verses", surah.ayahCount))")
                .font(.footnote).foregroundStyle(.secondary)
            if let bismillah {
                Text(bismillah)
                    .font(.custom("AmiriQuran-Regular", size: 26))
                    .padding(.top, 12)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 20)
    }

    private func surahNavigation(_ quran: Quran, _ surah: Surah, proxy: ScrollViewProxy) -> some View {
        HStack(spacing: 12) {
            if let previous = quran.surah(surah.number - 1) {
                Button { go(to: previous.number, proxy: proxy) } label: {
                    Label(L("feature_quran_previous"), systemImage: "chevron.backward").frame(maxWidth: .infinity)
                }
            }
            if let next = quran.surah(surah.number + 1) {
                Button { go(to: next.number, proxy: proxy) } label: {
                    Label(L("feature_quran_next"), systemImage: "chevron.forward").frame(maxWidth: .infinity)
                }
            }
        }
        .buttonStyle(.glass)
        .padding(20)
    }

    private func go(to number: Int, proxy: ScrollViewProxy) {
        surahNumber = number
        visible = nil
        withAnimation { proxy.scrollTo(1, anchor: .top) }
    }

    private func toggleBookmark(surah: Int, ayah: Int) {
        let mark = QuranPosition(surah: surah, ayah: ayah)
        model.update { settings in
            if let i = settings.bookmarks.firstIndex(of: mark) { settings.bookmarks.remove(at: i) } else { settings.bookmarks.insert(mark, at: 0) }
        }
    }

    private func copy(_ ayah: Ayah, _ surah: Surah) {
        UIPasteboard.general.string = "\(ayah.text) ﴿\(surah.arabicName) \(arabicDigits(ayah.number))﴾"
        withAnimation(.spring) { copied = true }
        Task {
            try? await Task.sleep(for: .seconds(1.6))
            withAnimation { copied = false }
        }
    }
}

private struct VerseView: View {
    let surah: Surah
    let ayah: Ayah
    let juz: Int?
    let bookmarked: Bool
    var onBookmark: () -> Void
    var onCopy: () -> Void

    var body: some View {
        VStack(alignment: .trailing, spacing: 8) {
            if let juz {
                Text(L("feature_quran_juz", Formats.localDigits("\(juz)")))
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.tint)
                    .frame(maxWidth: .infinity, alignment: .center)
            }
            HStack(alignment: .top) {
                if bookmarked {
                    Image(systemName: "bookmark.fill").foregroundStyle(.tint).font(.footnote)
                }
                Text("\(ayah.text) \(endOfAyah(ayah.number))")
                    .font(.custom("AmiriQuran-Regular", size: 26))
                    .lineSpacing(14)
                    .multilineTextAlignment(.trailing)
                    .frame(maxWidth: .infinity, alignment: .trailing)
                    .environment(\.layoutDirection, .rightToLeft)
            }
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 14)
        .contentShape(Rectangle())
        .contextMenu {
            Button(action: onBookmark) {
                Label(L(bookmarked ? "feature_quran_unbookmark" : "feature_quran_bookmark"), systemImage: bookmarked ? "bookmark.slash" : "bookmark")
            }
            Button(action: onCopy) { Label(L("core_ui_copy"), systemImage: "doc.on.doc") }
            ShareLink(item: "\(ayah.text) ﴿\(surah.arabicName) \(arabicDigits(ayah.number))﴾") {
                Label(L("core_ui_share"), systemImage: "square.and.arrow.up")
            }
        }
        .accessibilityLabel(L("feature_quran_verse", Formats.localDigits("\(ayah.number)")))
        .accessibilityValue(ayah.text)
        Divider().padding(.horizontal, 20).opacity(0.4)
    }
}

/// The end-of-verse sign with the verse number in Arabic digits: ۝١٢
func endOfAyah(_ number: Int) -> String { "۝" + arabicDigits(number) }

func arabicDigits(_ number: Int) -> String {
    String(String(number).unicodeScalars.map { Character(Unicode.Scalar(0x0660 + $0.value - 48)!) })
}
