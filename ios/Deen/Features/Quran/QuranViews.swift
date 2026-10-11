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
                .accessibilityIdentifier("continueReading")
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

/// The end-of-verse sign with the verse number in Arabic digits: ۝١٢
func endOfAyah(_ number: Int) -> String { "۝" + arabicDigits(number) }

func arabicDigits(_ number: Int) -> String {
    String(String(number).unicodeScalars.map { Character(Unicode.Scalar(0x0660 + $0.value - 48)!) })
}
