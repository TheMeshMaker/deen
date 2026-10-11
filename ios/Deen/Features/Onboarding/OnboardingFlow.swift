import DeenKit
import SwiftUI

/// First run: the welcome, then where you are, then how prayer times are calculated.
struct OnboardingFlow: View {
    private enum Step: Int { case welcome, location, method }

    @Environment(AppModel.self) private var model
    @State private var step: Step = .welcome
    @State private var city: City?

    var body: some View {
        ZStack {
            switch step {
            case .welcome:
                WelcomeView { step = .location }
                    .transition(.asymmetric(insertion: .opacity, removal: .move(edge: .leading).combined(with: .opacity)))
            case .location:
                NavigationStack {
                    VStack(spacing: 0) {
                        StepHeader(number: 1, title: L("feature_onboarding_location_title"), detail: L("feature_onboarding_location_body"))
                        LocationPicker { picked in
                            city = picked
                            withAnimation(.smooth) { step = .method }
                        }
                    }
                    .toolbar {
                        ToolbarItem(placement: .topBarLeading) {
                            Button { withAnimation(.smooth) { step = .welcome } } label: { Image(systemName: "chevron.backward") }
                                .accessibilityLabel(L("feature_onboarding_back"))
                        }
                    }
                }
                .transition(.move(edge: .trailing).combined(with: .opacity))
            case .method:
                if let city {
                    MethodStep(city: city, onBack: { withAnimation(.smooth) { step = .location } }) { settings in
                        Task {
                            // Ask for notifications and alarms now, so the first adhan can ring.
                            await AlertScheduler.requestPermissions()
                            model.update {
                                $0.location = city.asLocation
                                $0.prayer = settings
                                $0.onboardingComplete = true
                            }
                        }
                    }
                    .transition(.move(edge: .trailing).combined(with: .opacity))
                }
            }
        }
        .animation(.smooth(duration: 0.4), value: step)
    }
}

/// The first impression: the mark rising over a drifting star pattern, the greeting, what Deen does.
struct WelcomeView: View {
    var onStart: () -> Void
    @Environment(\.deenTheme) private var theme
    @Environment(\.openURL) private var openURL
    @State private var appeared = false

    var body: some View {
        let content = theme.gradient.content
        ZStack {
            theme.gradient.linear.ignoresSafeArea()
            DriftingPattern(color: theme.gradient.pattern, cell: 44).ignoresSafeArea()
            ScrollView {
                VStack(spacing: 18) {
                    HStack {
                        Spacer()
                        // iOS sets an app's language in the Settings app (Settings → Deen → Language).
                        Button {
                            if let url = URL(string: UIApplication.openSettingsURLString) { openURL(url) }
                        } label: {
                            Label(Locale.current.localizedString(forLanguageCode: Locale.current.language.languageCode?.identifier ?? "en")?
                                .localizedCapitalized ?? L("core_ui_language"), systemImage: "globe")
                                .font(.subheadline.weight(.medium))
                        }
                        .buttonStyle(.glass)
                    }
                    .appear(appeared, delay: 0)

                    ZStack {
                        Circle()
                            .fill(RadialGradient(colors: [content.opacity(0.24), .clear], center: .center, startRadius: 0, endRadius: 110))
                            .frame(width: 220, height: 220)
                        DeenMark()
                            .fill(.deenGold)
                            .frame(width: 140, height: 140)
                            .shadow(color: .black.opacity(0.25), radius: 12, y: 6)
                            .scaleEffect(appeared ? 1 : 0.6)
                            .opacity(appeared ? 1 : 0)
                            .animation(.spring(duration: 0.9, bounce: 0.3).delay(0.1), value: appeared)
                    }
                    Text("دِين")
                        .font(.custom("Amiri-Bold", size: 52))
                        .appear(appeared, delay: 0.15)
                    VStack(spacing: 6) {
                        Text(L("feature_onboarding_greeting")).font(.headline).opacity(0.8)
                        Text(L("feature_onboarding_welcome_title")).font(.largeTitle.weight(.semibold))
                        Text(L("feature_onboarding_welcome_body")).font(.body).opacity(0.85)
                    }
                    .multilineTextAlignment(.center)
                    .appear(appeared, delay: 0.25)

                    VStack(alignment: .leading, spacing: 16) {
                        Highlight(symbol: "bell.badge.fill", text: L("feature_onboarding_highlight_times"))
                        Highlight(symbol: "book.closed.fill", text: L("feature_onboarding_highlight_quran"))
                        Highlight(symbol: "sparkles", text: L("feature_onboarding_highlight_adhkar"))
                    }
                    .padding(20)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .glassEffect(.regular, in: .rect(cornerRadius: 28))
                    .appear(appeared, delay: 0.35)

                    VStack(spacing: 12) {
                        Button(action: onStart) {
                            Label(L("feature_onboarding_start"), systemImage: "arrow.forward")
                                .labelStyle(TrailingIconLabelStyle())
                                .font(.headline)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 8)
                        }
                        .buttonStyle(.glassProminent)
                        .tint(content.opacity(0.95))
                        .foregroundStyle(theme.gradient.start)
                        Text(L("feature_onboarding_promise")).font(.footnote).opacity(0.75)
                    }
                    .appear(appeared, delay: 0.45)
                }
                .padding(.horizontal, 24)
                .padding(.vertical, 12)
            }
            .scrollIndicators(.hidden)
        }
        .foregroundStyle(content)
        .environment(\.colorScheme, theme.gradient.hasDarkContent ? .light : .dark)
        .onAppear { appeared = true }
    }
}

private struct Highlight: View {
    let symbol: String
    let text: String

    var body: some View {
        HStack(spacing: 14) {
            Image(systemName: symbol)
                .font(.body.weight(.semibold))
                .frame(width: 38, height: 38)
                .glassEffect(.regular, in: .circle)
            Text(text).font(.body)
        }
    }
}

private struct TrailingIconLabelStyle: LabelStyle {
    func makeBody(configuration: Configuration) -> some View {
        HStack(spacing: 8) {
            configuration.title
            configuration.icon
        }
    }
}

private extension View {
    /// Fades in while rising, one element after another (none with Reduce Motion).
    func appear(_ shown: Bool, delay: Double) -> some View {
        modifier(Appear(shown: shown, delay: delay))
    }
}

private struct Appear: ViewModifier {
    let shown: Bool
    let delay: Double
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func body(content: Content) -> some View {
        content
            .opacity(shown || reduceMotion ? 1 : 0)
            .offset(y: shown || reduceMotion ? 0 : 18)
            .animation(.spring(duration: 0.7, bounce: 0.2).delay(delay), value: shown)
    }
}

/// "Step 1 of 2", a progress bar, the title and a line of explanation.
struct StepHeader: View {
    let number: Int
    let title: String
    let detail: String

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(L("feature_onboarding_step", Formats.localDigits("\(number)"), Formats.localDigits("2")))
                .font(.footnote.weight(.semibold))
                .foregroundStyle(.tint)
            ProgressView(value: Double(number), total: 2)
            Text(title).font(.largeTitle.weight(.bold)).padding(.top, 8)
            Text(detail).foregroundStyle(.secondary)
        }
        .padding(.horizontal, 20)
        .padding(.bottom, 8)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

/// The calculation method, suggested from the country (the user confirms or changes it).
private struct MethodStep: View {
    let city: City
    var onBack: () -> Void
    var onDone: (PrayerSettings) -> Void
    @State private var method: CalculationMethod
    @State private var asr: AsrMethod

    init(city: City, onBack: @escaping () -> Void, onDone: @escaping (PrayerSettings) -> Void) {
        self.city = city
        self.onBack = onBack
        self.onDone = onDone
        _method = State(initialValue: MethodSuggestions.method(countryCode: city.countryCode))
        _asr = State(initialValue: MethodSuggestions.asrMethod(countryCode: city.countryCode))
    }

    var body: some View {
        NavigationStack {
            List {
                Section {
                    StepHeader(number: 2, title: L("feature_onboarding_method_title"), detail: L("feature_onboarding_method_body"))
                        .listRowInsets(EdgeInsets())
                        .listRowBackground(Color.clear)
                }
                Section {
                    MethodRows(selection: $method, suggested: MethodSuggestions.method(countryCode: city.countryCode))
                }
                Section {
                    Picker(L("feature_onboarding_method_asr_title"), selection: $asr) {
                        ForEach(AsrMethod.allCases, id: \.self) { Text($0.title).tag($0) }
                    }
                    .pickerStyle(.segmented)
                } header: {
                    Text(L("feature_onboarding_method_asr_title"))
                } footer: {
                    Text(L("feature_onboarding_method_asr_body"))
                }
            }
            .safeAreaInset(edge: .bottom) {
                Button {
                    onDone(PrayerSettings(method: method, asrMethod: asr))
                } label: {
                    Text(L("feature_onboarding_finish")).font(.headline).frame(maxWidth: .infinity).padding(.vertical, 8)
                }
                .buttonStyle(.glassProminent)
                .padding(.horizontal, 20)
                .padding(.bottom, 8)
            }
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(action: onBack) { Image(systemName: "chevron.backward") }
                        .accessibilityLabel(L("feature_onboarding_back"))
                }
            }
        }
    }
}

/// Every method with the region it is used in; a check on the chosen one.
struct MethodRows: View {
    @Binding var selection: CalculationMethod
    var suggested: CalculationMethod?

    var body: some View {
        ForEach(CalculationMethod.allCases, id: \.self) { method in
            Button {
                selection = method
            } label: {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        HStack(spacing: 6) {
                            Text(method.title).foregroundStyle(.primary)
                            if method == suggested {
                                Text(L("feature_onboarding_method_suggested"))
                                    .font(.caption.weight(.semibold))
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 2)
                                    .background(.tint.opacity(0.15), in: .capsule)
                                    .foregroundStyle(.tint)
                            }
                        }
                        Text(method.region).font(.footnote).foregroundStyle(.secondary)
                    }
                    Spacer()
                    if method == selection {
                        Image(systemName: "checkmark").foregroundStyle(.tint).fontWeight(.semibold)
                    }
                }
            }
            .accessibilityAddTraits(method == selection ? .isSelected : [])
        }
    }
}
