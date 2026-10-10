package io.github.themeshmaker.dunya.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.themeshmaker.dunya.feature.adhkar.adhkarScreen
import io.github.themeshmaker.dunya.feature.more.moreScreen
import io.github.themeshmaker.dunya.feature.onboarding.LocationPickerRoute
import io.github.themeshmaker.dunya.feature.onboarding.MethodPickerRoute
import io.github.themeshmaker.dunya.feature.onboarding.OnboardingRoute
import io.github.themeshmaker.dunya.feature.onboarding.locationPickerScreen
import io.github.themeshmaker.dunya.feature.onboarding.methodPickerScreen
import io.github.themeshmaker.dunya.feature.onboarding.onboardingScreen
import io.github.themeshmaker.dunya.feature.qibla.qiblaScreen
import io.github.themeshmaker.dunya.feature.reliability.ReliabilityRoute
import io.github.themeshmaker.dunya.feature.reliability.reliabilityScreen
import io.github.themeshmaker.dunya.feature.tasbih.tasbihScreen
import io.github.themeshmaker.dunya.feature.today.TodayRoute
import io.github.themeshmaker.dunya.feature.today.todayScreen
import io.github.themeshmaker.dunya.navigation.TopLevelDestination

/** Full-screen flows that hide the bottom bar / rail. */
private val fullScreenRoutes = listOf(OnboardingRoute::class, LocationPickerRoute::class, MethodPickerRoute::class, ReliabilityRoute::class)

/**
 * App shell. NavigationSuiteScaffold shows a bottom bar on phones and switches to a
 * navigation rail on unfolded foldables and tablets.
 */
@Composable
fun DeenApp(startDestination: Any, navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showNavigation = currentDestination?.let { d -> fullScreenRoutes.none { d.hasRoute(it) } } ?: (startDestination != OnboardingRoute)

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
            navigationRailContainerColor = MaterialTheme.colorScheme.surface
        ),
        layoutType = if (showNavigation) {
            NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfoV2())
        } else {
            NavigationSuiteType.None
        },
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { destination ->
                val selected = currentDestination.isOn(destination)
                item(
                    selected = selected,
                    onClick = { navController.navigateToTopLevel(destination) },
                    colors = itemColors,
                    icon = {
                        Icon(
                            painter = painterResource(if (selected) destination.selectedIcon else destination.icon),
                            contentDescription = null
                        )
                    },
                    label = {
                        Text(stringResource(destination.label), fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                    }
                )
            }
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize()
        ) {
            onboardingScreen(
                onFinished = {
                    // Straight on to adhan setup (notifications, exact alarms, battery), then Today.
                    navController.navigate(ReliabilityRoute(setup = true)) { popUpTo<OnboardingRoute> { inclusive = true } }
                }
            )
            locationPickerScreen(onDone = { navController.popBackStack() })
            methodPickerScreen(onDone = { navController.popBackStack() })
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
                onChangeLocation = { navController.navigate(LocationPickerRoute) },
                onChangeMethod = { navController.navigate(MethodPickerRoute) },
                onOpenReliability = { navController.navigate(ReliabilityRoute()) }
            )
            adhkarScreen()
            tasbihScreen()
            qiblaScreen()
            moreScreen(onOpenReliability = { navController.navigate(ReliabilityRoute()) })
        }
    }
}

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
