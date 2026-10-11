import DeenKit
import SwiftUI

/// The tasbih counter, kept across launches; [todayTotal] starts again each day.
struct TasbihState: Codable, Equatable {
    var dhikrID = TasbihDhikr.subhanallah.rawValue
    var count = 0
    /// 0 = no target.
    var target = 33
    var day = ""
    var todayTotal = 0
    var allTime = 0
}

/// The tasbih's built-in phrases (as on Android).
enum TasbihDhikr: String, CaseIterable, Identifiable {
    case subhanallah, alhamdulillah, allahuakbar, tahlil, istighfar, subhanallahi_wabihamdihi, hawqala, salawat

    var id: String { rawValue }

    var arabic: String {
        switch self {
        case .subhanallah: return "سُبْحَانَ اللَّهِ"
        case .alhamdulillah: return "الْحَمْدُ لِلَّهِ"
        case .allahuakbar: return "اللَّهُ أَكْبَرُ"
        case .tahlil: return "لَا إِلَهَ إِلَّا اللَّهُ"
        case .istighfar: return "أَسْتَغْفِرُ اللَّهَ"
        case .subhanallahi_wabihamdihi: return "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ"
        case .hawqala: return "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ"
        case .salawat: return "اللَّهُمَّ صَلِّ وَسَلِّمْ عَلَى نَبِيِّنَا مُحَمَّدٍ"
        }
    }

    var english: String {
        switch self {
        case .subhanallah: return "Glory be to Allah"
        case .alhamdulillah: return "Praise be to Allah"
        case .allahuakbar: return "Allah is the Greatest"
        case .tahlil: return "There is no god but Allah"
        case .istighfar: return "I seek Allah's forgiveness"
        case .subhanallahi_wabihamdihi: return "Glory and praise be to Allah"
        case .hawqala: return "No power or strength except through Allah"
        case .salawat: return "Blessings and peace upon our Prophet Muhammad"
        }
    }
}

/// Today's date as a key (adhkar counts and the tasbih's daily total start again each day).
var todayKey: String {
    let c = Calendar(identifier: .gregorian).dateComponents([.year, .month, .day], from: Date())
    return String(format: "%04d-%02d-%02d", c.year ?? 0, c.month ?? 0, c.day ?? 0)
}

extension AdhkarCategory {
    var symbol: String {
        switch id {
        case "morning": return "sunrise.fill"
        case "evening": return "sunset.fill"
        case "after_prayer": return "hands.and.sparkles.fill"
        case "sleep": return "bed.double.fill"
        case "waking": return "sun.max.fill"
        case "adhan": return "speaker.wave.2.fill"
        case "mosque": return "building.columns.fill"
        case "wudu": return "drop.fill"
        case "home": return "house.fill"
        default: return "sparkles"
        }
    }
}

/// The Adhkar tab: the tasbih, what to say now, then every category with today's progress.
struct AdhkarHomeView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.deenTheme) private var theme

    private var suggested: String? {
        switch Calendar.current.component(.hour, from: Date()) {
        case 4...10: return "morning"
        case 15...19: return "evening"
        case 21...23, 0...1: return "sleep"
        default: return nil
        }
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    NavigationLink { TasbihView() } label: { tasbihCard }
                        .buttonStyle(.plain)
                        .accessibilityIdentifier("tasbihCard")
                    if let categories = model.adhkar {
                        if let id = suggested, let category = categories.first(where: { $0.id == id }) {
                            Text(L("feature_adhkar_now")).font(.headline).padding(.top, 6)
                            NavigationLink { AdhkarReaderView(category: category) } label: { CategoryCard(category: category, wide: true) }
                                .buttonStyle(.plain)
                                .accessibilityIdentifier("category-\(category.id)")
                        }
                        Text(L("feature_adhkar_all")).font(.headline).padding(.top, 6)
                        LazyVGrid(columns: [GridItem(.flexible(), spacing: 12), GridItem(.flexible(), spacing: 12)], spacing: 12) {
                            ForEach(categories.filter { $0.id != suggested }) { category in
                                NavigationLink { AdhkarReaderView(category: category) } label: { CategoryCard(category: category, wide: false) }
                                    .buttonStyle(.plain)
                                    .accessibilityIdentifier("category-\(category.id)")
                            }
                        }
                        Text(L("feature_adhkar_review_note")).font(.footnote).foregroundStyle(.secondary).padding(.top, 4)
                    } else {
                        ProgressView().frame(maxWidth: .infinity).padding(40)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 24)
            }
            .navigationTitle(L("feature_adhkar_title"))
            .task { await model.loadAdhkar() }
        }
    }

    private var tasbihCard: some View {
        let tasbih = model.settings.tasbih
        return HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(L("feature_tasbih_title")).font(.title2.weight(.semibold))
                Text(L("feature_tasbih_today", Formats.localDigits("\(tasbih.day == todayKey ? tasbih.todayTotal : 0)")))
                    .font(.subheadline).opacity(0.85)
            }
            Spacer()
            Text(L("feature_tasbih_open"))
                .font(.headline)
                .padding(.horizontal, 18).padding(.vertical, 10)
                .glassEffect(.regular.interactive(), in: .capsule)
        }
        .padding(20)
        .foregroundStyle(theme.gradient.content)
        .background {
            ZStack {
                theme.gradient.linear
                GeometricPattern(color: theme.gradient.pattern, cell: 40)
            }
            .clipShape(.rect(cornerRadius: 28))
        }
        .environment(\.colorScheme, theme.gradient.hasDarkContent ? .light : .dark)
    }
}

private struct CategoryCard: View {
    let category: AdhkarCategory
    let wide: Bool
    @Environment(AppModel.self) private var model

    var body: some View {
        let done = model.adhkarDone(in: category)
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Image(systemName: category.symbol)
                    .font(.title3)
                    .foregroundStyle(.tint)
                    .frame(width: 42, height: 42)
                    .background(.tint.opacity(0.12), in: .circle)
                if wide {
                    VStack(alignment: .leading) {
                        Text(category.title.text(arabic: isArabicUI)).font(.headline)
                        progressText(done)
                    }
                }
                Spacer()
            }
            if !wide {
                Text(category.title.text(arabic: isArabicUI)).font(.headline).lineLimit(1)
                progressText(done)
            }
            ProgressView(value: Double(done), total: Double(max(category.items.count, 1)))
        }
        .padding(16)
        .background(.background.secondary, in: .rect(cornerRadius: 24))
        .contentShape(.rect(cornerRadius: 24))
    }

    private func progressText(_ done: Int) -> some View {
        Text(L("feature_adhkar_progress", Formats.localDigits("\(done)"), Formats.localDigits("\(category.items.count)")))
            .font(.footnote).foregroundStyle(.secondary)
    }
}

/// One category's adhkar as cards: tap a card each time you say it; it moves on by itself when done.
struct AdhkarReaderView: View {
    let category: AdhkarCategory
    @Environment(AppModel.self) private var model
    @State private var confirmReset = false
    @State private var bumped = 0

    var body: some View {
        let done = model.adhkarDone(in: category)
        ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(spacing: 14) {
                    VStack(alignment: .leading, spacing: 6) {
                        Text(L("feature_adhkar_progress", Formats.localDigits("\(done)"), Formats.localDigits("\(category.items.count)")))
                            .font(.subheadline.weight(.semibold)).foregroundStyle(.tint)
                        ProgressView(value: Double(done), total: Double(max(category.items.count, 1)))
                        Text(L("feature_adhkar_tap_hint")).font(.footnote).foregroundStyle(.secondary)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    ForEach(category.items) { dhikr in
                        DhikrCard(dhikr: dhikr, arabic: arabic(of: dhikr), count: model.adhkarCount(category: category.id, item: dhikr.id)) {
                            let finished = model.countDhikr(category: category.id, item: dhikr.id, total: dhikr.count)
                            bumped += 1
                            if finished, let next = category.items.first(where: { model.adhkarCount(category: category.id, item: $0.id) < $0.count }) {
                                withAnimation(.smooth) { proxy.scrollTo(next.id, anchor: .top) }
                            }
                        }
                        .id(dhikr.id)
                        .accessibilityIdentifier("dhikr-\(dhikr.id)")
                    }
                    if done == category.items.count {
                        Text(L("feature_adhkar_all_done")).font(.headline).foregroundStyle(.tint).padding()
                    }
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 32)
            }
        }
        .sensoryFeedback(.increase, trigger: bumped)
        .navigationTitle(category.title.text(arabic: isArabicUI))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button { confirmReset = true } label: { Image(systemName: "arrow.counterclockwise") }
                    .accessibilityLabel(L("feature_adhkar_reset"))
            }
        }
        .confirmationDialog(L("feature_adhkar_reset_body"), isPresented: $confirmReset, titleVisibility: .visible) {
            Button(L("feature_adhkar_reset"), role: .destructive) { model.resetAdhkar(category: category.id) }
        }
        .task { await model.loadQuran() }
    }

    /// The dhikr's Arabic: its own text, or the Quran passages from the Tanzil text (never retyped).
    private func arabic(of dhikr: Dhikr) -> String {
        var parts: [String] = []
        if let preface = dhikr.preface { parts.append(preface) }
        if let text = dhikr.text { parts.append(text) }
        if let quran = model.quran {
            for range in dhikr.quran {
                let verses = quran.ayahs(of: range.surah).filter { (range.from...range.to).contains($0.number) }
                if range.from == 1, let bismillah = verses.first?.bismillah { parts.append(bismillah) }
                parts.append(verses.map { "\($0.text)\u{00A0}\(endOfAyah($0.number))" }.joined(separator: " "))
            }
        }
        return parts.joined(separator: "\n")
    }
}

private struct DhikrCard: View {
    let dhikr: Dhikr
    let arabic: String
    let count: Int
    var onCount: () -> Void
    @State private var showSource = false

    var body: some View {
        let done = count >= dhikr.count
        VStack(alignment: .leading, spacing: 12) {
            if let instruction = dhikr.instruction {
                Text(instruction.text(arabic: isArabicUI)).font(.subheadline.weight(.semibold)).foregroundStyle(.tint)
            }
            Text(arabic)
                .font(.custom(dhikr.isQuran ? "AmiriQuran-Regular" : "Amiri-Regular", size: 22))
                .lineSpacing(10)
                .multilineTextAlignment(.trailing)
                .frame(maxWidth: .infinity, alignment: .trailing)
                .environment(\.layoutDirection, .rightToLeft)
            if !isArabicUI {
                Text(dhikr.translation).font(.body).foregroundStyle(.secondary)
            }
            if let reward = dhikr.virtue ?? dhikr.sunnah {
                let stated = dhikr.virtue != nil
                HStack(alignment: .top, spacing: 10) {
                    Image(systemName: stated ? "rosette" : "book.closed.fill").foregroundStyle(stated ? AnyShapeStyle(.tint) : AnyShapeStyle(.secondary))
                    VStack(alignment: .leading, spacing: 2) {
                        Text(L(stated ? "feature_adhkar_reward" : "feature_adhkar_sunnah"))
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(stated ? AnyShapeStyle(.tint) : AnyShapeStyle(.secondary))
                        Text(reward.text(arabic: isArabicUI)).font(.subheadline)
                    }
                }
                .padding(12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background((stated ? Color.accentColor : Color.secondary).opacity(0.08), in: .rect(cornerRadius: 16))
            }
            HStack {
                Button(L(showSource ? "feature_adhkar_less" : "feature_adhkar_source")) { withAnimation { showSource.toggle() } }
                    .font(.subheadline)
                Spacer()
                ZStack {
                    Circle().fill(done ? AnyShapeStyle(.tint) : AnyShapeStyle(.tint.opacity(0.12)))
                    if done {
                        Image(systemName: "checkmark").font(.headline).foregroundStyle(.white)
                    } else {
                        Text(Formats.localDigits("\(dhikr.count - count)"))
                            .font(.headline.monospacedDigit())
                            .foregroundStyle(.tint)
                            .contentTransition(.numericText(countsDown: true))
                    }
                }
                .frame(width: 48, height: 48)
                .animation(.snappy, value: count)
            }
            if showSource {
                Text(dhikr.reference.text(arabic: isArabicUI)).font(.footnote).foregroundStyle(.secondary)
            }
        }
        .padding(18)
        .background(.background.secondary, in: .rect(cornerRadius: 26))
        .opacity(done ? 0.6 : 1)
        .contentShape(.rect(cornerRadius: 26))
        .onTapGesture { if !done { onCount() } }
        .accessibilityAddTraits(.isButton)
        .accessibilityHint(L("feature_adhkar_count_label", Formats.localDigits("\(count)"), Formats.localDigits("\(dhikr.count)")))
    }
}

/// The tasbih: one large glass button that counts, a phrase and an optional target.
struct TasbihView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.deenTheme) private var theme

    var body: some View {
        let tasbih = model.settings.tasbih
        let dhikr = TasbihDhikr(rawValue: tasbih.dhikrID) ?? .subhanallah
        ZStack {
            theme.gradient.linear.ignoresSafeArea()
            DriftingPattern(color: theme.gradient.pattern, cell: 48).ignoresSafeArea()
            VStack(spacing: 24) {
                Menu {
                    Picker("", selection: model.binding(\.tasbih.dhikrID)) {
                        ForEach(TasbihDhikr.allCases) { Text($0.arabic).tag($0.rawValue) }
                    }
                } label: {
                    VStack(spacing: 4) {
                        Text(dhikr.arabic).font(.custom("Amiri-Bold", size: 30))
                        if !isArabicUI { Text(dhikr.english).font(.subheadline).opacity(0.8) }
                    }
                    .padding(.horizontal, 22).padding(.vertical, 12)
                    .glassEffect(.regular.interactive(), in: .rect(cornerRadius: 24))
                }
                Spacer()
                Button { model.countTasbih() } label: {
                    ZStack {
                        if tasbih.target > 0 {
                            Circle()
                                .trim(from: 0, to: Double(tasbih.count % max(tasbih.target, 1)) / Double(tasbih.target))
                                .stroke(theme.gradient.content, style: StrokeStyle(lineWidth: 6, lineCap: .round))
                                .rotationEffect(.degrees(-90))
                                .padding(-10)
                        }
                        Text(Formats.localDigits("\(tasbih.count)"))
                            .font(.system(size: 76, weight: .light, design: .rounded))
                            .monospacedDigit()
                            .contentTransition(.numericText())
                    }
                    .frame(width: 250, height: 250)
                    .glassEffect(.regular.interactive(), in: .circle)
                }
                .buttonStyle(.plain)
                .accessibilityIdentifier("tasbihButton")
                .sensoryFeedback(.increase, trigger: tasbih.count)
                .sensoryFeedback(.success, trigger: tasbih.target > 0 && tasbih.count > 0 && tasbih.count % tasbih.target == 0)
                .accessibilityLabel(L("feature_tasbih_tap", Formats.localDigits("\(tasbih.count)")))
                Spacer()
                HStack(spacing: 28) {
                    stat(L("feature_tasbih_today_label"), tasbih.day == todayKey ? tasbih.todayTotal : 0)
                    stat(L("feature_tasbih_all_time"), tasbih.allTime)
                }
                Picker("", selection: model.binding(\.tasbih.target)) {
                    ForEach([33, 99, 100, 0], id: \.self) { Text($0 == 0 ? "∞" : Formats.localDigits("\($0)")).tag($0) }
                }
                .pickerStyle(.segmented)
                .padding(.horizontal, 40)
            }
            .padding(.vertical, 24)
            .animation(.snappy, value: tasbih.count)
        }
        .foregroundStyle(theme.gradient.content)
        .environment(\.colorScheme, theme.gradient.hasDarkContent ? .light : .dark)
        .navigationTitle(L("feature_tasbih_title"))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button { model.update { $0.tasbih.count = 0 } } label: { Image(systemName: "arrow.counterclockwise") }
                    .accessibilityLabel(L("feature_tasbih_reset"))
            }
        }
        .toolbarBackground(.hidden, for: .navigationBar)
    }

    private func stat(_ title: String, _ value: Int) -> some View {
        VStack(spacing: 2) {
            Text(Formats.localDigits("\(value)")).font(.title3.weight(.semibold).monospacedDigit())
            Text(title).font(.caption).opacity(0.8)
        }
    }
}
