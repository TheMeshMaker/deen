import DeenKit
import SwiftUI

/// Settings, sharing and the project's pages. (No donate button on iOS: the App Store only allows tips
/// through in-app purchase.)
struct MoreView: View {
    var openSettings = false
    @State private var path: [String] = []
    private let repository = URL(string: "https://github.com/TheMeshMaker/deen")!

    var body: some View {
        NavigationStack(path: $path) {
            List {
                Section {
                    NavigationLink(value: "settings") {
                        MenuLabel(L("feature_more_settings"), detail: L("feature_more_settings_body"), symbol: "gearshape.fill")
                    }
                }
                Section {
                    ShareLink(item: URL(string: "https://github.com/TheMeshMaker/deen/releases")!,
                              message: Text(L("feature_more_share_text", ""))) {
                        MenuLabel(L("feature_more_share"), detail: L("feature_more_share_body"), symbol: "square.and.arrow.up.fill")
                    }
                    Link(destination: repository) {
                        MenuLabel(L("feature_more_star"), detail: L("feature_more_star_body"), symbol: "star.fill")
                    }
                    Link(destination: repository.appendingPathComponent("issues")) {
                        MenuLabel(L("feature_more_report"), detail: L("feature_more_report_body"), symbol: "exclamationmark.bubble.fill")
                    }
                } header: {
                    Text(L("feature_more_support"))
                } footer: {
                    VStack(spacing: 10) {
                        Text(L("feature_more_dua")).foregroundStyle(.tint)
                        Text(L("feature_more_footer"))
                    }
                    .frame(maxWidth: .infinity)
                    .multilineTextAlignment(.center)
                    .padding(.top, 12)
                }
            }
            .navigationTitle(L("feature_more_title"))
            .navigationDestination(for: String.self) { _ in SettingsView() }
        }
        .onAppear {
            if openSettings && path.isEmpty { path = ["settings"] }
        }
    }
}

/// A list row: a tinted icon tile, a title and a line of detail.
struct MenuLabel: View {
    let title: String
    let detail: String?
    let symbol: String

    init(_ title: String, detail: String? = nil, symbol: String) {
        self.title = title
        self.detail = detail
        self.symbol = symbol
    }

    var body: some View {
        HStack(spacing: 14) {
            Image(systemName: symbol)
                .font(.body.weight(.semibold))
                .foregroundStyle(.white)
                .frame(width: 32, height: 32)
                .background(.tint, in: .rect(cornerRadius: 9))
            VStack(alignment: .leading, spacing: 2) {
                Text(title).foregroundStyle(.primary)
                if let detail { Text(detail).font(.footnote).foregroundStyle(.secondary) }
            }
        }
    }
}

struct SettingsView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.openURL) private var openURL

    var body: some View {
        Form {
            Section(L("feature_settings_appearance")) {
                ThemePicker()
                    .listRowInsets(EdgeInsets(top: 12, leading: 8, bottom: 12, trailing: 8))
                if model.settings.theme == .custom {
                    ColorPicker(L("feature_settings_custom_first"), selection: colorBinding(\.custom.start), supportsOpacity: false)
                    ColorPicker(L("feature_settings_custom_second"), selection: colorBinding(\.custom.end), supportsOpacity: false)
                }
                Picker(L("feature_settings_mode"), selection: model.binding(\.appearance)) {
                    ForEach(AppearanceMode.allCases, id: \.self) { Text($0.title).tag($0) }
                }
                .pickerStyle(.segmented)
            }

            Section(L("feature_settings_language")) {
                // iOS keeps each app's language in the Settings app.
                Button {
                    if let url = URL(string: UIApplication.openSettingsURLString) { openURL(url) }
                } label: {
                    MenuLabel(L("feature_settings_language"),
                              detail: Locale.current.localizedString(forLanguageCode: Locale.current.language.languageCode?.identifier ?? "en")?
                                .localizedCapitalized,
                              symbol: "globe")
                }
            }

            AlertSettingsSection()

            Section(L("feature_settings_prayer_times")) {
                NavigationLink {
                    LocationSearchView { city in model.update { $0.location = city.asLocation } }
                } label: {
                    MenuLabel(L("feature_settings_location"), detail: model.settings.location?.displayName(arabic: isArabicUI), symbol: "location.fill")
                }
                NavigationLink {
                    List { MethodRows(selection: prayerBinding(\.method)) }
                        .navigationTitle(L("feature_settings_method"))
                } label: {
                    MenuLabel(L("feature_settings_method"), detail: model.settings.prayer?.method.title, symbol: "slider.horizontal.3")
                }
                Picker(L("feature_onboarding_method_asr_title"), selection: prayerBinding(\.asrMethod)) {
                    ForEach(AsrMethod.allCases, id: \.self) { Text($0.title).tag($0) }
                }
            }

            Section(L("feature_settings_about")) {
                LabeledContent(L("feature_settings_version"),
                               value: Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "")
                Link(destination: URL(string: "https://github.com/TheMeshMaker/deen")!) {
                    MenuLabel(L("feature_settings_source_code"), detail: L("feature_settings_source_code_body"), symbol: "chevron.left.forwardslash.chevron.right")
                }
                MenuLabel(L("feature_settings_privacy"), detail: L("feature_settings_privacy_body"), symbol: "hand.raised.fill")
            }
        }
        .navigationTitle(L("feature_settings_title"))
    }

    private func colorBinding(_ keyPath: WritableKeyPath<AppSettings, UInt32>) -> Binding<Color> {
        Binding(get: { Color(argb: model.settings[keyPath: keyPath]) },
                set: { color in model.update { $0[keyPath: keyPath] = color.argb | 0xFF00_0000 } })
    }

    private func prayerBinding<Value>(_ keyPath: WritableKeyPath<PrayerSettings, Value>) -> Binding<Value> {
        Binding(get: { (model.settings.prayer ?? PrayerSettings())[keyPath: keyPath] },
                set: { value in model.update { $0.prayer = ($0.prayer ?? PrayerSettings()).with(keyPath, value) } })
    }
}

private extension PrayerSettings {
    func with<Value>(_ keyPath: WritableKeyPath<PrayerSettings, Value>, _ value: Value) -> PrayerSettings {
        var copy = self
        copy[keyPath: keyPath] = value
        return copy
    }
}

/// The seven colour themes as gradient swatches (the last is your own two colours).
private struct ThemePicker: View {
    @Environment(AppModel.self) private var model

    var body: some View {
        LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 4), count: 4), spacing: 12) {
            ForEach(ThemeStyle.allCases) { style in
                let selected = model.settings.theme == style
                let gradient = style.gradient(custom: model.settings.custom)
                Button {
                    withAnimation(.smooth) { model.update { $0.theme = style } }
                } label: {
                    VStack(spacing: 6) {
                        ZStack {
                            Circle().fill(gradient.linear)
                            GeometricPattern(color: gradient.pattern, cell: 22).clipShape(Circle())
                            if selected {
                                Image(systemName: "checkmark").font(.headline).foregroundStyle(gradient.content)
                            } else if style == .custom {
                                Image(systemName: "paintpalette.fill").foregroundStyle(gradient.content)
                            }
                        }
                        .frame(width: 52, height: 52)
                        .padding(3)
                        .overlay { if selected { Circle().stroke(.tint, lineWidth: 2) } }
                        Text(style.title)
                            .font(.caption.weight(selected ? .semibold : .regular))
                            .foregroundStyle(selected ? AnyShapeStyle(.tint) : AnyShapeStyle(.primary))
                            .lineLimit(1)
                            .minimumScaleFactor(0.8)
                    }
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(selected ? .isSelected : [])
            }
        }
    }
}
