package io.github.themeshmaker.dunya.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.themeshmaker.dunya.core.designsystem.component.pressScale
import io.github.themeshmaker.dunya.core.designsystem.component.softShadow
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenMotion
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTheme
import io.github.themeshmaker.dunya.core.model.DeenLinks
import io.github.themeshmaker.dunya.feature.adhkar.AdhkarReaderRoute
import io.github.themeshmaker.dunya.feature.adhkar.TasbihRoute
import io.github.themeshmaker.dunya.feature.adhkar.adhkarScreens
import io.github.themeshmaker.dunya.feature.hadith.HadithBookRoute
import io.github.themeshmaker.dunya.feature.hadith.HadithCollectionRoute
import io.github.themeshmaker.dunya.feature.hadith.NawawiRoute
import io.github.themeshmaker.dunya.feature.hadith.ProphetFactsRoute
import io.github.themeshmaker.dunya.feature.hadith.hadithScreens
import io.github.themeshmaker.dunya.feature.more.MoreActions
import io.github.themeshmaker.dunya.feature.more.moreScreen
import io.github.themeshmaker.dunya.feature.onboarding.LocationPickerRoute
import io.github.themeshmaker.dunya.feature.onboarding.MethodPickerRoute
import io.github.themeshmaker.dunya.feature.onboarding.OnboardingRoute
import io.github.themeshmaker.dunya.feature.onboarding.locationPickerScreen
import io.github.themeshmaker.dunya.feature.onboarding.methodPickerScreen
import io.github.themeshmaker.dunya.feature.onboarding.onboardingScreen
import io.github.themeshmaker.dunya.feature.qibla.QiblaRoute
import io.github.themeshmaker.dunya.feature.qibla.qiblaScreen
import io.github.themeshmaker.dunya.feature.quran.QuranReaderRoute
import io.github.themeshmaker.dunya.feature.quran.QuranTafsirRoute
import io.github.themeshmaker.dunya.feature.quran.quranScreens
import io.github.themeshmaker.dunya.feature.reliability.ReliabilityRoute
import io.github.themeshmaker.dunya.feature.reliability.reliabilityScreen
import io.github.themeshmaker.dunya.feature.settings.AboutRoute
import io.github.themeshmaker.dunya.feature.settings.AdhanVoiceRoute
import io.github.themeshmaker.dunya.feature.settings.SettingsRoute
import io.github.themeshmaker.dunya.feature.settings.settingsScreens
import io.github.themeshmaker.dunya.feature.timetable.TimetableRoute
import io.github.themeshmaker.dunya.feature.timetable.timetableScreen
import io.github.themeshmaker.dunya.feature.today.TodayActions
import io.github.themeshmaker.dunya.feature.today.TodayRoute
import io.github.themeshmaker.dunya.feature.today.todayScreen
import io.github.themeshmaker.dunya.navigation.TopLevelDestination

/**
 * App shell. The navigation (bottom bar on phones, rail on unfolded foldables and tablets) only
 * shows on the five tabs; readers and settings get the whole screen. In the Sleek style phones
 * get a floating pill-shaped bar instead of the standard one.
 *
 * @param link an in-app destination from a notification (see [DeenLinks]); opened once.
 */
@Composable
fun DeenApp(startDestination: Any, link: String?, onLinkHandled: () -> Unit, navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val onTab =
        currentDestination?.let { d -> TopLevelDestination.entries.any { d.hasRoute(it.routeClass) } } ?: (startDestination == TodayRoute)

    LaunchedEffect(link, currentDestination != null) {
        val category = DeenLinks.adhkarCategory(link) ?: return@LaunchedEffect
        if (currentDestination == null || currentDestination.hasRoute(OnboardingRoute::class)) return@LaunchedEffect
        navController.navigate(AdhkarReaderRoute(category)) { launchSingleTop = true }
        onLinkHandled()
    }

    val look = DeenTheme.look
    val adaptive = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfoV2())
    val floatingBar = look.sleek && adaptive == NavigationSuiteType.NavigationBar

    val host: @Composable (Modifier) -> Unit = { modifier ->
        if (look.motion) {
            // Sleek: a pushed screen slides in from the end while the one below drifts back a
            // quarter of the way and dims (and the reverse on back); tabs cross-fade.
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = modifier,
                enterTransition = {
                    if (initialState.destination.isTab() && targetState.destination.isTab()) {
                        fadeIn(tween(240, delayMillis = 60)) +
                            scaleIn(tween(DeenMotion.SCREEN_MS, easing = DeenMotion.EaseOutSoft), initialScale = 0.97f)
                    } else {
                        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, DeenMotion.screen())
                    }
                },
                exitTransition = {
                    if (initialState.destination.isTab() && targetState.destination.isTab()) {
                        fadeOut(tween(140))
                    } else {
                        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, DeenMotion.screen()) { it / 4 } +
                            fadeOut(DeenMotion.screen(), targetAlpha = 0.5f)
                    }
                },
                popEnterTransition = {
                    if (initialState.destination.isTab() && targetState.destination.isTab()) {
                        fadeIn(tween(240, delayMillis = 60))
                    } else {
                        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, DeenMotion.screen()) { it / 4 } +
                            fadeIn(DeenMotion.screen(), initialAlpha = 0.5f)
                    }
                },
                popExitTransition = {
                    if (initialState.destination.isTab() && targetState.destination.isTab()) {
                        fadeOut(tween(140))
                    } else {
                        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, DeenMotion.screen())
                    }
                }
            ) {
                graph(navController)
            }
        } else {
            NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
                graph(navController)
            }
        }
    }

    if (floatingBar) {
        // A Surface, not just a background: it also sets the content colour for text on the page.
        Surface(color = look.page, modifier = Modifier.fillMaxSize()) {
            Column {
                host(Modifier.weight(1f))
                // The bar slides away (and its space closes) when a reader opens, and back on the tabs.
                AnimatedVisibility(
                    visible = onTab,
                    enter = if (look.motion) {
                        expandVertically(DeenMotion.snappy(), expandFrom = Alignment.Top) + slideInVertically(DeenMotion.snappy()) { it } +
                            fadeIn()
                    } else {
                        fadeIn(snap())
                    },
                    exit = if (look.motion) {
                        shrinkVertically(DeenMotion.snappy(), shrinkTowards = Alignment.Top) +
                            slideOutVertically(DeenMotion.snappy()) { it } +
                            fadeOut()
                    } else {
                        fadeOut(snap())
                    }
                ) {
                    FloatingNavBar(currentDestination) { navController.navigateToTopLevel(it) }
                }
            }
        }
        return
    }

    // Deen's own bar: plain surface, theme-coloured selection, no Material "pill" indicator.
    val itemColors = NavigationSuiteDefaults.itemColors(
        navigationBarItemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = Color.Transparent,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        navigationRailItemColors = NavigationRailItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = Color.Transparent,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
    NavigationSuiteScaffold(
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationBarContainerColor = MaterialTheme.colorScheme.surface,
            navigationRailContainerColor = look.page
        ),
        layoutType = if (onTab) adaptive else NavigationSuiteType.None,
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { destination ->
                val selected = currentDestination.isOn(destination)
                item(
                    selected = selected,
                    onClick = { navController.navigateToTopLevel(destination) },
                    colors = itemColors,
                    icon = {
                        Icon(painterResource(if (selected) destination.selectedIcon else destination.icon), contentDescription = null)
                    },
                    label = {
                        Text(stringResource(destination.label), fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                    }
                )
            }
        }
    ) {
        host(Modifier.fillMaxSize())
    }
}

/**
 * Sleek: a rounded bar floating above the page on a wide soft shadow, the selected tab as a
 * gradient pill. Changing tab gives a light haptic tick.
 */
@Composable
private fun FloatingNavBar(current: NavDestination?, onSelect: (TopLevelDestination) -> Unit) {
    val look = DeenTheme.look
    val haptics = LocalHapticFeedback.current
    val soft = look.cardElevation > 0.dp
    Surface(
        shape = CircleShape,
        color = look.card,
        border = look.cardBorder,
        shadowElevation = if (soft) 0.dp else 6.dp,
        modifier = Modifier
            .navigationBarsPadding()
            .padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 10.dp)
            .softShadow(look, CircleShape)
            .fillMaxWidth()
    ) {
        Row(Modifier.padding(6.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            TopLevelDestination.entries.forEach { destination ->
                val selected = current.isOn(destination)
                NavPill(destination, selected = selected, motion = look.motion, onClick = {
                    if (look.motion && !selected) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    onSelect(destination)
                })
            }
        }
    }
}

/**
 * One tab of the floating bar. The selected one widens into a filled pill with its label, so
 * changing tabs looks like the pill flowing from one place to the other.
 */
@Composable
private fun RowScope.NavPill(destination: TopLevelDestination, selected: Boolean, motion: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val gradient = DeenTheme.gradient
    val weight by animateFloatAsState(if (selected) 2.6f else 1f, if (motion) DeenMotion.snappy() else snap(), label = "navWeight")
    val fill by animateFloatAsState(if (selected) 1f else 0f, if (motion) tween(260) else snap(), label = "navFill")
    val tint by animateColorAsState(
        if (selected) gradient.content else colors.onSurfaceVariant,
        if (motion) tween(220) else snap(),
        label = "navTint"
    )
    val bounce = remember { Animatable(1f) }
    LaunchedEffect(selected, motion) {
        if (selected && motion) {
            bounce.snapTo(0.72f)
            bounce.animateTo(1f, DeenMotion.bouncy())
        }
    }
    val interaction = remember { MutableInteractionSource() }
    Row(
        Modifier
            .weight(weight)
            .pressScale(interaction, enabled = motion, pressed = 0.92f)
            .clip(CircleShape)
            .background(gradient.brush, alpha = fill)
            .clickable(interactionSource = interaction, indication = null, role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painterResource(if (selected) destination.selectedIcon else destination.icon),
            contentDescription = if (selected) null else stringResource(destination.label),
            tint = tint,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer {
                    scaleX = bounce.value
                    scaleY = bounce.value
                }
        )
        AnimatedVisibility(
            visible = selected,
            enter = if (motion) fadeIn(tween(220, delayMillis = 70)) + expandHorizontally(DeenMotion.snappy()) else fadeIn(snap()),
            exit = if (motion) fadeOut(tween(90)) + shrinkHorizontally(DeenMotion.snappy()) else fadeOut(snap())
        ) {
            Text(
                stringResource(destination.label),
                style = MaterialTheme.typography.labelLarge,
                color = gradient.content,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}

private fun NavGraphBuilder.graph(navController: NavHostController) {
    val back: () -> Unit = { navController.popBackStack() }
    onboardingScreen(
        onFinished = {
            // Straight on to adhan setup (notifications, exact alarms, battery), then Today.
            navController.navigate(ReliabilityRoute(setup = true)) { popUpTo<OnboardingRoute> { inclusive = true } }
        }
    )
    locationPickerScreen(onDone = back)
    methodPickerScreen(onDone = back)
    reliabilityScreen(
        onDone = { setup ->
            if (setup) {
                navController.navigate(TodayRoute) { popUpTo<ReliabilityRoute> { inclusive = true } }
            } else {
                navController.popBackStack()
            }
        }
    )
    todayScreen(
        TodayActions(
            onChangeLocation = { navController.navigate(LocationPickerRoute) },
            onChangeMethod = { navController.navigate(MethodPickerRoute) },
            onOpenReliability = { navController.navigate(ReliabilityRoute()) },
            onOpenQibla = { navController.navigate(QiblaRoute) },
            onOpenTimetable = { navController.navigate(TimetableRoute) },
            onOpenTasbih = { navController.navigate(TasbihRoute) },
            onOpenSettings = { navController.navigate(SettingsRoute) },
            onOpenQuran = { surah, ayah -> navController.navigate(QuranReaderRoute(surah, ayah)) },
            onOpenHadith = { number -> navController.navigate(NawawiRoute(number)) },
            onOpenFacts = { navController.navigate(ProphetFactsRoute) }
        )
    )
    quranScreens(
        onOpen = { surah, ayah -> navController.navigate(QuranReaderRoute(surah, ayah)) },
        onBack = back,
        // Next / previous surah replaces the reader instead of stacking readers.
        onOpenSurahInPlace = { surah ->
            navController.navigate(QuranReaderRoute(surah, 1)) { popUpTo<QuranReaderRoute> { inclusive = true } }
        },
        onOpenTafsir = { surah, ayah -> navController.navigate(QuranTafsirRoute(surah, ayah)) }
    )
    hadithScreens(
        onOpenNawawi = { navController.navigate(NawawiRoute(it)) },
        onOpenFacts = { navController.navigate(ProphetFactsRoute) },
        onOpenCollection = { navController.navigate(HadithCollectionRoute(it)) },
        onOpenBook = { id, book, highlight -> navController.navigate(HadithBookRoute(id, book, highlight)) },
        onBack = back
    )
    adhkarScreens(
        onOpenCategory = { navController.navigate(AdhkarReaderRoute(it)) },
        onOpenTasbih = { navController.navigate(TasbihRoute) },
        onBack = back
    )
    moreScreen(
        MoreActions(
            onOpenSettings = { navController.navigate(SettingsRoute) },
            onOpenQibla = { navController.navigate(QiblaRoute) },
            onOpenTimetable = { navController.navigate(TimetableRoute) },
            onOpenTasbih = { navController.navigate(TasbihRoute) },
            onOpenReliability = { navController.navigate(ReliabilityRoute()) },
            onOpenSources = { navController.navigate(AboutRoute) }
        )
    )
    settingsScreens(
        onBack = back,
        onOpenVoice = { fajr -> navController.navigate(AdhanVoiceRoute(fajr)) },
        onOpenReliability = { navController.navigate(ReliabilityRoute()) },
        onOpenLocation = { navController.navigate(LocationPickerRoute) },
        onOpenMethod = { navController.navigate(MethodPickerRoute) },
        onOpenTimetable = { navController.navigate(TimetableRoute) },
        onOpenAbout = { navController.navigate(AboutRoute) }
    )
    qiblaScreen(onBack = back)
    timetableScreen(onBack = back)
}

private fun NavDestination.isTab(): Boolean = TopLevelDestination.entries.any { hasRoute(it.routeClass) }

private fun NavDestination?.isOn(destination: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(destination.routeClass) } == true

/**
 * Standard tab behaviour: one copy of each tab, each keeping its own back stack. Today is
 * always the root (onboarding is removed from the stack when it finishes).
 */
private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo<TodayRoute> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
