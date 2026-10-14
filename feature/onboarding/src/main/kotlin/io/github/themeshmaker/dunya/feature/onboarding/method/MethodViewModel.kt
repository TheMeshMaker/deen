/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.feature.onboarding.method

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.themeshmaker.dunya.core.domain.UserDataRepository
import io.github.themeshmaker.dunya.core.model.AsrMethod
import io.github.themeshmaker.dunya.core.model.CalculationMethod
import io.github.themeshmaker.dunya.core.model.PrayerSettings
import io.github.themeshmaker.dunya.core.prayer.MethodSuggestions
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MethodUiState(
    val loaded: Boolean = false,
    val method: CalculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE,
    val asrMethod: AsrMethod = AsrMethod.STANDARD,
    /** What we'd suggest for the saved location's country; marked in the list. */
    val suggestedMethod: CalculationMethod? = null
)

@HiltViewModel
class MethodViewModel @Inject constructor(private val userDataRepository: UserDataRepository) : ViewModel() {

    private val state = MutableStateFlow(MethodUiState())
    val uiState: StateFlow<MethodUiState> = state.asStateFlow()

    private val saved = Channel<Unit>(Channel.BUFFERED)
    val settingsSaved: Flow<Unit> = saved.receiveAsFlow()

    private var existing: PrayerSettings? = null
    private var userChoseManually = false

    init {
        // Follows the saved location, so going back in onboarding and picking another city
        // updates the suggestion (unless the user already picked something themselves).
        viewModelScope.launch {
            userDataRepository.userData
                .map { it.location?.countryCode to it.prayerSettings }
                .distinctUntilChanged()
                .collect { (country, settings) ->
                    existing = settings
                    state.update { current ->
                        val suggested = country?.let { MethodSuggestions.method(it) }
                        if (current.loaded && userChoseManually) {
                            current.copy(suggestedMethod = suggested)
                        } else {
                            MethodUiState(
                                loaded = true,
                                method = settings?.method ?: MethodSuggestions.method(country),
                                asrMethod = settings?.asrMethod ?: MethodSuggestions.asrMethod(country),
                                suggestedMethod = suggested
                            )
                        }
                    }
                }
        }
    }

    fun selectMethod(method: CalculationMethod) {
        userChoseManually = true
        state.update { it.copy(method = method) }
    }

    fun selectAsr(asrMethod: AsrMethod) {
        userChoseManually = true
        state.update { it.copy(asrMethod = asrMethod) }
    }

    fun save() {
        val current = state.value
        if (!current.loaded) return
        viewModelScope.launch {
            // Keep any other settings (offsets, Hijri correction…) the user already has.
            val settings = existing?.copy(method = current.method, asrMethod = current.asrMethod)
                ?: PrayerSettings(method = current.method, asrMethod = current.asrMethod)
            userDataRepository.setPrayerSettings(settings)
            saved.send(Unit)
        }
    }
}
