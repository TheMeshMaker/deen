/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.themeshmaker.dunya.core.domain.UserDataRepository
import io.github.themeshmaker.dunya.core.model.CustomColors
import io.github.themeshmaker.dunya.core.model.LiquidSettings
import io.github.themeshmaker.dunya.core.model.ThemeMode
import io.github.themeshmaker.dunya.core.model.ThemeStyle
import io.github.themeshmaker.dunya.core.model.UiStyle
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface MainActivityUiState {
    /** Settings not read yet; the splash screen stays up. */
    data object Loading : MainActivityUiState

    data class Ready(
        val onboardingComplete: Boolean,
        val themeStyle: ThemeStyle,
        val themeMode: ThemeMode,
        val uiStyle: UiStyle,
        val customColors: CustomColors = CustomColors(),
        val liquid: LiquidSettings = LiquidSettings()
    ) : MainActivityUiState
}

@HiltViewModel
class MainActivityViewModel @Inject constructor(userDataRepository: UserDataRepository) : ViewModel() {
    val uiState: StateFlow<MainActivityUiState> = userDataRepository.userData
        .map { MainActivityUiState.Ready(it.onboardingComplete, it.themeStyle, it.themeMode, it.uiStyle, it.customColors, it.liquid) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, MainActivityUiState.Loading)
}
