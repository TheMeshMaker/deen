package io.github.themeshmaker.dunya

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.themeshmaker.dunya.core.domain.UserDataRepository
import io.github.themeshmaker.dunya.core.model.ThemeStyle
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface MainActivityUiState {
    /** Settings not read yet; the splash screen stays up. */
    data object Loading : MainActivityUiState

    data class Ready(val onboardingComplete: Boolean, val themeStyle: ThemeStyle) : MainActivityUiState
}

@HiltViewModel
class MainActivityViewModel @Inject constructor(userDataRepository: UserDataRepository) : ViewModel() {
    val uiState: StateFlow<MainActivityUiState> = userDataRepository.userData
        .map { MainActivityUiState.Ready(it.onboardingComplete, it.themeStyle) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, MainActivityUiState.Loading)
}
