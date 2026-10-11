import DeenKit
import SwiftUI
import UIKit

/// The reader: a surah in Amiri Quran with the Bismillah as a header. Remembers where you stopped.
/// ⋮ → player mode (recites the surah and lights up the word being recited), the surah's tafsir and
/// the reciter. Long-press a verse to listen from it, read its tafsir, bookmark, copy or share it.
struct QuranReaderView: View {
    let start: QuranPosition
    @Environment(AppModel.self) private var model
    @State private var surahNumber: Int
    @State private var visible: Int?
    @State private var copied = false
    @State private var playerMode = false
    @State private var tafsirFor: Int?
    @State private var showSurahTafsir = false
    @State private var player = QuranPlayer.shared

    init(start: QuranPosition) {
        self.start = start
        _surahNumber = State(initialValue: start.surah)
    }

    var body: some View {
        if let quran = model.quran, let surah = quran.surah(surahNumber) {
            content(quran, surah)
        } else {
            ProgressView().task { await model.loadQuran() }
        }
    }

    private func content(_ quran: Quran, _ surah: Surah) -> some View {
        let ayahs = quran.ayahs(of: surah.number)
        let juz = quran.juzStarts(in: surah.number)
        let playingHere = player.surah == surah.number
        return ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(spacing: 0) {
                    header(surah, bismillah: ayahs.first?.bismillah, lit: playingHere && player.ayah == 0)
                    ForEach(ayahs) { ayah in
                        let current = playingHere && player.ayah == ayah.number
                        VerseView(
                            surah: surah, ayah: ayah, juz: juz[ayah.number],
                            bookmarked: model.settings.bookmarks.contains(QuranPosition(surah: surah.number, ayah: ayah.number)),
                            current: current, word: current ? player.word : nil,
                            onBookmark: { toggleBookmark(surah: surah.number, ayah: ayah.number) },
                            onCopy: { copy(ayah, surah) },
                            onListen: {
                                playerMode = true
                                player.play(surah: surah, from: ayah.number, reciter: reciter)
                            },
                            onTafsir: { tafsirFor = ayah.number }
                        )
                        .id(ayah.number)
                        .accessibilityIdentifier("verse-\(ayah.number)")
                    }
                    surahNavigation(quran, surah, proxy: proxy)
                }
                .scrollTargetLayout()
            }
            .scrollPosition(id: $visible, anchor: .top)
            .onAppear {
                if start.surah == surahNumber, start.ayah > 1 { proxy.scrollTo(start.ayah, anchor: .top) }
            }
            .onChange(of: player.ayah) { _, ayah in
                // Follow the recitation.
                guard playerMode, playingHere, let ayah, ayah > 0 else { return }
                withAnimation(.smooth) { proxy.scrollTo(ayah, anchor: .center) }
            }
        }
        .navigationTitle(surah.title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Menu {
                    Toggle(isOn: $playerMode.animation(.smooth)) { Label(L("feature_quran_player_mode"), systemImage: "waveform") }
                    Button { showSurahTafsir = true } label: { Label(L("feature_quran_surah_tafsir"), systemImage: "text.book.closed") }
                    Picker(selection: model.binding(\.reciterID)) {
                        ForEach(Reciter.allCases) { Text($0.name(arabic: isArabicUI)).tag($0.rawValue) }
                    } label: {
                        Label(L("feature_quran_reciter"), systemImage: "person.wave.2")
                    }
                    .pickerStyle(.menu)
                } label: {
                    Image(systemName: "ellipsis")
                }
                .accessibilityLabel(L("feature_quran_more"))
                .accessibilityIdentifier("readerMenu")
            }
        }
        .safeAreaInset(edge: .bottom) {
            if playerMode {
                PlayerBar(surah: surah, onReciter: {}, onClose: {
                    if playingHere { player.stop() }
                    withAnimation(.smooth) { playerMode = false }
                })
                .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .task(id: visible) {
            // Remember the verse at the top, once scrolling has settled.
            guard let visible else { return }
            try? await Task.sleep(for: .milliseconds(600))
            model.update { $0.lastRead = QuranPosition(surah: surahNumber, ayah: visible) }
        }
        .onChange(of: model.settings.reciterID) { _, _ in
            // Switching voice mid-surah carries on from the same verse.
            if playingHere, let ayah = player.ayah { player.play(surah: surah, from: max(ayah, 1), reciter: reciter) }
        }
        .sheet(item: Binding(get: { tafsirFor.map { VerseRef(ayah: $0) } }, set: { tafsirFor = $0?.ayah })) { ref in
            TafsirSheet(surah: surah, ayah: ref.ayah) {
                tafsirFor = nil
                showSurahTafsir = true
            }
            .presentationDetents([.medium, .large])
        }
        .navigationDestination(isPresented: $showSurahTafsir) {
            SurahTafsirView(surah: surah, startAyah: tafsirFor ?? visible ?? 1)
        }
        .overlay(alignment: .bottom) {
            if copied {
                Text(L("core_ui_copied"))
                    .font(.subheadline.weight(.medium))
                    .padding(.horizontal, 18).padding(.vertical, 10)
                    .glassEffect(.regular, in: .capsule)
                    .padding(.bottom, playerMode ? 96 : 24)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
    }

    private var reciter: Reciter { Reciter(rawValue: model.settings.reciterID) ?? .alafasy }

    private func header(_ surah: Surah, bismillah: String?, lit: Bool) -> some View {
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
                    .foregroundStyle(lit ? AnyShapeStyle(Color.recitation) : AnyShapeStyle(.primary))
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

private struct VerseRef: Identifiable {
    let ayah: Int
    var id: Int { ayah }
}

extension Color {
    /// The green that marks the word being recited (the same in every theme, as on Android).
    static var recitation: Color {
        Color(UIColor { traits in
            traits.userInterfaceStyle == .dark ? UIColor(red: 0.20, green: 0.83, blue: 0.60, alpha: 1)
                : UIColor(red: 0.04, green: 0.60, blue: 0.40, alpha: 1)
        })
    }
}

private struct VerseView: View {
    let surah: Surah
    let ayah: Ayah
    let juz: Int?
    let bookmarked: Bool
    let current: Bool
    let word: Int?
    var onBookmark: () -> Void
    var onCopy: () -> Void
    var onListen: () -> Void
    var onTafsir: () -> Void

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
                // A no-break space keeps the verse sign with the verse's last word.
                Text(current ? highlighted(ayah.text, word: word, green: .recitation) + AttributedString("\u{00A0}\(endOfAyah(ayah.number))")
                             : AttributedString("\(ayah.text)\u{00A0}\(endOfAyah(ayah.number))"))
                    .font(.custom("AmiriQuran-Regular", size: 26))
                    .lineSpacing(14)
                    .multilineTextAlignment(.trailing)
                    .frame(maxWidth: .infinity, alignment: .trailing)
                    .environment(\.layoutDirection, .rightToLeft)
            }
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 14)
        .background(current ? Color.recitation.opacity(0.07) : .clear)
        .contentShape(Rectangle())
        .contextMenu {
            Button(action: onListen) { Label(L("feature_quran_play_from_here"), systemImage: "play.circle") }
            Button(action: onTafsir) { Label(L("feature_quran_tafsir"), systemImage: "text.book.closed") }
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

// MARK: - Tafsir

/// The tafsir sources as chips; the chosen one is remembered.
struct TafsirChips: View {
    @Binding var selection: String

    var body: some View {
        ScrollView(.horizontal) {
            GlassEffectContainer(spacing: 8) {
                HStack(spacing: 8) {
                    ForEach(TafsirSource.allCases) { source in
                        let chosen = selection == source.rawValue
                        Button { selection = source.rawValue } label: {
                            Text(source.name(arabic: isArabicUI)).font(.subheadline.weight(chosen ? .semibold : .regular))
                                .padding(.horizontal, 6)
                        }
                        .buttonStyle(.glass)
                        .tint(chosen ? .accentColor : nil)
                    }
                }
                .padding(.horizontal, 20)
            }
        }
        .scrollIndicators(.hidden)
    }
}

/// One verse's tafsir, from the verse's long-press menu.
struct TafsirSheet: View {
    let surah: Surah
    let ayah: Int
    var onWholeSurah: () -> Void
    @Environment(AppModel.self) private var model
    @State private var entries: [TafsirEntry]?
    @State private var meaning: [Int: String] = [:]
    @State private var unavailable = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    TafsirChips(selection: model.binding(\.tafsirID)).padding(.horizontal, -20)
                    if let text = model.quran?.ayahs(of: surah.number).first(where: { $0.number == ayah })?.text {
                        ArabicBlock(text: "\(text)\u{00A0}\(endOfAyah(ayah))", size: 24)
                    }
                    if !isArabicUI, let meaning = meaning[ayah] {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(L("feature_quran_meaning")).font(.subheadline.weight(.semibold)).foregroundStyle(.tint)
                            Text(meaning)
                            Text(L("feature_quran_meaning_credit")).font(.caption).foregroundStyle(.secondary)
                        }
                    }
                    TafsirBody(entry: entries?.first { $0.ayah == ayah }, loading: entries == nil && !unavailable, unavailable: unavailable) {
                        Task { await load() }
                    }
                    Button(action: onWholeSurah) { Label(L("feature_quran_tafsir_full"), systemImage: "text.book.closed") }
                        .buttonStyle(.glass)
                }
                .padding(20)
            }
            .navigationTitle(L("feature_quran_tafsir_of", surah.title, Formats.localDigits("\(ayah)")))
            .navigationBarTitleDisplayMode(.inline)
        }
        .task(id: model.settings.tafsirID) { await load() }
    }

    private func load() async {
        entries = nil
        unavailable = false
        meaning = await model.meaning(surah: surah.number)
        entries = await model.tafsir(surah: surah)
        unavailable = entries == nil
    }
}

struct TafsirBody: View {
    let entry: TafsirEntry?
    let loading: Bool
    let unavailable: Bool
    var onRetry: () -> Void

    var body: some View {
        if loading {
            ProgressView().frame(maxWidth: .infinity)
        } else if unavailable {
            VStack(alignment: .leading, spacing: 10) {
                Text(L("feature_quran_tafsir_offline")).foregroundStyle(.secondary)
                Button(L("feature_quran_retry"), action: onRetry).buttonStyle(.glass)
            }
        } else if let entry {
            if let covered = entry.coveredBy {
                Text(L("feature_quran_tafsir_together", Formats.localDigits("\(covered)"))).font(.footnote).foregroundStyle(.secondary)
            }
            ArabicBlock(text: entry.text, size: 20)
        }
    }
}

/// The tafsir of a whole surah, verse by verse.
struct SurahTafsirView: View {
    let surah: Surah
    let startAyah: Int
    @Environment(AppModel.self) private var model
    @State private var entries: [TafsirEntry]?
    @State private var meaning: [Int: String] = [:]
    @State private var unavailable = false

    var body: some View {
        ScrollViewReader { proxy in
            List {
                TafsirChips(selection: model.binding(\.tafsirID))
                    .listRowInsets(EdgeInsets())
                    .listRowBackground(Color.clear)
                if unavailable {
                    TafsirBody(entry: nil, loading: false, unavailable: true) { Task { await load() } }
                } else if let entries {
                    ForEach(model.quran?.ayahs(of: surah.number) ?? []) { ayah in
                        let entry = entries.first { $0.ayah == ayah.number }
                        VStack(alignment: .leading, spacing: 10) {
                            HStack(alignment: .top, spacing: 10) {
                                StarBadge(text: Formats.localDigits("\(ayah.number)"), size: 34)
                                ArabicBlock(text: ayah.text, size: 22)
                            }
                            if !isArabicUI, let m = meaning[ayah.number] { Text(m).font(.subheadline) }
                            if let entry, entry.coveredBy != nil {
                                Text(L("feature_quran_tafsir_together", Formats.localDigits("\(entry.coveredBy ?? 0)")))
                                    .font(.footnote).foregroundStyle(.secondary)
                            } else if let entry {
                                ArabicBlock(text: entry.text, size: 19)
                            }
                        }
                        .padding(.vertical, 6)
                        .id(ayah.number)
                    }
                    .onAppear { if startAyah > 1 { proxy.scrollTo(startAyah, anchor: .top) } }
                } else {
                    ProgressView().frame(maxWidth: .infinity)
                }
            }
        }
        .navigationTitle(L("feature_quran_tafsir_title", surah.title))
        .navigationBarTitleDisplayMode(.inline)
        .task(id: model.settings.tafsirID) { await load() }
    }

    private func load() async {
        entries = nil
        unavailable = false
        meaning = await model.meaning(surah: surah.number)
        entries = await model.tafsir(surah: surah)
        unavailable = entries == nil
    }
}
