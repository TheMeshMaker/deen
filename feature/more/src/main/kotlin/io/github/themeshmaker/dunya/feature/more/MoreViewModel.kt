package io.github.themeshmaker.dunya.feature.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.themeshmaker.dunya.core.domain.UserDataRepository
import io.github.themeshmaker.dunya.core.model.AppBuildInfo
import io.github.themeshmaker.dunya.core.model.ThemeStyle
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MoreUiState(val buildInfo: AppBuildInfo, val themeStyle: ThemeStyle = ThemeStyle.EMERALD)

@HiltViewModel
class MoreViewModel @Inject constructor(buildInfo: AppBuildInfo, private val userDataRepository: UserDataRepository) : ViewModel() {

    val uiState: StateFlow<MoreUiState> = userDataRepository.userData
        .map { MoreUiState(buildInfo, it.themeStyle) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MoreUiState(buildInfo))

    fun selectTheme(style: ThemeStyle) {
        viewModelScope.launch { userDataRepository.setThemeStyle(style) }
    }
}
