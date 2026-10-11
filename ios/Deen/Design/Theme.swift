import SwiftUI
import UIKit

/// The same colour themes as Android (core:designsystem Theme.kt), plus the user's own two colours.
enum ThemeStyle: String, Codable, CaseIterable, Identifiable {
    case emerald, kiswah, desert, andalus, rose, midnight, custom

    var id: String { rawValue }
    var title: String { L("core_ui_theme_\(rawValue)") }
}

/// The custom theme's two gradient colours (ARGB).
struct CustomColors: Codable, Equatable {
    var start: UInt32 = 0xFF1D2671
    var end: UInt32 = 0xFF2BB3A7
}

/// A theme's gradient and what is drawn on top of it.
struct DeenGradient: Equatable {
    let start: Color
    let end: Color
    let content: Color
    let pattern: Color

    var linear: LinearGradient {
        LinearGradient(colors: [start, end], startPoint: .topLeading, endPoint: .bottomTrailing)
    }

    /// A light custom gradient carries dark text (and wants dark status-bar text).
    var hasDarkContent: Bool { content == Color(argb: 0xFF15171A) }
}

/// The resolved theme for the current light/dark mode, available to every view.
struct ThemeContext: Equatable {
    let gradient: DeenGradient
    /// Tint for buttons, toggles and highlights.
    let accent: Color

    init(style: ThemeStyle, custom: CustomColors, dark: Bool) {
        gradient = style.gradient(custom: custom)
        accent = style.accent(custom: custom, dark: dark)
    }

    static let fallback = ThemeContext(style: .emerald, custom: CustomColors(), dark: false)
}

private struct ThemeKey: EnvironmentKey {
    static let defaultValue = ThemeContext.fallback
}

extension EnvironmentValues {
    var deenTheme: ThemeContext {
        get { self[ThemeKey.self] }
        set { self[ThemeKey.self] = newValue }
    }
}

extension ThemeStyle {
    func gradient(custom: CustomColors) -> DeenGradient {
        switch self {
        case .emerald: return DeenGradient(start: Color(argb: 0xFF064E3B), end: Color(argb: 0xFF0E8A62), content: .white, pattern: .white.opacity(0.10))
        case .kiswah: return DeenGradient(start: Color(argb: 0xFF0D0D0D), end: Color(argb: 0xFF4A3812), content: Color(argb: 0xFFF6E7B8),
                                          pattern: Color(argb: 0xFFD4AF37).opacity(0.22))
        case .desert: return DeenGradient(start: Color(argb: 0xFF7C2D12), end: Color(argb: 0xFFC2622D), content: .white, pattern: .white.opacity(0.12))
        case .andalus: return DeenGradient(start: Color(argb: 0xFF0A2A66), end: Color(argb: 0xFF2474C6), content: .white, pattern: .white.opacity(0.12))
        case .rose: return DeenGradient(start: Color(argb: 0xFF5B0E2D), end: Color(argb: 0xFFC2456F), content: .white, pattern: .white.opacity(0.12))
        case .midnight: return DeenGradient(start: Color(argb: 0xFF070B24), end: Color(argb: 0xFF2E2F7A), content: Color(argb: 0xFFF3E7C4),
                                            pattern: Color(argb: 0xFFE9D8A6).opacity(0.18))
        case .custom:
            let a = Color(argb: custom.start)
            let b = Color(argb: custom.end)
            let light = (UIColor(a).luminance + UIColor(b).luminance) / 2 > 0.55
            let content: Color = light ? Color(argb: 0xFF15171A) : .white
            return DeenGradient(start: a, end: b, content: content, pattern: content.opacity(light ? 0.10 : 0.12))
        }
    }

    func accent(custom: CustomColors, dark: Bool) -> Color {
        switch self {
        case .emerald: return Color(argb: dark ? 0xFF5FD3A5 : 0xFF0B7A55)
        case .kiswah: return Color(argb: dark ? 0xFFE3C36B : 0xFF8A6A1C)
        case .desert: return Color(argb: dark ? 0xFFF0A27A : 0xFFA3471F)
        case .andalus: return Color(argb: dark ? 0xFF8FB8F0 : 0xFF1D5FAF)
        case .rose: return Color(argb: dark ? 0xFFF3A0BD : 0xFFB23A64)
        case .midnight: return Color(argb: dark ? 0xFFAEB5FF : 0xFF3F4AB8)
        case .custom:
            // The more colourful of the two, made dark enough (light mode) or light enough (dark mode) to read.
            let a = UIColor(Color(argb: custom.start))
            let b = UIColor(Color(argb: custom.end))
            let seed = a.saturation >= b.saturation ? a : b
            return Color(seed.withBrightness(dark ? max(seed.brightnessValue, 0.85) : min(seed.brightnessValue, 0.55),
                                             saturation: dark ? min(seed.saturation, 0.55) : seed.saturation))
        }
    }
}

extension Color {
    init(argb: UInt32) {
        self.init(.sRGB,
                  red: Double((argb >> 16) & 0xFF) / 255,
                  green: Double((argb >> 8) & 0xFF) / 255,
                  blue: Double(argb & 0xFF) / 255,
                  opacity: Double((argb >> 24) & 0xFF) / 255)
    }

    var argb: UInt32 {
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        UIColor(self).getRed(&r, green: &g, blue: &b, alpha: &a)
        func byte(_ v: CGFloat) -> UInt32 { UInt32(max(0, min(255, (v * 255).rounded()))) }
        return byte(a) << 24 | byte(r) << 16 | byte(g) << 8 | byte(b)
    }
}

private extension UIColor {
    var luminance: CGFloat {
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        getRed(&r, green: &g, blue: &b, alpha: &a)
        func linear(_ c: CGFloat) -> CGFloat { c <= 0.03928 ? c / 12.92 : pow((c + 0.055) / 1.055, 2.4) }
        return 0.2126 * linear(r) + 0.7152 * linear(g) + 0.0722 * linear(b)
    }

    var saturation: CGFloat {
        var h: CGFloat = 0, s: CGFloat = 0, v: CGFloat = 0, a: CGFloat = 0
        getHue(&h, saturation: &s, brightness: &v, alpha: &a)
        return s
    }

    var brightnessValue: CGFloat {
        var h: CGFloat = 0, s: CGFloat = 0, v: CGFloat = 0, a: CGFloat = 0
        getHue(&h, saturation: &s, brightness: &v, alpha: &a)
        return v
    }

    func withBrightness(_ brightness: CGFloat, saturation: CGFloat) -> UIColor {
        var h: CGFloat = 0, s: CGFloat = 0, v: CGFloat = 0, a: CGFloat = 0
        getHue(&h, saturation: &s, brightness: &v, alpha: &a)
        return UIColor(hue: h, saturation: saturation, brightness: brightness, alpha: 1)
    }
}
