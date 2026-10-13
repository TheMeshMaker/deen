import DeenKit
import Foundation

/// A string by its Android key (Resources/Localizable.xcstrings is generated from the Android strings by
/// tools/i18n/build_ios_strings.py), with its placeholders filled in.
func L(_ key: String, _ args: CVarArg...) -> String {
    let format = Bundle.main.localizedString(forKey: key, value: nil, table: nil)
    guard !args.isEmpty else { return format }
    // Formatted without a locale (so a year never gets a thousands separator), then shown in the
    // language's own digits, as Android does.
    return Formats.localDigits(String(format: format, arguments: args))
}

/// A plural string: the language's own rules pick the form for [count].
func LP(_ key: String, _ count: Int) -> String {
    String.localizedStringWithFormat(Bundle.main.localizedString(forKey: key, value: nil, table: nil), count)
}

/// Arabic is the one language where place and surah names are shown in Arabic script.
var isArabicUI: Bool { Locale.current.language.languageCode?.identifier == "ar" }

extension Prayer {
    func name(friday: Bool = false) -> String {
        friday && self == .dhuhr ? L("core_ui_prayer_jumuah") : L("core_ui_prayer_\(rawValue)")
    }
}

extension CalculationMethod {
    private var key: String {
        switch self {
        case .ummAlQura: return "umm_al_qura"
        case .muslimWorldLeague: return "mwl"
        case .northAmerica: return "isna"
        case .egyptian: return "egyptian"
        case .karachi: return "karachi"
        case .dubai: return "dubai"
        case .kuwait: return "kuwait"
        case .qatar: return "qatar"
        case .singapore: return "singapore"
        case .moonsightingCommittee: return "moonsighting"
        case .turkey: return "turkey"
        }
    }

    var title: String { L("core_ui_method_\(key)") }
    var region: String { L("core_ui_method_\(key)_region") }
}

extension AsrMethod {
    var title: String { self == .hanafi ? L("core_ui_asr_hanafi_short") : L("core_ui_asr_standard_short") }
}

extension HijriDate {
    var text: String {
        L("core_ui_hijri_date", day, L("core_ui_hijri_months.\(month - 1)"), year)
    }
}

/// Times and dates are shown in the place's own time zone (the phone may be elsewhere).
enum Formats {
    static func time(_ date: Date, in zone: TimeZone) -> String {
        let f = DateFormatter()
        f.locale = .current
        f.calendar = Calendar(identifier: .gregorian)
        f.timeZone = zone
        f.timeStyle = .short
        f.dateStyle = .none
        return f.string(from: date)
    }

    /// Always the Gregorian date (Arabic locales default to the Islamic calendar; the Hijri date is shown beside it).
    static func longDate(_ date: Date, in zone: TimeZone) -> String {
        let f = DateFormatter()
        f.locale = .current
        f.calendar = Calendar(identifier: .gregorian)
        f.timeZone = zone
        f.setLocalizedDateFormatFromTemplate("EEEEdMMMM")
        return f.string(from: date)
    }

    /// A day of the place's calendar, written like [longDate] ("Tuesday, 13 October").
    static func longDate(_ day: LocalDay) -> String { longDate(day.noon, in: TimeZone(identifier: "UTC")!) }

    /// "Tue" / "الثلاثاء", short.
    static func weekdayShort(_ day: LocalDay) -> String { utcFormat(day, template: "EEE") }

    /// "Feb" / "فبراير".
    static func monthShort(_ day: LocalDay) -> String { utcFormat(day, template: "MMM") }

    /// "October 2026".
    static func monthTitle(_ day: LocalDay) -> String { utcFormat(day, template: "MMMMy") }

    private static func utcFormat(_ day: LocalDay, template: String) -> String {
        let f = DateFormatter()
        f.locale = .current
        f.calendar = Calendar(identifier: .gregorian)
        f.timeZone = TimeZone(identifier: "UTC")
        f.setLocalizedDateFormatFromTemplate(template)
        return f.string(from: day.noon)
    }

    /// 01:23:45, with the language's own digits.
    static func countdown(_ interval: TimeInterval) -> String {
        let total = max(Int(interval), 0)
        let text = String(format: "%02d:%02d:%02d", total / 3600, total % 3600 / 60, total % 60)
        return localDigits(text)
    }

    static func localDigits(_ text: String) -> String {
        let formatter = NumberFormatter()
        formatter.locale = .current
        guard let zero = formatter.string(from: 0), zero != "0", let zeroScalar = zero.unicodeScalars.first else { return text }
        return String(String.UnicodeScalarView(text.unicodeScalars.map { scalar in
            guard ("0"..."9").contains(scalar), let shifted = Unicode.Scalar(zeroScalar.value + scalar.value - 48) else { return scalar }
            return shifted
        }))
    }
}
