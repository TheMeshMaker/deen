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
    @State private var planning = false

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
            .sheet(isPresented: $planning) { KhatmahSheet() }
        }
        .task {
            if openLastRead, path.isEmpty, let last = model.settings.lastRead { path = [last] }
        }
        .task(id: model.pendingReader) {
            guard let position = model.pendingReader else { return }
            path = [position]
            model.pendingReader = nil
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
                KhatmahRow(quran: quran, open: { path.append($0) }, plan: { planning = true })
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

/// The khatmah: an invitation to plan one, or today's portion, the progress and a way to end it.
private struct KhatmahRow: View {
    let quran: Quran
    let open: (QuranPosition) -> Void
    let plan: () -> Void
    @Environment(AppModel.self) private var model

    var body: some View {
        let zone = model.settings.location?.timeZone ?? .current
        if let khatmah = model.settings.khatmah, quran.pages.count > 0 {
            let progress = KhatmahProgress.of(khatmah, lastRead: model.settings.lastRead, pages: quran.pages, today: .today(in: zone))
            VStack(alignment: .leading, spacing: 10) {
                HStack(spacing: 14) {
                    Image(systemName: "rosette").font(.title2).frame(width: 44, height: 44).glassEffect(.regular.tint(.accentColor.opacity(0.3)), in: .circle)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(L("feature_quran_khatmah")).font(.caption.weight(.semibold)).foregroundStyle(.tint)
                        Text(progress.complete ? L("feature_quran_khatmah_complete")
                             : progress.pagesLeftToday == 0 ? L("feature_quran_khatmah_done_today")
                             : L("feature_quran_khatmah_today", Formats.localDigits("\(progress.todayFrom)"), Formats.localDigits("\(progress.todayTo)")))
                            .font(.headline)
                        Text(L("feature_quran_khatmah_status", Formats.localDigits("\(min(progress.day, khatmah.days))"), Formats.localDigits("\(khatmah.days)"),
                               Formats.localDigits("\(progress.pagesRead)"), Formats.localDigits("\(progress.totalPages)")))
                            .font(.caption).foregroundStyle(.secondary)
                    }
                    Spacer()
                    Text(Formats.localDigits("\(Int(progress.fraction * 100))%")).font(.headline).foregroundStyle(.tint)
                }
                ProgressView(value: progress.fraction)
                if progress.ahead < 0, !progress.complete {
                    Text(LP("feature_quran_khatmah_behind", -progress.ahead)).font(.caption).foregroundStyle(.red)
                }
                HStack {
                    Button(L(progress.complete ? "feature_quran_khatmah_new" : "feature_quran_khatmah_end"), role: progress.complete ? nil : .destructive) {
                        withAnimation(.snappy) { model.update { $0.khatmah = nil } }
                        if progress.complete { plan() }
                    }
                    .buttonStyle(.glass)
                    Spacer()
                    if !progress.complete {
                        Button(L("feature_quran_khatmah_read")) {
                            let last = model.settings.lastRead.flatMap { quran.pages.page(of: $0) >= khatmah.startPage ? $0 : nil }
                            open(last ?? quran.pages.start(of: progress.todayFrom))
                        }
                        .buttonStyle(.glassProminent)
                    }
                }
            }
            .padding(.vertical, 6)
        } else {
            Button(action: plan) {
                HStack(spacing: 14) {
                    Image(systemName: "rosette").font(.title2).frame(width: 44, height: 44).glassEffect(.regular.tint(.accentColor.opacity(0.3)), in: .circle)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(L("feature_quran_khatmah")).font(.caption.weight(.semibold)).foregroundStyle(.tint)
                        Text(L("feature_quran_khatmah_invite")).font(.headline)
                    }
                    Spacer()
                    Image(systemName: "plus.circle.fill").font(.title3).foregroundStyle(.tint)
                }
            }
            .foregroundStyle(.primary)
        }
    }
}

/// Choosing a khatmah: how many days, and from Al-Fatiha or from where the reader is.
private struct KhatmahSheet: View {
    @Environment(AppModel.self) private var model
    @Environment(\.dismiss) private var dismiss
    @State private var days = 30
    @State private var fromHere = false

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Text(L("feature_quran_khatmah_body")).font(.subheadline).foregroundStyle(.secondary)
                }
                Section(L("feature_quran_khatmah_days")) {
                    Picker(L("feature_quran_khatmah_days"), selection: $days) {
                        ForEach(KhatmahPlan.dayChoices, id: \.self) { Text(LP("feature_quran_khatmah_days_choice", $0)).tag($0) }
                    }
                    .pickerStyle(.wheel)
                    Text(LP("feature_quran_khatmah_per_day", (KhatmahPlan.pages + days - 1) / days)).foregroundStyle(.tint)
                }
                if model.settings.lastRead != nil {
                    Section {
                        Picker("", selection: $fromHere) {
                            Text(L("feature_quran_khatmah_from_start")).tag(false)
                            Text(L("feature_quran_khatmah_from_here")).tag(true)
                        }
                        .pickerStyle(.segmented)
                    }
                }
            }
            .navigationTitle(L("feature_quran_khatmah_title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(L("feature_quran_khatmah_start")) { start() }
                }
            }
        }
        .presentationDetents([.medium, .large])
    }

    private func start() {
        let zone = model.settings.location?.timeZone ?? .current
        let page = fromHere ? model.settings.lastRead.flatMap { model.quran?.pages.page(of: $0) } ?? 1 : 1
        model.update { s in
            if !fromHere { s.lastRead = QuranPosition(surah: 1, ayah: 1) }
            s.khatmah = KhatmahPlan(startedOn: .today(in: zone), days: days, startPage: page)
        }
        dismiss()
    }
}
