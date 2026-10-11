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
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    NavigationLink {
                        LocationSearchView { city in model.update { $0.location = city.asLocation } }
                    } label: {
                        Label(model.settings.location?.displayName(arabic: isArabicUI) ?? "", systemImage: "location.fill")
                            .labelStyle(.titleAndIcon)
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

                PrayerList(today: today, next: next, zone: zone, isFriday: isFriday)
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 32)
        }
        .scrollIndicators(.hidden)
    }
}

/// Today's five times and sunrise on a Liquid Glass card; the next one is lit.
private struct PrayerList: View {
    let today: PrayerDay
    let next: NextPrayer
    let zone: TimeZone
    let isFriday: Bool
    @Namespace private var glass

    var body: some View {
        VStack(spacing: 2) {
            ForEach(Prayer.allCases, id: \.self) { prayer in
                let isNext = prayer == next.prayer && next.at == today[prayer]
                HStack {
                    Image(systemName: prayer.symbol)
                        .frame(width: 26)
                        .opacity(prayer.isPrayer ? 1 : 0.6)
                    Text(prayer.name(friday: isFriday))
                        .font(.body.weight(isNext ? .semibold : .regular))
                    Spacer()
                    Text(Formats.time(today[prayer], in: zone))
                        .font(.body.weight(isNext ? .semibold : .regular))
                        .monospacedDigit()
                }
                .opacity(prayer.isPrayer ? 1 : 0.7)
                .padding(.horizontal, 16)
                .padding(.vertical, 13)
                .background {
                    if isNext {
                        Capsule().fill(.white.opacity(0.16))
                    }
                }
                .accessibilityElement(children: .combine)
            }
        }
        .padding(6)
        .glassEffect(.regular, in: .rect(cornerRadius: 30))
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
