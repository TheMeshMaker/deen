package io.github.themeshmaker.dunya.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.github.themeshmaker.dunya.core.designsystem.component.GeometricPattern
import io.github.themeshmaker.dunya.core.designsystem.component.LightSystemBarIcons
import io.github.themeshmaker.dunya.core.designsystem.icon.DeenIcons
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTextStyles
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTheme
import io.github.themeshmaker.dunya.feature.onboarding.location.LocationContent
import io.github.themeshmaker.dunya.feature.onboarding.method.MethodContent

private enum class Step { WELCOME, LOCATION, METHOD }

/**
 * First run. Notification, exact-alarm and battery steps join this flow in Milestone 2
 * (docs/BLUEPRINT.md §16.4).
 */
@Composable
internal fun OnboardingScreen(onFinished: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    var step by rememberSaveable { mutableStateOf(Step.WELCOME) }
    BackHandler(enabled = step != Step.WELCOME) { step = Step.entries[step.ordinal - 1] }

    Surface(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = step,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "onboarding"
        ) { current ->
            when (current) {
                Step.WELCOME -> WelcomeStep(onStart = { step = Step.LOCATION })

                Step.LOCATION -> LocationContent(
                    onSaved = { step = Step.METHOD },
                    modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                    header = {
                        StepHeader(
                            title = stringResource(R.string.feature_onboarding_location_title),
                            body = stringResource(R.string.feature_onboarding_location_body),
                            onBack = { step = Step.WELCOME }
                        )
                    }
                )

                Step.METHOD -> MethodContent(
                    onSaved = { viewModel.finish(onFinished) },
                    modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                    header = {
                        StepHeader(
                            title = stringResource(R.string.feature_onboarding_method_title),
                            body = stringResource(R.string.feature_onboarding_method_body),
                            onBack = { step = Step.LOCATION }
                        )
                    }
                ) { save, enabled ->
                    Button(onClick = save, enabled = enabled, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(stringResource(R.string.feature_onboarding_finish))
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep(onStart: () -> Unit) {
    val gradient = DeenTheme.gradient
    LightSystemBarIcons()
    Box(
        Modifier
            .fillMaxSize()
            .background(gradient.brush)
    ) {
        GeometricPattern(color = gradient.pattern, modifier = Modifier.fillMaxSize())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("دِين", style = DeenTextStyles.arabicDisplay.copy(fontSize = 64.sp, lineHeight = 100.sp), color = gradient.content)
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.feature_onboarding_welcome_title),
                style = MaterialTheme.typography.headlineMedium,
                color = gradient.content,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.feature_onboarding_welcome_body),
                style = MaterialTheme.typography.bodyLarge,
                color = gradient.content.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(40.dp))
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = gradient.content, contentColor = gradient.start)
            ) {
                Text(stringResource(R.string.feature_onboarding_start), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun StepHeader(title: String, body: String, onBack: () -> Unit) {
    Column(Modifier.padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)) {
        BackButton(onBack)
        Column(Modifier.padding(start = 12.dp)) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun BackButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(painterResource(DeenIcons.Back), contentDescription = stringResource(R.string.feature_onboarding_back))
    }
}

@Preview
@Composable
private fun WelcomeStepPreview() {
    DeenTheme { Surface { WelcomeStep(onStart = {}) } }
}
