import SwiftUI

@main
struct DeenApp: App {
    @State private var model = AppModel()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environment(model)
        }
    }
}

/// First run → onboarding; afterwards the tabs. Applies the colour theme and light/dark choice.
struct RootView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.colorScheme) private var colorScheme

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
    }
}

enum AppTab: String, Hashable {
    case today, quran, more, settings, reader
}

/// The tab bar: Liquid Glass, shrinking away as the page scrolls.
struct MainTabs: View {
    @State private var tab: AppTab

    init(initial: AppTab) {
        switch initial {
        case .settings: _tab = State(initialValue: .more)
        case .reader: _tab = State(initialValue: .quran)
        default: _tab = State(initialValue: initial)
        }
        openSettings = initial == .settings
        openReader = initial == .reader
    }

    private let openSettings: Bool
    private let openReader: Bool

    var body: some View {
        TabView(selection: $tab) {
            Tab(L("nav_today"), systemImage: "sun.horizon.fill", value: AppTab.today) {
                TodayView()
            }
            Tab(L("nav_quran"), systemImage: "book.closed.fill", value: AppTab.quran) {
                QuranHomeView(openLastRead: openReader)
            }
            Tab(L("nav_more"), systemImage: "square.grid.2x2.fill", value: AppTab.more) {
                MoreView(openSettings: openSettings)
            }
        }
        .tabBarMinimizeBehavior(.onScrollDown)
    }
}
