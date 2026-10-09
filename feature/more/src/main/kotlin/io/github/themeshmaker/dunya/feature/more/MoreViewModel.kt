package io.github.themeshmaker.dunya.feature.more

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.themeshmaker.dunya.core.model.AppBuildInfo
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MoreUiState(val buildInfo: AppBuildInfo)

@HiltViewModel
class MoreViewModel @Inject constructor(buildInfo: AppBuildInfo) : ViewModel() {
    val uiState: StateFlow<MoreUiState> = MutableStateFlow(MoreUiState(buildInfo)).asStateFlow()
}
