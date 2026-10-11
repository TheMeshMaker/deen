import DeenKit
import SwiftUI

@main
struct DeenApp: App {
    @State private var model = AppModel()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environment(model)
        }
        // Rolls the adhan alarms forward even if the app isn't opened for days.
        .backgroundTask(.appRefresh(AlertScheduler.refreshTaskID)) {
            if let settings = AppModel.savedSettings() { await AlertScheduler.reschedule(settings) }
        }
    }
}

/// First run → onboarding; afterwards the tabs. Applies the colour theme and light/dark choice.
struct RootView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        let theme = ThemeContext(style: model.settings.theme, custom: model.settings.custom, dark: colorScheme == .dark)
        Group {
            if model.settings.isSetUp {
                MainTabs(initial: AppTab(rawValue: model.demo ?? "") ?? .today)
            } else {
                OnboardingFlow()
            }
        }
        .environment(\.deenTheme, theme)
        .tint(theme.accent)
        .preferredColorScheme(model.settings.appearance.colorScheme)
        .animation(.smooth(duration: 0.45), value: model.settings.isSetUp)
        // Re-plan the adhan whenever what it depends on changes, and each time the app comes back.
        .task(id: AlertInputs(model.settings, active: scenePhase == .active)) {
            guard model.demo == nil, scenePhase == .active else { return }
            try? await Task.sleep(for: .milliseconds(400))
            await AlertScheduler.reschedule(model.settings)
        }
    }
}

/// The settings the alert plan depends on.
private struct AlertInputs: Equatable {
    let location: SavedLocation?
    let prayer: PrayerSettings?
    let alerts: AlertSettings
    let active: Bool

    init(_ settings: AppSettings, active: Bool) {
        location = settings.location
        prayer = settings.prayer
        alerts = settings.alerts
        self.active = active
    }
}

enum AppTab: String, Hashable {
    case today, quran, hadith, adhkar, more, settings, reader, qibla, timetable
}

/// The tab bar: Liquid Glass, shrinking away as the page scrolls.
struct MainTabs: View {
    @State private var tab: AppTab
    @Environment(AppModel.self) private var model

    init(initial: AppTab) {
        switch initial {
        case .settings: _tab = State(initialValue: .more)
        case .reader: _tab = State(initialValue: .quran)
        case .qibla, .timetable: _tab = State(initialValue: .more)
        default: _tab = State(initialValue: initial)
        }
        openSettings = initial == .settings
        openTool = initial == .qibla || initial == .timetable ? initial.rawValue : nil
        openReader = initial == .reader
    }

    private let openSettings: Bool
    private let openTool: String?
    private let openReader: Bool

    var body: some View {
        TabView(selection: $tab) {
            Tab(L("nav_today"), systemImage: "sun.horizon.fill", value: AppTab.today) {
                TodayView()
            }
            Tab(L("nav_quran"), systemImage: "book.closed.fill", value: AppTab.quran) {
                QuranHomeView(openLastRead: openReader)
            }
            Tab(L("nav_hadith"), systemImage: "text.book.closed.fill", value: AppTab.hadith) {
                HadithHomeView()
            }
            Tab(L("nav_adhkar"), systemImage: "hands.and.sparkles.fill", value: AppTab.adhkar) {
                AdhkarHomeView()
            }
            Tab(L("nav_more"), systemImage: "square.grid.2x2.fill", value: AppTab.more) {
                MoreView(openSettings: openSettings, openTool: openTool)
            }
        }
        // (Kept full size in screenshot/test mode, so the walkthrough can always reach every tab.)
        .tabBarMinimizeBehavior(model.demo == nil ? .onScrollDown : .never)
    }
}
