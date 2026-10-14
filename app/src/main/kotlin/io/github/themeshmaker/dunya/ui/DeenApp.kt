package io.github.themeshmaker.dunya.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.themeshmaker.dunya.core.designsystem.component.LiquidBackdrop
import io.github.themeshmaker.dunya.core.designsystem.component.LiquidScrollEdge
import io.github.themeshmaker.dunya.core.designsystem.component.LocalFloatingBarInset
import io.github.themeshmaker.dunya.core.designsystem.component.LocalLiquidSettings
import io.github.themeshmaker.dunya.core.designsystem.component.PRESS_CONTROL
import io.github.themeshmaker.dunya.core.designsystem.component.ProvideLiquidGlass
import io.github.themeshmaker.dunya.core.designsystem.component.liquidGlass
import io.github.themeshmaker.dunya.core.designsystem.component.liquidSource
import io.github.themeshmaker.dunya.core.designsystem.component.pageBackground
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
import io.github.themeshmaker.dunya.feature.timetable.IslamicCalendarRoute
import io.github.themeshmaker.dunya.feature.timetable.TimetableRoute
import io.github.themeshmaker.dunya.feature.timetable.islamicCalendarScreen
import io.github.themeshmaker.dunya.feature.timetable.timetableScreen
import io.github.themeshmaker.dunya.feature.today.PrayerTrackerRoute
import io.github.themeshmaker.dunya.feature.today.TodayActions
import io.github.themeshmaker.dunya.feature.today.TodayRoute
import io.github.themeshmaker.dunya.feature.today.todayScreen
import io.github.themeshmaker.dunya.feature.today.trackerScreen
import io.github.themeshmaker.dunya.navigation.TopLevelDestination
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

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
        if (link == null || currentDestination == null || currentDestination.hasRoute(OnboardingRoute::class)) return@LaunchedEffect
        val category = DeenLinks.adhkarCategory(link)
        val position = DeenLinks.quranPosition(link)
        when {
            category != null -> navController.navigate(AdhkarReaderRoute(category)) { launchSingleTop = true }
            position != null -> navController.navigate(QuranReaderRoute(position.surah, position.ayah)) { launchSingleTop = true }
            else -> return@LaunchedEffect
        }
        onLinkHandled()
    }

    val look = DeenTheme.look
    val adaptive = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfoV2())
    val floatingBar = look.sleek && adaptive == NavigationSuiteType.NavigationBar

    val host: @Composable (Modifier) -> Unit = { modifier ->
        if (look.liquid && look.motion) {
            // Liquid: pages are clear over the shared backdrop, so screens zoom and fade through
            // each other instead of sliding (two clear pages side by side would overlap).
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = modifier,
                enterTransition = { fadeIn(tween(280, delayMillis = 40)) + scaleIn(DeenMotion.screen(), initialScale = 0.94f) },
                exitTransition = { fadeOut(tween(180)) + scaleOut(DeenMotion.screen(), targetScale = 1.03f) },
                popEnterTransition = { fadeIn(tween(280, delayMillis = 40)) + scaleIn(DeenMotion.screen(), initialScale = 1.03f) },
                popExitTransition = { fadeOut(tween(180)) + scaleOut(DeenMotion.screen(), targetScale = 0.94f) }
            ) {
                graph(navController)
            }
        } else if (look.motion) {
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

    if (look.liquid) {
        // The coloured backdrop (the full theme gradient on Today), the screens over it, and on
        // phones a glass tab bar floating over the screens; tablets keep the side rail.
        val onToday = currentDestination?.hasRoute(TodayRoute::class) ?: (startDestination == TodayRoute)
        // While a page scrolls down the tab bar shrinks to the current tab; scrolling up brings it back.
        var barMinimized by remember { mutableStateOf(false) }
        LaunchedEffect(currentDestination) { barMinimized = false }
        val scrollWatcher = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (available.y < -SCROLL_INTENT) {
                        barMinimized = true
                    } else if (available.y > SCROLL_INTENT) {
                        barMinimized = false
                    }
                    return Offset.Zero
                }
            }
        }
        ProvideLiquidGlass(dark = look.dark) {
            Surface(color = Color.Transparent, contentColor = MaterialTheme.colorScheme.onBackground, modifier = Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize()) {
                    LiquidBackdrop(vivid = onToday, modifier = Modifier.fillMaxSize())
                    if (adaptive == NavigationSuiteType.NavigationBar) {
                        CompositionLocalProvider(LocalFloatingBarInset provides if (onTab) LiquidBarSpace else 0.dp) {
                            host(
                                Modifier
                                    .fillMaxSize()
                                    .nestedScroll(scrollWatcher)
                                    .liquidSource(1f)
                            )
                        }
                        AnimatedVisibility(
                            visible = onTab,
                            modifier = Modifier.align(Alignment.BottomStart),
                            enter = if (look.motion) slideInVertically(DeenMotion.snappy()) { it } + fadeIn() else fadeIn(snap()),
                            exit = if (look.motion) slideOutVertically(DeenMotion.snappy()) { it } + fadeOut() else fadeOut(snap())
                        ) {
                            LiquidTabBar(
                                currentDestination,
                                minimized = barMinimized && look.motion,
                                onExpand = { barMinimized = false }
                            ) { navController.navigateToTopLevel(it) }
                        }
                    } else {
                        RailScaffold(currentDestination, onTab, adaptive, navController, transparent = true) {
                            host(Modifier.fillMaxSize().liquidSource(1f))
                        }
                    }
                    // Content passing under the status bar melts into a blur (iOS's scroll edge effect).
                    LiquidScrollEdge(
                        tint = if (onToday) DeenTheme.gradient.start else look.page,
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsTopHeight(WindowInsets.statusBars.add(WindowInsets(top = 14.dp)))
                    )
                }
            }
        }
        return
    }

    if (floatingBar) {
        // A Surface, not just a background: it also sets the content colour for text on the page.
        Surface(
            color = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxSize()
                .pageBackground(look)
        ) {
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

    RailScaffold(currentDestination, onTab, adaptive, navController, transparent = false) { host(Modifier.fillMaxSize()) }
}

/** Deen's own bar or rail: plain surface, theme-coloured selection, no Material "pill" indicator. */
@Composable
private fun RailScaffold(
    currentDestination: NavDestination?,
    onTab: Boolean,
    adaptive: NavigationSuiteType,
    navController: NavHostController,
    transparent: Boolean,
    content: @Composable () -> Unit
) {
    val look = DeenTheme.look
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
            navigationBarContainerColor = if (look.heritage) look.card else MaterialTheme.colorScheme.surface,
            navigationRailContainerColor = if (transparent) Color.Transparent else look.page
        ),
        containerColor = if (transparent) Color.Transparent else MaterialTheme.colorScheme.background,
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
        content()
    }
}

/** How far a scroll must move before it counts as scrolling down or up (for shrinking the bar). */
private const val SCROLL_INTENT = 6f

/** Room the floating Liquid tab bar takes at the bottom of a tab screen (above the system bar). */
private val LiquidBarSpace = 84.dp
private val LiquidBarHeight = 64.dp

/** The bar shrunk while scrolling down: just the current tab. */
private val LiquidBarCompact = 64.dp

/**
 * Liquid: the iOS 26/27 tab bar. A capsule of glass floats over the screen (which scrolls beneath
 * it); a clearer, tinted lens marks the selected tab. The lens behaves like a drop of liquid: it
 * squashes and stretches as it travels, overshoots a little and settles; pressed or dragged, it
 * lifts out of the bar and follows the finger, ticking as it passes each tab, and lands on the
 * one it is let go over. While a page scrolls down, the bar shrinks to the current tab ([minimized]);
 * scrolling up or tapping it brings it back.
 */
@Composable
internal fun LiquidTabBar(
    current: NavDestination?,
    minimized: Boolean = false,
    onExpand: () -> Unit = {},
    onSelect: (TopLevelDestination) -> Unit
) {
    val look = DeenTheme.look
    val haptics = LocalHapticFeedback.current
    val tabs = TopLevelDestination.entries
    val selected = tabs.indexOfFirst { current.isOn(it) }.coerceAtLeast(0)
    val scope = rememberCoroutineScope()
    val position = remember { Animatable(selected.toFloat()) }
    val pop = remember { Animatable(0f) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(selected) {
        if (dragging) return@LaunchedEffect
        if (!look.motion) {
            position.snapTo(selected.toFloat())
            return@LaunchedEffect
        }
        // A tap: the lens swells as it leaves, glides over and settles with a little wobble.
        launch {
            pop.animateTo(1f, tween(120))
            pop.animateTo(0f, LensSpring)
        }
        position.animateTo(selected.toFloat(), LensSpring)
    }
    val lift by animateFloatAsState(if (dragging) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = 420f), label = "lensLift")
    val collapse by animateFloatAsState(
        if (minimized) 1f else 0f,
        if (look.motion) spring(dampingRatio = 0.82f, stiffness = 360f) else snap(),
        label = "barCollapse"
    )
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val lensTint = MaterialTheme.colorScheme.primary.copy(alpha = if (look.dark) 0.30f else 0.16f)
    BoxWithConstraints(
        Modifier
            .navigationBarsPadding()
            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
            .fillMaxWidth()
            .height(LiquidBarHeight)
    ) {
        val fullWidth = maxWidth
        // The lens is a wide capsule reaching past its tab on both sides, as on iOS (how far is set in
        // Settings), so the tabs sit inset by that overhang and the lens still fits inside the bar.
        val overhang = LensMaxOverhang * LocalLiquidSettings.current.lensWidth.coerceIn(0f, 1f)
        val rowPadding = overhang + LensInset
        val tabWidth = (fullWidth - rowPadding * 2) / tabs.size
        val tabWidthPx = with(LocalDensity.current) { tabWidth.toPx() }
        val barWidth = lerp(fullWidth, LiquidBarCompact, collapse)
        val barInteraction = remember { MutableInteractionSource() }
        Box(
            Modifier
                .width(barWidth)
                .fillMaxHeight()
                .liquidGlass(
                    RoundedCornerShape(percent = 50),
                    interactionSource = if (minimized) barInteraction else null,
                    pressScale = PRESS_CONTROL
                )
        ) {
            // The lens: stretched along its path by speed (and squashed across it), swelling out of
            // the bar while lifted.
            val speed = (abs(position.velocity) / 9f).coerceAtMost(1f)
            val raise = maxOf(lift, pop.value * 0.55f)
            Box(
                Modifier
                    .offset(x = LensInset + tabWidth * position.value)
                    .width(tabWidth + overhang * 2)
                    .fillMaxHeight()
                    .padding(vertical = LensInset)
                    .graphicsLayer {
                        alpha = 1f - collapse
                        scaleX = 1f + 0.36f * speed + 0.16f * raise
                        scaleY = 1f - 0.14f * speed + 0.40f * raise
                    }
                    .liquidGlass(RoundedCornerShape(percent = 50), tint = lensTint, clear = true)
            )
            Row(
                Modifier
                    .requiredWidth(fullWidth)
                    .fillMaxHeight()
                    .align(Alignment.CenterStart)
                    .graphicsLayer { alpha = (1f - collapse * 2.2f).coerceIn(0f, 1f) }
                    .padding(horizontal = rowPadding)
                    .pointerInput(tabs.size, rtl, tabWidthPx, minimized) {
                        if (minimized) return@pointerInput
                        var crossed = selected
                        detectHorizontalDragGestures(
                            onDragStart = {
                                dragging = true
                                crossed = position.value.roundToInt()
                            },
                            onDragEnd = {
                                dragging = false
                                val target = position.value.roundToInt().coerceIn(0, tabs.lastIndex)
                                if (target != selected) onSelect(tabs[target])
                                scope.launch { position.animateTo(target.toFloat(), LensSpring) }
                            },
                            onDragCancel = {
                                dragging = false
                                scope.launch { position.animateTo(selected.toFloat(), LensSpring) }
                            },
                            onHorizontalDrag = { change, amount ->
                                change.consume()
                                val step = (if (rtl) -amount else amount) / tabWidthPx
                                val next = (position.value + step).coerceIn(0f, tabs.lastIndex.toFloat())
                                scope.launch { position.snapTo(next) }
                                // A tick each time the lens passes over a tab, as on iOS.
                                val over = next.roundToInt()
                                if (over != crossed) {
                                    crossed = over
                                    if (look.motion) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                }
                            }
                        )
                    }
            ) {
                tabs.forEachIndexed { index, destination ->
                    val isSelected = index == selected
                    val highlight = (1f - abs(position.value - index)).coerceIn(0f, 1f)
                    val tint = lerp(MaterialTheme.colorScheme.onSurface, MaterialTheme.colorScheme.primary, highlight)
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = !minimized,
                                role = Role.Tab
                            ) {
                                if (!isSelected) {
                                    if (look.motion) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                    onSelect(destination)
                                }
                            }
                            .semantics { this.selected = isSelected },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painterResource(if (isSelected) destination.selectedIcon else destination.icon),
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer {
                                    val grow = 1f + 0.10f * highlight + 0.08f * raise * highlight
                                    scaleX = grow
                                    scaleY = grow
                                }
                        )
                        Text(
                            stringResource(destination.label),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = tint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
            // Shrunk: the current tab alone; a tap brings the whole bar back.
            if (collapse > 0f) {
                val destination = tabs[selected]
                val label = stringResource(destination.label)
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = ((collapse - 0.45f) / 0.55f).coerceIn(0f, 1f) }
                        .clickable(
                            interactionSource = barInteraction,
                            indication = null,
                            enabled = minimized,
                            role = Role.Button,
                            onClick = onExpand
                        )
                        .semantics { contentDescription = label },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painterResource(destination.selectedIcon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

/** How far the lens can reach past its tab on each side (at the widest setting), and the gap it keeps from the bar's edge. */
private val LensMaxOverhang = 24.dp
private val LensInset = 4.dp

/** Slightly under-damped, so the lens overshoots a touch and settles like liquid. */
private val LensSpring = spring<Float>(dampingRatio = 0.66f, stiffness = 340f)

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
            onOpenTasbih = { navController.navigate(TasbihRoute()) },
            onOpenSettings = { navController.navigate(SettingsRoute) },
            onOpenQuran = { surah, ayah -> navController.navigate(QuranReaderRoute(surah, ayah)) },
            onOpenHadith = { number -> navController.navigate(NawawiRoute(number)) },
            onOpenFacts = { navController.navigate(ProphetFactsRoute) },
            onOpenTracker = { navController.navigate(PrayerTrackerRoute) },
            onOpenCalendar = { navController.navigate(IslamicCalendarRoute) }
        )
    )
    trackerScreen(onBack = back)
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
        onOpenTasbih = { navController.navigate(it) },
        onOpenQuran = { surah, ayah -> navController.navigate(QuranReaderRoute(surah, ayah)) },
        onOpenTracker = { navController.navigate(PrayerTrackerRoute) },
        onOpenHadith = { navController.navigate(NawawiRoute(it)) },
        onBack = back
    )
    moreScreen(
        MoreActions(
            onOpenSettings = { navController.navigate(SettingsRoute) },
            onOpenQibla = { navController.navigate(QiblaRoute) },
            onOpenTimetable = { navController.navigate(TimetableRoute) },
            onOpenTasbih = { navController.navigate(TasbihRoute()) },
            onOpenTracker = { navController.navigate(PrayerTrackerRoute) },
            onOpenCalendar = { navController.navigate(IslamicCalendarRoute) },
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
    islamicCalendarScreen(onBack = back)
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
