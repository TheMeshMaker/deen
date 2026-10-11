package io.github.themeshmaker.dunya.feature.more

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.themeshmaker.dunya.core.model.AppBuildInfo
import javax.inject.Inject

@HiltViewModel
class MoreViewModel @Inject constructor(buildInfo: AppBuildInfo) : ViewModel() {
    val canDonate: Boolean = buildInfo.canLinkToDonations
}
