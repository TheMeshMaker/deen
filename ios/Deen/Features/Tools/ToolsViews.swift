import CoreLocation
import DeenKit
import SwiftUI

// MARK: - Qibla

/// The phone's compass heading (true north when location allows, otherwise magnetic).
@MainActor
@Observable
final class HeadingProvider: NSObject, CLLocationManagerDelegate {
    private(set) var heading: Double?
    private(set) var needsCalibration = false
    let available = CLLocationManager.headingAvailable()
    private let manager = CLLocationManager()

    func start() {
        guard available else { return }
        manager.delegate = self
        manager.headingFilter = 1
        manager.startUpdatingHeading()
    }

    func stop() { manager.stopUpdatingHeading() }

    nonisolated func locationManager(_ manager: CLLocationManager, didUpdateHeading newHeading: CLHeading) {
        let value = newHeading.trueHeading >= 0 ? newHeading.trueHeading : newHeading.magneticHeading
        let poor = newHeading.headingAccuracy < 0 || newHeading.headingAccuracy > 25
        Task { @MainActor in
            self.heading = value
            self.needsCalibration = poor
        }
    }

    nonisolated func locationManagerShouldDisplayHeadingCalibration(_ manager: CLLocationManager) -> Bool { true }
}

/// The Qibla compass: the dial turns with the phone, the Kaaba marker sits on the Qibla bearing.
struct QiblaView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.deenTheme) private var theme
    @State private var compass = HeadingProvider()

    var body: some View {
        ZStack {
            theme.gradient.linear.ignoresSafeArea()
            DriftingPattern(color: theme.gradient.pattern, cell: 48).ignoresSafeArea()
            if let location = model.settings.location {
                content(location)
            } else {
                Text(L("feature_qibla_no_location")).padding()
            }
        }
        .foregroundStyle(theme.gradient.content)
        .environment(\.colorScheme, theme.gradient.hasDarkContent ? .light : .dark)
        .navigationTitle(L("feature_qibla_title"))
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(.hidden, for: .navigationBar)
        .onAppear { compass.start() }
        .onDisappear { compass.stop() }
    }

    private func content(_ location: SavedLocation) -> some View {
        let bearing = QiblaFinder.bearing(from: location.coordinates)
        let heading = compass.heading ?? 0
        var diff = (bearing - heading).truncatingRemainder(dividingBy: 360)
        if diff > 180 { diff -= 360 }
        if diff < -180 { diff += 360 }
        let aligned = compass.heading != nil && abs(diff) < 3
        return VStack(spacing: 28) {
            Text(statusText(aligned: aligned, diff: diff))
                .font(.title2.weight(.semibold))
                .multilineTextAlignment(.center)
                .contentTransition(.opacity)
                .animation(.smooth, value: aligned)
            ZStack {
                Circle().fill(.clear).glassEffect(.regular, in: .circle)
                Circle().stroke(aligned ? Color.recitation : theme.gradient.content.opacity(0.35), lineWidth: aligned ? 4 : 1.5)
                // The dial: north and the Kaaba, turning against the phone's heading.
                ZStack {
                    ForEach(0..<72) { i in
                        Capsule()
                            .fill(theme.gradient.content.opacity(i % 18 == 0 ? 0.9 : 0.35))
                            .frame(width: i % 18 == 0 ? 3 : 1.5, height: i % 18 == 0 ? 14 : 7)
                            .offset(y: -128)
                            .rotationEffect(.degrees(Double(i) * 5))
                    }
                    Text("N").font(.headline).offset(y: -100)
                    Image(systemName: "cube.fill")
                        .font(.title)
                        .foregroundStyle(.deenGold)
                        .offset(y: -100)
                        .rotationEffect(.degrees(bearing))
                }
                .rotationEffect(.degrees(-heading))
                .animation(.interpolatingSpring(stiffness: 120, damping: 18), value: heading)
                Image(systemName: "location.north.fill").font(.system(size: 44)).foregroundStyle(aligned ? Color.recitation : theme.gradient.content)
            }
            .frame(width: 290, height: 290)
            .sensoryFeedback(.success, trigger: aligned) { _, now in now }
            VStack(spacing: 6) {
                Text(L("feature_qibla_bearing", Formats.localDigits(String(format: "%.0f", bearing))))
                Text(L("feature_qibla_distance", Formats.localDigits(String(format: "%.0f", QiblaFinder.distanceKm(from: location.coordinates)))))
                    .opacity(0.85)
                Text(L(compass.available ? (compass.needsCalibration ? "feature_qibla_calibrate" : "feature_qibla_tip") : "feature_qibla_no_compass"))
                    .font(.footnote).opacity(0.75).multilineTextAlignment(.center).padding(.top, 6)
            }
            .padding(.horizontal, 24)
        }
    }

    private func statusText(aligned: Bool, diff: Double) -> String {
        guard compass.heading != nil else { return L("feature_qibla_title") }
        if aligned { return L("feature_qibla_aligned") }
        let degrees = Formats.localDigits(String(format: "%.0f", abs(diff)))
        return L(diff > 0 ? "feature_qibla_turn_right" : "feature_qibla_turn_left", degrees)
    }
}

// MARK: - Timetable

/// A month of prayer times, paged by Gregorian or Hijri month; today is highlighted.
struct TimetableView: View {
    @Environment(AppModel.self) private var model
    @State private var year: Int
    @State private var month: Int
    @State private var kind: MonthCalendar.Kind?

    init() {
        let now = Calendar(identifier: .gregorian).dateComponents([.year, .month], from: Date())
        _year = State(initialValue: now.year ?? 2026)
        _month = State(initialValue: now.month ?? 1)
    }

    var body: some View {
        if let location = model.settings.location, let settings = model.settings.prayer {
            let selected = kind ?? model.settings.timetableKind
            let days = MonthCalendar.days(year: year, month: month, kind: selected, hijriOffset: settings.hijriOffsetDays)
            let today = PrayerEngine.localDate(of: Date(), in: location.timeZone)
            List {
                Section {
                    Picker("", selection: Binding(get: { selected }, set: { switchCalendar(to: $0, offset: settings.hijriOffsetDays) })) {
                        Text(L("feature_timetable_gregorian")).tag(MonthCalendar.Kind.gregorian)
                        Text(L("feature_timetable_hijri")).tag(MonthCalendar.Kind.hijri)
                    }
                    .pickerStyle(.segmented)
                    HStack {
                        Button { step(-1, selected) } label: { Image(systemName: "chevron.backward") }
                            .accessibilityLabel(L("feature_timetable_previous"))
                        Spacer()
                        Text(title(selected)).font(.headline)
                        Spacer()
                        Button { step(1, selected) } label: { Image(systemName: "chevron.forward") }
                            .accessibilityLabel(L("feature_timetable_next"))
                    }
                    .buttonStyle(.glass)
                }
                Section {
                    row(day: L("feature_timetable_day"), times: ["fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha"].map { L("feature_timetable_short_\($0)") },
                        header: true, today: false)
                    ForEach(days, id: \.self) { d in
                        if let times = PrayerEngine.day(year: d.year, month: d.month, day: d.day, at: location.coordinates, settings: settings) {
                            row(day: Formats.localDigits("\(selected == .hijri ? d.hijri.day : d.day)"),
                                times: Prayer.allCases.map { Self.shortTime(times[$0], zone: location.timeZone) },
                                header: false, today: d.year == today.year && d.month == today.month && d.day == today.day)
                        }
                    }
                }
            }
            .navigationTitle(L("feature_timetable_title"))
            .navigationBarTitleDisplayMode(.inline)
        }
    }

    private func row(day: String, times: [String], header: Bool, today: Bool) -> some View {
        HStack(spacing: 2) {
            Text(day).frame(width: 34, alignment: .leading)
            ForEach(Array(times.enumerated()), id: \.offset) { _, t in
                Text(t).frame(maxWidth: .infinity)
            }
        }
        .font(header ? .caption2.weight(.semibold) : .caption.monospacedDigit())
        .foregroundStyle(header ? AnyShapeStyle(.secondary) : today ? AnyShapeStyle(.tint) : AnyShapeStyle(.primary))
        .fontWeight(today ? .bold : nil)
        .lineLimit(1)
        .minimumScaleFactor(0.8)
        .listRowBackground(today ? Color.accentColor.opacity(0.12) : nil)
    }

    private func title(_ kind: MonthCalendar.Kind) -> String {
        if kind == .hijri {
            return L("feature_timetable_hijri_month", L("core_ui_hijri_months.\(month - 1)"), Formats.localDigits("\(year)"))
        }
        let f = DateFormatter()
        f.locale = .current
        f.calendar = Calendar(identifier: .gregorian)
        f.setLocalizedDateFormatFromTemplate("MMMMyyyy")
        return f.string(from: Calendar(identifier: .gregorian).date(from: DateComponents(year: year, month: month, day: 15)) ?? Date())
    }

    private func step(_ delta: Int, _ kind: MonthCalendar.Kind) {
        month += delta
        if month < 1 { month = 12; year -= 1 }
        if month > 12 { month = 1; year += 1 }
    }

    /// Switching calendar shows the month that contains today.
    private func switchCalendar(to newKind: MonthCalendar.Kind, offset: Int) {
        kind = newKind
        model.update { $0.timetableKind = newKind }
        let today = PrayerEngine.localDate(of: Date(), in: .current)
        if newKind == .hijri {
            let h = HijriCalendar.hijri(year: today.year, month: today.month, day: today.day, offsetDays: offset)
            year = h.year
            month = h.month
        } else {
            year = today.year
            month = today.month
        }
    }

    /// 5:12 (the timetable has no room for AM/PM; 24-hour where the phone uses it).
    static func shortTime(_ date: Date, zone: TimeZone) -> String {
        let f = DateFormatter()
        f.locale = .current
        f.calendar = Calendar(identifier: .gregorian)
        f.timeZone = zone
        let twelve = (DateFormatter.dateFormat(fromTemplate: "j", options: 0, locale: .current) ?? "").contains("a")
        f.dateFormat = twelve ? "h:mm" : "HH:mm"
        return f.string(from: date)
    }
}

// MARK: - Quick actions

/// Qibla, timetable and tasbih as glass buttons (on Today).
struct QuickActions: View {
    var body: some View {
        GlassEffectContainer(spacing: 14) {
            HStack(spacing: 14) {
                action(L("feature_today_qibla"), symbol: "location.north.circle.fill", id: "qiblaButton") { QiblaView() }
                action(L("feature_today_timetable"), symbol: "calendar", id: "timetableButton") { TimetableView() }
                action(L("feature_today_tasbih"), symbol: "circle.hexagongrid.fill", id: "tasbihQuick") { TasbihView() }
            }
        }
    }

    private func action<Destination: View>(_ title: String, symbol: String, id: String, @ViewBuilder destination: @escaping () -> Destination) -> some View {
        NavigationLink {
            destination()
        } label: {
            VStack(spacing: 6) {
                Image(systemName: symbol).font(.title2)
                Text(title).font(.caption.weight(.medium)).lineLimit(1)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
        }
        .buttonStyle(.glass)
        .accessibilityIdentifier(id)
    }
}
