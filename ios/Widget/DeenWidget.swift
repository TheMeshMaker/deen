import DeenKit
import SwiftUI
import WidgetKit

/// Home-screen and Lock Screen widget: the next prayer with a live countdown, and today's times.
@main
struct DeenWidgetBundle: WidgetBundle {
    var body: some Widget {
        DeenPrayerWidget()
    }
}

struct DeenPrayerWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "DeenPrayerTimes", provider: PrayerProvider()) { entry in
            PrayerWidgetView(entry: entry)
        }
        .configurationDisplayName(L("feature_widget_name"))
        .description(L("feature_widget_description"))
        .supportedFamilies([.systemSmall, .systemMedium, .accessoryRectangular, .accessoryInline])
    }
}

struct PrayerEntry: TimelineEntry {
    let date: Date
    let snapshot: WidgetSnapshot?
}

/// One entry now and one just after each coming prayer, so "next" always moves on by itself.
struct PrayerProvider: TimelineProvider {
    func placeholder(in context: Context) -> PrayerEntry { PrayerEntry(date: Date(), snapshot: .sample) }

    func getSnapshot(in context: Context, completion: @escaping (PrayerEntry) -> Void) {
        completion(PrayerEntry(date: Date(), snapshot: WidgetSnapshot.load() ?? .sample))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<PrayerEntry>) -> Void) {
        let now = Date()
        let snapshot = WidgetSnapshot.load()
        var dates = [now]
        if let s = snapshot {
            for offset in 0...1 {
                let d = PrayerEngine.localDate(of: now.addingTimeInterval(Double(offset) * 86_400), in: s.location.timeZone)
                if let day = PrayerEngine.day(year: d.year, month: d.month, day: d.day, at: s.location.coordinates, settings: s.prayer) {
                    dates += Prayer.prayers.map { day[$0].addingTimeInterval(1) }.filter { $0 > now }
                }
            }
        }
        completion(Timeline(entries: dates.sorted().map { PrayerEntry(date: $0, snapshot: snapshot) }, policy: .atEnd))
    }
}

struct PrayerWidgetView: View {
    let entry: PrayerEntry
    @Environment(\.widgetFamily) private var family

    var body: some View {
        if let s = entry.snapshot, let result = PrayerEngine.today(now: entry.date, location: s.location, settings: s.prayer) {
            let gradient = (ThemeStyle(rawValue: s.theme) ?? .emerald).gradient(custom: CustomColors(start: s.customStart, end: s.customEnd))
            let zone = s.location.timeZone
            let name = result.next.prayer.name()
            Group {
                switch family {
                case .accessoryInline:
                    Text("\(name) \(Formats.time(result.next.at, in: zone))")
                case .accessoryRectangular:
                    VStack(alignment: .leading) {
                        Text(name).font(.headline)
                        Text(result.next.at, style: .timer).monospacedDigit()
                        Text(L("feature_today_at", Formats.time(result.next.at, in: zone))).font(.caption)
                    }
                case .systemMedium:
                    HStack(spacing: 16) {
                        next(name: name, at: result.next.at, zone: zone)
                        VStack(alignment: .leading, spacing: 3) {
                            ForEach(Prayer.prayers, id: \.self) { prayer in
                                HStack {
                                    Text(prayer.name()).font(.caption.weight(prayer == result.next.prayer ? .bold : .regular))
                                    Spacer()
                                    Text(Formats.time(result.today[prayer], in: zone)).font(.caption.monospacedDigit())
                                }
                            }
                        }
                    }
                default:
                    next(name: name, at: result.next.at, zone: zone)
                }
            }
            .foregroundStyle(gradient.content)
            .containerBackground(for: .widget) {
                ZStack {
                    gradient.linear
                    GeometricPattern(color: gradient.pattern, cell: 30)
                }
            }
        } else {
            Text(L("feature_widget_setup"))
                .font(.caption)
                .containerBackground(.fill.tertiary, for: .widget)
        }
    }

    private func next(name: String, at: Date, zone: TimeZone) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(L("feature_today_next_prayer")).font(.caption2).opacity(0.8)
            Text(name).font(.title3.weight(.semibold))
            Text(at, style: .timer).font(.title2.weight(.light).monospacedDigit())
            Text(L("feature_today_at", Formats.time(at, in: zone))).font(.caption).opacity(0.85)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}
