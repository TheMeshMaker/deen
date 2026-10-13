import DeenKit
import SwiftUI

/// The home screen: the next prayer with a live countdown over the theme's gradient, and today's times
/// on Liquid Glass.
struct TodayView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.deenTheme) private var theme

    var body: some View {
        NavigationStack {
            ZStack {
                theme.gradient.linear.ignoresSafeArea()
                DriftingPattern(color: theme.gradient.pattern, cell: 48).ignoresSafeArea()
                TimelineView(.periodic(from: .now, by: 1)) { context in
                    if let location = model.settings.location, let settings = model.settings.prayer,
                       let result = PrayerEngine.today(now: context.date, location: location, settings: settings) {
                        TodayContent(now: context.date, location: location, settings: settings, today: result.today, next: result.next)
                    } else {
                        ProgressView().tint(theme.gradient.content)
                    }
                }
            }
            .foregroundStyle(theme.gradient.content)
            // The gradient is dark (except a light custom one), so glass on it uses its dark look.
            .environment(\.colorScheme, theme.gradient.hasDarkContent ? .light : .dark)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    NavigationLink {
                        LocationSearchView { city in model.update { $0.location = city.asLocation } }
                    } label: {
                        HStack(spacing: 6) {
                            Image(systemName: "location.fill")
                            Text(model.settings.location?.displayName(arabic: isArabicUI) ?? "").lineLimit(1)
                        }
                        .font(.subheadline.weight(.semibold))
                        .padding(.horizontal, 4)
                    }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    NavigationLink {
                        SettingsView()
                    } label: {
                        Image(systemName: "gearshape.fill")
                    }
                    .accessibilityLabel(L("feature_today_settings"))
                }
            }
            .toolbarBackground(.hidden, for: .navigationBar)
            // Toolbar buttons in the gradient's text colour, not the theme tint (green on green).
            .tint(theme.gradient.content)
            // Light status bar and toolbar text over the dark gradient.
            .toolbarColorScheme(theme.gradient.hasDarkContent ? .light : .dark, for: .navigationBar)
        }
    }
}

private struct TodayContent: View {
    let now: Date
    let location: SavedLocation
    let settings: PrayerSettings
    let today: PrayerDay
    let next: NextPrayer
    @Environment(\.deenTheme) private var theme

    private var zone: TimeZone { location.timeZone }

    private var isFriday: Bool {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = zone
        return calendar.component(.weekday, from: now) == 6
    }

    var body: some View {
        let name = next.prayer.name(friday: isFriday)
        let remaining = next.at.timeIntervalSince(now)
        ScrollView {
            VStack(spacing: 22) {
                Text("\(Formats.longDate(now, in: zone)) · \(HijriCalendar.hijri(year: today.year, month: today.month, day: today.day, offsetDays: settings.hijriOffsetDays).text)")
                    .font(.subheadline)
                    .opacity(0.85)
                    .multilineTextAlignment(.center)

                ZStack {
                    Circle()
                        .fill(RadialGradient(colors: [theme.gradient.content.opacity(0.18), .clear], center: .center, startRadius: 0, endRadius: 150))
                    ProgressRing(progress: next.progress(now: now), color: theme.gradient.content)
                        .padding(12)
                    VStack(spacing: 4) {
                        Text(L("feature_today_next_prayer")).font(.footnote.weight(.medium)).opacity(0.75)
                        Text(name)
                            .font(.title.weight(.semibold))
                            .contentTransition(.opacity)
                        Text(Formats.countdown(remaining))
                            .font(.system(size: 46, weight: .light, design: .rounded))
                            .monospacedDigit()
                            .contentTransition(.numericText(countsDown: true))
                            .animation(.snappy, value: Int(remaining))
                            .environment(\.layoutDirection, .leftToRight)
                            .accessibilityLabel(L("feature_today_countdown_description", name,
                                                  LP("feature_today_hours", Int(remaining) / 3600),
                                                  LP("feature_today_minutes", Int(remaining) % 3600 / 60)))
                        Text(L("feature_today_at", Formats.time(next.at, in: zone)))
                            .font(.headline)
                            .opacity(0.85)
                    }
                }
                .frame(width: 270, height: 270)
                .padding(.top, 4)

                PrayerList(now: now, location: location, settings: settings, today: today, next: next, isFriday: isFriday)
                QuickActions()
                TodayCards(location: location, settings: settings)
                if AdhanPlayer.shared.playing != nil {
                    Button {
                        AdhanPlayer.shared.stop()
                    } label: {
                        Label(L("core_alarm_stop"), systemImage: "stop.fill").padding(.horizontal, 12)
                    }
                    .buttonStyle(.glass)
                }
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 32)
        }
        .scrollIndicators(.hidden)
    }
}

/// Today's five times and sunrise on a Liquid Glass card; the next one is lit. Each prayer has a
/// tick-box for the tracker (once its time has come), and the card ends with the night's times.
private struct PrayerList: View {
    let now: Date
    let location: SavedLocation
    let settings: PrayerSettings
    let today: PrayerDay
    let next: NextPrayer
    let isFriday: Bool
    @Environment(AppModel.self) private var model

    private var zone: TimeZone { location.timeZone }

    var body: some View {
        let day = LocalDay(year: today.year, month: today.month, day: today.day)
        let prayed = model.settings.prayedDays[day] ?? []
        let ramadan = HijriCalendar.hijri(year: today.year, month: today.month, day: today.day, offsetDays: settings.hijriOffsetDays).isRamadan
        VStack(spacing: 2) {
            ForEach(Prayer.allCases, id: \.self) { prayer in
                let isNext = prayer == next.prayer && next.at == today[prayer]
                HStack {
                    if prayer.isPrayer {
                        let done = prayed.contains(prayer)
                        let due = today[prayer] <= now
                        Button {
                            withAnimation(.snappy) { model.setPrayed(!done, prayer: prayer, day: day) }
                        } label: {
                            Image(systemName: done ? "checkmark.circle.fill" : "circle")
                                .font(.title3)
                                .contentTransition(.symbolEffect(.replace))
                                .frame(width: 30, height: 30)
                        }
                        .buttonStyle(.plain)
                        .disabled(!due && !done)
                        .opacity(due || done ? 1 : 0.35)
                        .sensoryFeedback(.success, trigger: done)
                        .accessibilityLabel(L(done ? "feature_today_prayed_undo" : "feature_today_prayed_mark", prayer.name(friday: isFriday)))
                    } else {
                        Image(systemName: prayer.symbol)
                            .frame(width: 30)
                            .opacity(0.6)
                    }
                    VStack(alignment: .leading, spacing: 0) {
                        Text(prayer.name(friday: isFriday))
                            .font(.body.weight(isNext ? .semibold : .regular))
                        if ramadan, prayer == .fajr || prayer == .maghrib {
                            Text(L(prayer == .fajr ? "feature_today_suhoor_ends" : "feature_today_iftar"))
                                .font(.caption2.weight(.medium)).opacity(0.8)
                        }
                    }
                    Spacer()
                    Text(Formats.time(today[prayer], in: zone))
                        .font(.body.weight(isNext ? .semibold : .regular))
                        .monospacedDigit()
                    let mode = model.settings.alerts.mode(for: prayer)
                    Button {
                        withAnimation(.snappy) { model.update { $0.alerts.modes[prayer] = mode.next(for: prayer) } }
                    } label: {
                        Image(systemName: mode.symbol)
                            .font(.footnote.weight(.semibold))
                            .frame(width: 34, height: 34)
                            .contentTransition(.symbolEffect(.replace))
                    }
                    .buttonStyle(.plain)
                    .opacity(mode == .silent ? 0.55 : 1)
                    .accessibilityLabel(L("feature_today_alert_for", prayer.name(friday: isFriday), mode.title))
                }
                .opacity(prayer.isPrayer ? 1 : 0.7)
                .padding(.horizontal, 16)
                .padding(.vertical, 13)
                .background {
                    if isNext {
                        Capsule().fill(.white.opacity(0.16))
                    }
                }
                .accessibilityElement(children: .contain)
            }
            if let night = NightTimes.of(now: now, location: location, settings: settings) {
                Label(L("feature_today_night_line", Formats.time(night.midnight, in: zone), Formats.time(night.lastThird, in: zone)),
                      systemImage: "moon.zzz")
                    .font(.footnote)
                    .opacity(0.8)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 16)
                    .padding(.top, 6)
                    .padding(.bottom, 8)
            }
        }
        .padding(6)
        .glassEffect(.regular, in: .rect(cornerRadius: 30))
    }
}

/// Under the quick actions: the week's prayers, the next Islamic day, and the khatmah (or where
/// reading stopped). Each opens its own screen.
private struct TodayCards: View {
    let location: SavedLocation
    let settings: PrayerSettings
    @Environment(AppModel.self) private var model
    @Environment(\.deenTheme) private var theme

    var body: some View {
        let today = LocalDay.today(in: location.timeZone)
        let prayed = model.settings.prayedDays
        let streak = PrayerTracker.streak(prayed, today: today)
        VStack(spacing: 14) {
            NavigationLink { TrackerView() } label: {
                VStack(spacing: 14) {
                    header(symbol: "checkmark.circle.fill", title: L("feature_today_tracker_title"),
                           line: streak > 0 ? LP("feature_today_streak", streak) : L("feature_today_tracker_hint"),
                           trailing: L("feature_today_tracker_today", Formats.localDigits("\(PrayerTracker.count(prayed, today))"), Formats.localDigits("5")))
                    WeekStrip(prayed: prayed, today: today, tint: theme.gradient.content, tickColor: theme.gradient.start)
                }
                .card()
            }
            .accessibilityIdentifier("trackerCard")
            if let event = IslamicEvents.next(today: today, offsetDays: settings.hijriOffsetDays) {
                NavigationLink { IslamicCalendarView() } label: {
                    header(symbol: "calendar.badge.clock", title: L("feature_today_upcoming"), line: event.event.title,
                           detail: "\(Formats.longDate(event.start)) · \(event.hijri.text)", trailing: event.when(today: today))
                        .card()
                }
            }
            khatmah(today: today)
        }
        .buttonStyle(.plain)
        .task { await model.loadQuran() }
    }

    @ViewBuilder
    private func khatmah(today: LocalDay) -> some View {
        if let plan = model.settings.khatmah, let pages = model.quran?.pages, pages.count > 0 {
            let progress = KhatmahProgress.of(plan, lastRead: model.settings.lastRead, pages: pages, today: today)
            let target = model.settings.lastRead.flatMap { pages.page(of: $0) >= plan.startPage && !progress.complete ? $0 : nil }
                ?? pages.start(of: progress.todayFrom)
            NavigationLink { QuranReaderView(start: target) } label: {
                VStack(spacing: 12) {
                    header(symbol: "book.pages.fill",
                           title: L("feature_today_khatmah_day", Formats.localDigits("\(min(progress.day, plan.days))"), Formats.localDigits("\(plan.days)")),
                           line: progress.complete ? L("feature_today_khatmah_complete")
                               : progress.pagesLeftToday == 0 ? L("feature_today_khatmah_done_today")
                               : L("feature_today_khatmah_today", Formats.localDigits("\(progress.todayFrom)"), Formats.localDigits("\(progress.todayTo)")),
                           detail: progress.complete || progress.pagesLeftToday == 0 ? nil : LP("feature_today_khatmah_left", progress.pagesLeftToday),
                           trailing: Formats.localDigits("\(Int(progress.fraction * 100))%"))
                    ProgressView(value: progress.fraction).tint(theme.gradient.content)
                }
                .card()
            }
        } else if let last = model.settings.lastRead, let surah = model.quran?.surah(last.surah) {
            NavigationLink { QuranReaderView(start: last) } label: {
                header(symbol: "book.pages.fill", title: L("feature_today_continue_quran"),
                       line: L("feature_today_quran_position", surah.title, Formats.localDigits("\(last.ayah)")))
                    .card()
            }
        }
    }

    private func header(symbol: String, title: String, line: String, detail: String? = nil, trailing: String? = nil) -> some View {
        HStack(spacing: 12) {
            Image(systemName: symbol)
                .font(.title3)
                .frame(width: 42, height: 42)
                .glassEffect(.regular.tint(theme.gradient.content.opacity(0.12)), in: .rect(cornerRadius: 13))
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(.caption.weight(.semibold)).opacity(0.8)
                Text(line).font(.headline).multilineTextAlignment(.leading)
                if let detail { Text(detail).font(.caption).opacity(0.75) }
            }
            Spacer(minLength: 0)
            if let trailing {
                Text(trailing)
                    .font(.subheadline.weight(.semibold))
                    .padding(.horizontal, 10).padding(.vertical, 5)
                    .glassEffect(.regular, in: .capsule)
            }
        }
    }
}

private extension View {
    /// A pane of Liquid Glass around a Today card.
    func card() -> some View {
        frame(maxWidth: .infinity, alignment: .leading)
            .padding(16)
            .contentShape(.rect(cornerRadius: 26))
            .glassEffect(.regular.interactive(), in: .rect(cornerRadius: 26))
    }
}

extension Prayer {
    var symbol: String {
        switch self {
        case .fajr: return "sun.horizon"
        case .sunrise: return "sunrise"
        case .dhuhr: return "sun.max"
        case .asr: return "sun.min"
        case .maghrib: return "sunset"
        case .isha: return "moon.stars"
        }
    }
}
