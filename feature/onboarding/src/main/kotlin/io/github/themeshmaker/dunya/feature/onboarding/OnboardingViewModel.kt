/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.themeshmaker.dunya.core.domain.UserDataRepository
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(private val userDataRepository: UserDataRepository) : ViewModel() {
    fun finish(onFinished: () -> Unit) {
        viewModelScope.launch {
            userDataRepository.setOnboardingComplete()
            onFinished()
        }
    }
}
