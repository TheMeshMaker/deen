import DeenKit
import SwiftUI

/// Settings → Adhan: what happens at each prayer, the voices, and the optional reminders.
struct AlertSettingsSection: View {
    @Environment(AppModel.self) private var model
    @Environment(\.openURL) private var openURL
    @State private var player = AdhanPlayer.shared
    @State private var testScheduled = false

    var body: some View {
        Section {
            ForEach(Prayer.allCases, id: \.self) { prayer in
                Picker(selection: modeBinding(prayer)) {
                    ForEach(prayer == .sunrise ? [AlertMode.notification, .silent] : AlertMode.allCases, id: \.self) { mode in
                        Label(mode.title, systemImage: mode.symbol).tag(mode)
                    }
                } label: {
                    Label(prayer.name(), systemImage: prayer.symbol)
                }
            }
        } header: {
            Text(L("feature_settings_adhan"))
        } footer: {
            VStack(alignment: .leading, spacing: 8) {
                if AlertScheduler.alarmsDenied {
                    Text(L("ios_alarms_off"))
                    Button(L("feature_reliability_open_settings")) {
                        if let url = URL(string: UIApplication.openSettingsURLString) { openURL(url) }
                    }
                }
                Text(L("ios_adhan_note"))
            }
        }

        Section {
            voicePicker(L("feature_settings_voice"), selection: model.binding(\.alerts.voice), allowSame: false)
            voicePicker(L("feature_settings_fajr_voice"), selection: fajrBinding, allowSame: true)
            Button {
                Task {
                    await AlertScheduler.scheduleTest(voice: model.settings.alerts.voice)
                    testScheduled = true
                }
            } label: {
                Label(L("feature_reliability_test_button"), systemImage: "alarm.waves.left.and.right")
            }
            if testScheduled {
                Text(L("ios_test_scheduled")).font(.footnote).foregroundStyle(.secondary)
            }
        } footer: {
            Text(L("feature_settings_fajr_note"))
        }

        Section {
            Picker(L("feature_settings_reminder"), selection: model.binding(\.alerts.reminderMinutes)) {
                ForEach(AlertSettings.reminderChoices, id: \.self) { Text(minutes($0)).tag($0) }
            }
            Toggle(L("feature_settings_after_adhan"), isOn: afterAdhanOn)
            if model.settings.alerts.afterAdhanOn {
                ForEach(Prayer.prayers, id: \.self) { prayer in
                    Picker(prayer.name(), selection: afterBinding(prayer)) {
                        ForEach(AlertSettings.afterAdhanChoices, id: \.self) { Text(minutes($0)).tag($0) }
                    }
                }
            }
        } footer: {
            Text("\(L("feature_settings_reminder_body")) \(L("feature_settings_after_adhan_body"))")
        }
    }

    private func voicePicker(_ title: String, selection: Binding<String>, allowSame: Bool) -> some View {
        HStack {
            Picker(title, selection: selection) {
                if allowSame { Text(L("feature_settings_fajr_same")).tag("") }
                ForEach(AlertSettings.voices, id: \.self) { Text(voiceName($0)).tag($0) }
            }
            let voice = selection.wrappedValue.isEmpty ? model.settings.alerts.voice : selection.wrappedValue
            Button {
                if player.playing == voice { player.stop() } else { player.play(voice: voice) }
            } label: {
                Image(systemName: player.playing == voice ? "stop.circle.fill" : "play.circle.fill").font(.title2)
            }
            .buttonStyle(.borderless)
            .accessibilityLabel(L(player.playing == voice ? "feature_settings_voice_stop" : "feature_settings_voice_play"))
        }
    }

    private func voiceName(_ voice: String) -> String { L("core_alarm_voice_\(voice)") }

    private func minutes(_ value: Int) -> String {
        value == 0 ? L("feature_settings_off") : L("feature_settings_minutes_short", Formats.localDigits("\(value)"))
    }

    private func modeBinding(_ prayer: Prayer) -> Binding<AlertMode> {
        Binding(get: { model.settings.alerts.mode(for: prayer) },
                set: { mode in model.update { $0.alerts.modes[prayer] = mode } })
    }

    private var fajrBinding: Binding<String> {
        Binding(get: { model.settings.alerts.fajrVoice ?? "" },
                set: { voice in model.update { $0.alerts.fajrVoice = voice.isEmpty ? nil : voice } })
    }

    private var afterAdhanOn: Binding<Bool> {
        Binding(get: { model.settings.alerts.afterAdhanOn },
                set: { on in
                    model.update {
                        $0.alerts.afterAdhanMinutes = on
                            ? Dictionary(uniqueKeysWithValues: Prayer.prayers.map { ($0, AlertSettings.afterAdhanDefault) }) : [:]
                    }
                })
    }

    private func afterBinding(_ prayer: Prayer) -> Binding<Int> {
        Binding(get: { model.settings.alerts.afterAdhan(for: prayer) },
                set: { value in model.update { $0.alerts.afterAdhanMinutes[prayer] = value } })
    }
}

extension AlertMode {
    var title: String {
        switch self {
        case .adhan: return L("feature_today_mode_adhan")
        case .notification: return L("feature_today_mode_notification")
        case .silent: return L("feature_today_mode_silent")
        }
    }

    var symbol: String {
        switch self {
        case .adhan: return "speaker.wave.2.fill"
        case .notification: return "bell.fill"
        case .silent: return "bell.slash"
        }
    }

    /// Tapping the icon on Today cycles Adhan → Notification → Silent (sunrise: Notification ↔ Silent).
    func next(for prayer: Prayer) -> AlertMode {
        if prayer == .sunrise { return self == .silent ? .notification : .silent }
        switch self {
        case .adhan: return .notification
        case .notification: return .silent
        case .silent: return .adhan
        }
    }
}
