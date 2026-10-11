package io.github.themeshmaker.dunya.feature.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.themeshmaker.dunya.core.alarm.AdhanPreview
import io.github.themeshmaker.dunya.core.alarm.AdhanVoices
import io.github.themeshmaker.dunya.core.domain.UserDataRepository
import io.github.themeshmaker.dunya.core.model.AdhanVoice
import io.github.themeshmaker.dunya.core.model.AlertSettings
import io.github.themeshmaker.dunya.core.model.AppBuildInfo
import io.github.themeshmaker.dunya.core.model.CustomColors
import io.github.themeshmaker.dunya.core.model.MasjidReminder
import io.github.themeshmaker.dunya.core.model.Prayer
import io.github.themeshmaker.dunya.core.model.ReadingSettings
import io.github.themeshmaker.dunya.core.model.ThemeMode
import io.github.themeshmaker.dunya.core.model.ThemeStyle
import io.github.themeshmaker.dunya.core.model.UiStyle
import io.github.themeshmaker.dunya.core.model.UserData
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(val data: UserData?, val buildInfo: AppBuildInfo, val customVoice: String?, val customFajrVoice: String?)

/** One-off messages for a snackbar or toast. */
enum class SettingsEvent {
    SECRETS_UNLOCKED,
    VOICE_IMPORTED,
    VOICE_TOO_LARGE,
    VOICE_NOT_AUDIO,
    VOICE_FAILED
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userData: UserDataRepository,
    private val voices: AdhanVoices,
    private val preview: AdhanPreview,
    buildInfo: AppBuildInfo
) : ViewModel() {

    private val customNames = MutableStateFlow(voices.customName(false) to voices.customName(true))

    val uiState: StateFlow<SettingsUiState> = combine(userData.userData, customNames) { data, (custom, customFajr) ->
        SettingsUiState(data, buildInfo, custom ?: customLabelIfPresent(false), customFajr ?: customLabelIfPresent(true))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState(null, buildInfo, null, null))

    private val _events = MutableSharedFlow<SettingsEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<SettingsEvent> = _events

    /** The voice being previewed, for the play/stop icon. */
    val previewing = MutableStateFlow<String?>(null)

    val bundledVoices: List<AdhanVoices.Bundled> get() = voices.bundled

    private var versionTaps = 0

    private fun customLabelIfPresent(fajr: Boolean): String? = if (voices.hasCustom(fajr)) "" else null

    fun setThemeStyle(style: ThemeStyle) = launch { userData.setThemeStyle(style) }

    /** Saves the two colours and switches to the custom theme. */
    fun setCustomColors(colors: CustomColors) = launch {
        userData.setCustomColors(colors)
        userData.setThemeStyle(ThemeStyle.CUSTOM)
    }

    fun setThemeMode(mode: ThemeMode) = launch { userData.setThemeMode(mode) }

    fun setUiStyle(style: UiStyle) = launch { userData.setUiStyle(style) }

    fun setReading(transform: (ReadingSettings) -> ReadingSettings) = launch {
        userData.setReadingSettings(transform(userData.userData.first().reading))
    }

    fun setAlerts(transform: (AlertSettings) -> AlertSettings) = launch {
        userData.setAlertSettings(transform(userData.userData.first().alertSettings))
    }

    fun setVoice(voice: String, fajr: Boolean) = setAlerts { if (fajr) it.copy(fajrVoice = voice) else it.copy(voice = voice) }

    /** Fajr only: back to the same recording as the other prayers. */
    fun useSameVoiceForFajr() = setAlerts { it.copy(fajrVoice = null) }

    fun setMasjid(transform: (MasjidReminder) -> MasjidReminder) = setAlerts { it.copy(masjid = transform(it.masjid)) }

    fun toggleMasjidPrayer(prayer: Prayer) = setMasjid { m ->
        val prayers = if (prayer in m.prayers) m.prayers - prayer else m.prayers + prayer
        m.copy(prayers = prayers.ifEmpty { m.prayers })
    }

    fun importVoice(uri: Uri, fajr: Boolean) = launch {
        when (voices.importCustom(uri, fajr)) {
            AdhanVoices.ImportResult.OK -> {
                customNames.value = voices.customName(false) to voices.customName(true)
                setVoice(AdhanVoice.CUSTOM, fajr)
                _events.emit(SettingsEvent.VOICE_IMPORTED)
            }

            AdhanVoices.ImportResult.TOO_LARGE -> _events.emit(SettingsEvent.VOICE_TOO_LARGE)

            AdhanVoices.ImportResult.NOT_AUDIO -> _events.emit(SettingsEvent.VOICE_NOT_AUDIO)

            AdhanVoices.ImportResult.FAILED -> _events.emit(SettingsEvent.VOICE_FAILED)
        }
    }

    fun togglePreview(voice: String, fajr: Boolean) {
        if (previewing.value == voice) {
            preview.stop()
            previewing.value = null
        } else {
            previewing.value = voice
            preview.play(voice, fajr) { previewing.value = null }
        }
    }

    fun stopPreview() {
        preview.stop()
        previewing.value = null
    }

    /** About → version: seven taps reveal the hidden options, like Android's developer options. */
    fun tapVersion() {
        versionTaps++
        if (versionTaps == SECRET_TAPS) {
            launch {
                userData.setSecretsUnlocked(true)
                _events.emit(SettingsEvent.SECRETS_UNLOCKED)
            }
        }
    }

    fun hideSecrets() = launch {
        versionTaps = 0
        userData.setSecretsUnlocked(false)
    }

    override fun onCleared() {
        preview.release()
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private companion object {
        const val SECRET_TAPS = 7
    }
}
