/*
 * Deen — Copyright © 2026 TheMeshMaker
 * Licensed under the GNU GPL, version 3 or later, with the additional terms in NOTICE.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.themeshmaker.dunya.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.github.themeshmaker.dunya.core.designsystem.component.GeometricPattern
import io.github.themeshmaker.dunya.core.designsystem.component.LightSystemBarIcons
import io.github.themeshmaker.dunya.core.designsystem.component.ThinProgress
import io.github.themeshmaker.dunya.core.designsystem.icon.DeenIcons
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenMotion
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTextStyles
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTheme
import io.github.themeshmaker.dunya.core.designsystem.theme.rememberReducedMotion
import io.github.themeshmaker.dunya.core.ui.AppLocales
import io.github.themeshmaker.dunya.core.ui.LanguageSheet
import io.github.themeshmaker.dunya.core.ui.displayName
import io.github.themeshmaker.dunya.core.ui.findActivity
import io.github.themeshmaker.dunya.core.ui.localDigits
import io.github.themeshmaker.dunya.feature.onboarding.location.LocationContent
import io.github.themeshmaker.dunya.feature.onboarding.method.MethodContent
import kotlinx.coroutines.delay

private enum class Step { WELCOME, LOCATION, METHOD }

/** First run: a welcome, then where you are and how prayer times are calculated (docs/BLUEPRINT.md §16.4). */
@Composable
internal fun OnboardingScreen(onFinished: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    var step by rememberSaveable { mutableStateOf(Step.WELCOME) }
    BackHandler(enabled = step != Step.WELCOME) { step = Step.entries[step.ordinal - 1] }
    val reduced = rememberReducedMotion()

    Surface(Modifier.fillMaxSize(), color = DeenTheme.look.page) {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                if (reduced) {
                    fadeIn() togetherWith fadeOut()
                } else {
                    // Forward slides in from the end, back from the start (mirrored in Arabic).
                    val direction = if (targetState.ordinal > initialState.ordinal) SlideDirection.Start else SlideDirection.End
                    (slideIntoContainer(direction, DeenMotion.screen()) { it / 3 } + fadeIn(tween(260, delayMillis = 60))) togetherWith
                        (slideOutOfContainer(direction, DeenMotion.screen()) { it / 4 } + fadeOut(tween(160)))
                }
            },
            label = "onboarding"
        ) { current ->
            when (current) {
                Step.WELCOME -> WelcomeStep(onStart = { step = Step.LOCATION })

                Step.LOCATION -> LocationContent(
                    onSaved = { step = Step.METHOD },
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    header = {
                        StepHeader(
                            number = 1,
                            title = stringResource(R.string.feature_onboarding_location_title),
                            body = stringResource(R.string.feature_onboarding_location_body),
                            onBack = { step = Step.WELCOME }
                        )
                    }
                )

                Step.METHOD -> MethodContent(
                    onSaved = { viewModel.finish(onFinished) },
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    header = {
                        StepHeader(
                            number = 2,
                            title = stringResource(R.string.feature_onboarding_method_title),
                            body = stringResource(R.string.feature_onboarding_method_body),
                            onBack = { step = Step.LOCATION }
                        )
                    }
                ) { save, enabled ->
                    Button(
                        onClick = save,
                        enabled = enabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(56.dp)
                    ) {
                        Text(stringResource(R.string.feature_onboarding_finish), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

/**
 * The welcome: the mark rises out of a slowly drifting star pattern with a breathing glow, then
 * the greeting, three things Deen does and the button. The language can be changed right here.
 */
@Composable
private fun WelcomeStep(onStart: () -> Unit) {
    val gradient = DeenTheme.gradient
    val content = gradient.content
    val reduced = rememberReducedMotion()
    val context = LocalContext.current
    var picking by rememberSaveable { mutableStateOf(false) }
    val language = remember(context) { AppLocales.current(context) }
    LightSystemBarIcons()

    val ambient = rememberInfiniteTransition(label = "welcome")
    val cellPx = with(LocalDensity.current) { PATTERN_CELL.toPx() }
    val drift by ambient.animateFloat(
        initialValue = 0f,
        targetValue = if (reduced) 0f else cellPx,
        animationSpec = infiniteRepeatable(tween(30_000, easing = LinearEasing), RepeatMode.Restart),
        label = "drift"
    )
    val breath by ambient.animateFloat(
        initialValue = 0f,
        targetValue = if (reduced) 0f else 1f,
        animationSpec = infiniteRepeatable(tween(4_200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(gradient.brush)
    ) {
        GeometricPattern(color = gradient.pattern, modifier = Modifier.fillMaxSize(), cell = PATTERN_CELL, offsetX = drift)
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Language, before anything else, so everyone can read what follows.
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Surface(
                        onClick = { picking = true },
                        shape = CircleShape,
                        color = content.copy(alpha = 0.12f),
                        contentColor = content,
                        border = BorderStroke(1.dp, content.copy(alpha = 0.25f)),
                        modifier = Modifier.appear(0, reduced)
                    ) {
                        Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(DeenIcons.Language), contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(
                                language.displayName(),
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .padding(top = 8.dp)
                            .size(176.dp)
                            .drawBehind {
                                val radius = size.minDimension * (0.46f + 0.05f * breath)
                                drawCircle(
                                    Brush.radialGradient(
                                        listOf(content.copy(alpha = 0.20f + 0.08f * breath), Color.Transparent),
                                        center,
                                        radius
                                    ),
                                    radius = radius
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painterResource(DeenIcons.Mark),
                            contentDescription = null,
                            modifier = Modifier
                                .size(132.dp)
                                .appear(1, reduced, from = 0.6f, distance = 0.dp)
                        )
                    }
                    Text(
                        "دِين",
                        style = DeenTextStyles.arabicDisplay.copy(fontSize = 52.sp, lineHeight = 84.sp),
                        color = content,
                        modifier = Modifier.appear(2, reduced)
                    )
                    Text(
                        stringResource(R.string.feature_onboarding_greeting),
                        style = MaterialTheme.typography.titleMedium,
                        color = content.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.appear(3, reduced)
                    )
                    Text(
                        stringResource(R.string.feature_onboarding_welcome_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = content,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .appear(3, reduced)
                    )
                    Text(
                        stringResource(R.string.feature_onboarding_welcome_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = content.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .appear(4, reduced)
                    )
                }

                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(content.copy(alpha = 0.10f))
                        .padding(vertical = 6.dp)
                ) {
                    Highlight(
                        DeenIcons.Adhan,
                        stringResource(R.string.feature_onboarding_highlight_times),
                        content,
                        Modifier.appear(5, reduced)
                    )
                    Highlight(
                        DeenIcons.Quran,
                        stringResource(R.string.feature_onboarding_highlight_quran),
                        content,
                        Modifier.appear(6, reduced)
                    )
                    Highlight(
                        DeenIcons.Adhkar,
                        stringResource(R.string.feature_onboarding_highlight_adhkar),
                        content,
                        Modifier.appear(7, reduced)
                    )
                }

                Column(Modifier.appear(8, reduced), horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = onStart,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = content, contentColor = gradient.start)
                    ) {
                        Text(stringResource(R.string.feature_onboarding_start), style = MaterialTheme.typography.titleMedium)
                        Icon(
                            painterResource(DeenIcons.ChevronEnd),
                            contentDescription = null,
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .size(20.dp)
                        )
                    }
                    Text(
                        stringResource(R.string.feature_onboarding_promise),
                        style = MaterialTheme.typography.labelMedium,
                        color = content.copy(alpha = 0.75f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }
    }
    if (picking) {
        LanguageSheet(
            current = language,
            onPick = { picked ->
                picking = false
                if (picked != language) context.findActivity()?.let { AppLocales.set(it, picked) }
            },
            onDismiss = { picking = false }
        )
    }
}

@Composable
private fun Highlight(icon: Int, text: String, content: Color, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(content.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
        }
        Text(text, style = MaterialTheme.typography.bodyLarge, color = content, modifier = Modifier.weight(1f))
    }
}

/**
 * Fades in while rising (and, with [from] < 1, growing), one element after another by [index].
 * Always on for the welcome (it's the first impression), except with the system's reduce-motion.
 */
private fun Modifier.appear(index: Int, reduced: Boolean, from: Float = 0.96f, distance: androidx.compose.ui.unit.Dp = 18.dp): Modifier =
    composed {
        if (reduced) return@composed this
        val progress = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            delay(120L + index * 70L)
            progress.animateTo(1f, spring(dampingRatio = 0.78f, stiffness = 180f))
        }
        val px = with(LocalDensity.current) { distance.toPx() }
        graphicsLayer {
            val p = progress.value
            alpha = p.coerceIn(0f, 1f)
            translationY = (1f - p) * px
            val scale = from + (1f - from) * p
            scaleX = scale
            scaleY = scale
        }
    }

private val PATTERN_CELL = 44.dp

/** Back, then "Step 1 of 2" with a progress bar, the title and a line of explanation. */
@Composable
private fun StepHeader(number: Int, title: String, body: String, onBack: () -> Unit) {
    Column(Modifier.padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)) {
        BackButton(onBack)
        Column(Modifier.padding(start = 12.dp)) {
            Text(
                stringResource(R.string.feature_onboarding_step, localDigits(number), localDigits(STEPS)),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            ThinProgress(
                number / STEPS.toFloat(),
                Modifier
                    .padding(top = 8.dp, bottom = 16.dp)
                    .fillMaxWidth()
            )
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private const val STEPS = 2

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
