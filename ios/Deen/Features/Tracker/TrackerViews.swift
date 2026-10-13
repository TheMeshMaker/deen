import DeenKit
import SwiftUI

// MARK: - Pieces shared with Today

/// A ring in five segments, one per prayer, filling as they are ticked off; a full day becomes a
/// filled disc with a tick.
struct DayRing: View {
    let count: Int
    /// The ring's colour (the theme's accent unless given), and the tick's on a full day.
    var tint: Color?
    var tickColor: Color = .white
    var size: CGFloat = 30
    var dot = false
    @Environment(\.deenTheme) private var theme

    var body: some View {
        let color = tint ?? theme.accent
        ZStack {
            if count >= Prayer.prayers.count {
                Circle().fill(color)
                Image(systemName: "checkmark")
                    .font(.system(size: size * 0.42, weight: .bold))
                    .foregroundStyle(tickColor)
                    .transition(.scale.combined(with: .opacity))
            } else {
                ForEach(0..<Prayer.prayers.count, id: \.self) { i in
                    let share = 1.0 / Double(Prayer.prayers.count)
                    Circle()
                        .trim(from: Double(i) * share + 0.02, to: Double(i + 1) * share - 0.02)
                        .stroke(i < count ? color : color.opacity(0.18), style: StrokeStyle(lineWidth: size * 0.12, lineCap: .round))
                        .rotationEffect(.degrees(-90))
                }
                if dot { Circle().fill(color).frame(width: 4, height: 4) }
            }
        }
        .frame(width: size, height: size)
        // A clock face: the same way round in every language.
        .environment(\.layoutDirection, .leftToRight)
        .animation(.snappy, value: count)
        .accessibilityHidden(true)
    }
}

/// The last seven days, a ring each; today is marked.
struct WeekStrip: View {
    let prayed: [LocalDay: Set<Prayer>]
    let today: LocalDay
    var tint: Color?
    var tickColor: Color = .white

    var body: some View {
        HStack {
            ForEach((0...6).reversed(), id: \.self) { back in
                let day = today.adding(days: -back)
                VStack(spacing: 6) {
                    Text(Formats.weekdayShort(day))
                        .font(.caption2.weight(back == 0 ? .bold : .regular))
                        .opacity(back == 0 ? 1 : 0.7)
                    DayRing(count: PrayerTracker.count(prayed, day), tint: tint, tickColor: tickColor, dot: back == 0)
                }
                .frame(maxWidth: .infinity)
            }
        }
    }
}

extension IslamicEvent {
    var title: String {
        switch self {
        case .newYear: return L("core_ui_event_new_year")
        case .ashura: return L("core_ui_event_ashura")
        case .ramadan: return L("core_ui_event_ramadan")
        case .lastTenNights: return L("core_ui_event_last_ten")
        case .eidAlFitr: return L("core_ui_event_eid_fitr")
        case .arafah: return L("core_ui_event_arafah")
        case .eidAlAdha: return L("core_ui_event_eid_adha")
        case .whiteDays: return L("core_ui_event_white_days")
        }
    }

    var detail: String {
        switch self {
        case .newYear: return L("core_ui_event_new_year_detail")
        case .ashura: return L("core_ui_event_ashura_detail")
        case .ramadan: return L("core_ui_event_ramadan_detail")
        case .lastTenNights: return L("core_ui_event_last_ten_detail")
        case .eidAlFitr: return L("core_ui_event_eid_fitr_detail")
        case .arafah: return L("core_ui_event_arafah_detail")
        case .eidAlAdha: return L("core_ui_event_eid_adha_detail")
        case .whiteDays: return L("core_ui_event_white_days_detail")
        }
    }
}

extension UpcomingEvent {
    /// "Now", "Today", "Tomorrow" or "In 12 days".
    func when(today: LocalDay) -> String {
        switch daysAway {
        case 0: return start < today ? L("core_ui_event_now") : L("core_ui_event_today")
        case 1: return L("core_ui_event_tomorrow")
        default: return LP("core_ui_event_in_days", daysAway)
        }
    }
}

// MARK: - Prayer tracker

/// Streaks, today's prayers, and a month of rings; tap a past day to tick off its prayers.
struct TrackerView: View {
    @Environment(AppModel.self) private var model
    @State private var monthOffset = 0
    @State private var editing: EditedDay?

    private struct EditedDay: Identifiable {
        let day: LocalDay
        var id: String { day.key }
    }

    var body: some View {
        let zone = model.settings.location?.timeZone ?? .current
        let today = LocalDay.today(in: zone)
        let prayed = model.settings.prayedDays
        ScrollView {
            VStack(spacing: 16) {
                stats(prayed: prayed, today: today)
                VStack(alignment: .leading, spacing: 4) {
                    Text(L("feature_today_tracker_today_title")).font(.headline).foregroundStyle(.tint).padding(.bottom, 4)
                    DayToggles(day: today, today: today)
                }
                .padding(18)
                .glassEffect(.regular, in: .rect(cornerRadius: 26))
                month(prayed: prayed, today: today)
                Text(L("feature_today_tracker_private"))
                    .font(.footnote).foregroundStyle(.secondary).multilineTextAlignment(.center)
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 24)
        }
        .navigationTitle(L("feature_today_tracker_title"))
        .sheet(item: $editing) { edited in
            NavigationStack {
                ScrollView { DayToggles(day: edited.day, today: today).padding(20) }
                    .navigationTitle(Formats.longDate(edited.day))
                    .navigationBarTitleDisplayMode(.inline)
            }
            .presentationDetents([.medium])
        }
    }

    private func stats(prayed: [LocalDay: Set<Prayer>], today: LocalDay) -> some View {
        let streak = PrayerTracker.streak(prayed, today: today)
        let best = PrayerTracker.bestStreak(prayed)
        let totals = PrayerTracker.monthTotals(prayed, year: today.year, month: today.month, today: today)
        let percent = totals.due == 0 ? 0 : totals.done * 100 / totals.due
        return GlassEffectContainer(spacing: 12) {
            HStack(spacing: 12) {
                stat(L("feature_today_stat_streak"), LP("feature_today_days_label", streak), nil)
                stat(L("feature_today_stat_best"), LP("feature_today_days_label", best), nil)
                stat(L("feature_today_stat_month"), Formats.localDigits("\(percent)%"),
                     L("feature_today_stat_of", Formats.localDigits("\(totals.done)"), Formats.localDigits("\(totals.due)")))
            }
            // Equal heights, whether or not a card has a note under its figure.
            .fixedSize(horizontal: false, vertical: true)
        }
    }

    private func stat(_ label: String, _ value: String, _ note: String?) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label).font(.caption.weight(.medium)).foregroundStyle(.secondary).lineLimit(1)
            Text(value).font(.title3.weight(.bold)).foregroundStyle(.tint).lineLimit(1).minimumScaleFactor(0.7)
            if let note { Text(note).font(.caption2).foregroundStyle(.secondary).lineLimit(1) }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .padding(14)
        .glassEffect(.regular, in: .rect(cornerRadius: 20))
    }

    private func month(prayed: [LocalDay: Set<Prayer>], today: LocalDay) -> some View {
        let first = LocalDay(year: today.year, month: today.month, day: 1).addingMonths(monthOffset)
        let calendar = Calendar.current
        let lead = (first.weekday - calendar.firstWeekday + 7) % 7
        let count = first.monthLength
        let cells: [LocalDay?] = Array(repeating: nil, count: lead) + (0..<count).map { first.adding(days: $0) }
        let symbols = calendar.veryShortStandaloneWeekdaySymbols
        let order = (0..<7).map { (calendar.firstWeekday - 1 + $0) % 7 }
        return VStack(spacing: 10) {
            HStack {
                Button { withAnimation(.snappy) { monthOffset -= 1 } } label: { Image(systemName: "chevron.backward") }
                    .accessibilityLabel(L("feature_today_month_previous"))
                Spacer()
                Text(Formats.monthTitle(first)).font(.headline).contentTransition(.numericText())
                Spacer()
                Button { withAnimation(.snappy) { monthOffset += 1 } } label: { Image(systemName: "chevron.forward") }
                    .disabled(monthOffset >= 0)
                    .accessibilityLabel(L("feature_today_month_next"))
            }
            .buttonStyle(.glass)
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 4), count: 7), spacing: 10) {
                ForEach(order, id: \.self) { i in
                    Text(symbols[i]).font(.caption2).foregroundStyle(.secondary)
                }
                ForEach(Array(cells.enumerated()), id: \.offset) { _, day in
                    if let day {
                        let future = day > today
                        Button { editing = EditedDay(day: day) } label: {
                            VStack(spacing: 3) {
                                if future {
                                    Circle().fill(.quaternary).frame(width: 30, height: 30)
                                } else {
                                    DayRing(count: PrayerTracker.count(prayed, day))
                                }
                                Text(Formats.localDigits("\(day.day)"))
                                    .font(.caption2.weight(day == today ? .bold : .regular))
                                    .foregroundStyle(day == today ? AnyShapeStyle(.tint) : AnyShapeStyle(.secondary))
                            }
                        }
                        .buttonStyle(.plain)
                        .disabled(future)
                        .accessibilityLabel(L("feature_today_day_count", Formats.localDigits("\(day.day)"),
                                              Formats.localDigits("\(PrayerTracker.count(prayed, day))"), Formats.localDigits("5")))
                    } else {
                        Color.clear.frame(height: 30)
                    }
                }
            }
        }
        .padding(16)
        .glassEffect(.regular, in: .rect(cornerRadius: 26))
    }
}

/// The five prayers of [day] with a switch each. Today's later prayers wait for their time.
private struct DayToggles: View {
    let day: LocalDay
    let today: LocalDay
    @Environment(AppModel.self) private var model

    var body: some View {
        let set = model.settings.prayedDays[day] ?? []
        let times: PrayerDay? = model.settings.location.flatMap { location in
            model.settings.prayer.flatMap { PrayerEngine.day(year: day.year, month: day.month, day: day.day, at: location.coordinates, settings: $0) }
        }
        VStack(spacing: 2) {
            ForEach(Prayer.prayers, id: \.self) { prayer in
                let started = day < today || (day == today && (times.map { $0[prayer] <= Date() } ?? true))
                Toggle(isOn: Binding(
                    get: { set.contains(prayer) },
                    set: { on in withAnimation(.snappy) { model.setPrayed(on, prayer: prayer, day: day) } }
                )) {
                    Text(prayer.name(friday: day.weekday == 6))
                }
                .disabled(!started && !set.contains(prayer))
                .padding(.vertical, 6)
            }
        }
        .sensoryFeedback(.success, trigger: set.count)
    }
}

// MARK: - Islamic calendar

/// The coming days of the Islamic year, soonest first, and the weekly and monthly sunnah fasts.
struct IslamicCalendarView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.deenTheme) private var theme

    var body: some View {
        let zone = model.settings.location?.timeZone ?? .current
        let today = LocalDay.today(in: zone)
        let events = IslamicEvents.upcoming(today: today, offsetDays: model.settings.prayer?.hijriOffsetDays ?? 0)
        ScrollView {
            VStack(spacing: 14) {
                if let first = events.first { hero(first, today: today) }
                ForEach(events.dropFirst(), id: \.event) { row($0, today: today) }
                HStack(alignment: .top, spacing: 12) {
                    Image(systemName: "drop.fill").foregroundStyle(.tint)
                    VStack(alignment: .leading, spacing: 4) {
                        Text(L("feature_timetable_sunnah_fasts")).font(.headline)
                        Text(L("feature_timetable_sunnah_fasts_body")).font(.subheadline).foregroundStyle(.secondary)
                    }
                    Spacer(minLength: 0)
                }
                .padding(18)
                .glassEffect(.regular, in: .rect(cornerRadius: 26))
                Text(L("feature_timetable_calendar_note"))
                    .font(.footnote).foregroundStyle(.secondary).multilineTextAlignment(.center)
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 24)
        }
        .navigationTitle(L("feature_timetable_islamic_calendar"))
    }

    /// The soonest day, large on the theme's gradient, with its countdown.
    private func hero(_ event: UpcomingEvent, today: LocalDay) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(L("feature_timetable_next_day")).font(.subheadline.weight(.medium)).opacity(0.8)
            Text(event.event.title).font(.title.weight(.bold))
            Text("\(Formats.longDate(event.start)) · \(event.hijri.text)").font(.subheadline).opacity(0.85)
            Text(event.daysAway > 1 ? LP("feature_timetable_days_to_go", event.daysAway) : event.when(today: today))
                .font(.system(size: 34, weight: .light, design: .rounded))
                .padding(.top, 8)
            Text(event.event.detail).font(.subheadline).opacity(0.9)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(22)
        .foregroundStyle(theme.gradient.content)
        .background {
            ZStack {
                theme.gradient.linear
                GeometricPattern(color: theme.gradient.pattern, cell: 40)
            }
            .clipShape(.rect(cornerRadius: 28))
        }
    }

    private func row(_ event: UpcomingEvent, today: LocalDay) -> some View {
        HStack(spacing: 14) {
            VStack(spacing: 0) {
                Text(Formats.localDigits("\(event.start.day)")).font(.title2.weight(.bold))
                Text(Formats.monthShort(event.start)).font(.caption2)
            }
            .foregroundStyle(.tint)
            .frame(width: 56, height: 56)
            .glassEffect(.regular.tint(theme.accent.opacity(0.18)), in: .rect(cornerRadius: 16))
            VStack(alignment: .leading, spacing: 2) {
                Text(event.event.title).font(.headline)
                Text(event.hijri.text).font(.caption).foregroundStyle(.secondary)
                Text(event.event.detail).font(.caption).foregroundStyle(.secondary)
            }
            Spacer(minLength: 0)
            Text(event.when(today: today))
                .font(.caption.weight(.semibold))
                .foregroundStyle(.tint)
                .padding(.horizontal, 10).padding(.vertical, 5)
                .glassEffect(.regular, in: .capsule)
        }
        .padding(14)
        .glassEffect(.regular, in: .rect(cornerRadius: 24))
    }
}

extension LocalDay {
    var monthLength: Int {
        var c = Calendar(identifier: .gregorian)
        c.timeZone = TimeZone(identifier: "UTC")!
        let date = c.date(from: DateComponents(year: year, month: month, day: 1, hour: 12)) ?? Date()
        return c.range(of: .day, in: .month, for: date)?.count ?? 30
    }

    func addingMonths(_ n: Int) -> LocalDay {
        let index = (month - 1) + n
        let y = year + Int((Double(index) / 12).rounded(.down))
        let m = ((index % 12) + 12) % 12 + 1
        return LocalDay(year: y, month: m, day: 1)
    }

}
