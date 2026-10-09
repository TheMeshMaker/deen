package io.github.themeshmaker.dunya.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.themeshmaker.dunya.feature.adhkar.adhkarScreen
import io.github.themeshmaker.dunya.feature.more.moreScreen
import io.github.themeshmaker.dunya.feature.qibla.qiblaScreen
import io.github.themeshmaker.dunya.feature.tasbih.tasbihScreen
import io.github.themeshmaker.dunya.feature.today.TodayRoute
import io.github.themeshmaker.dunya.feature.today.todayScreen
import io.github.themeshmaker.dunya.navigation.TopLevelDestination

/**
 * App shell. NavigationSuiteScaffold shows a bottom bar on phones and switches to a
 * navigation rail on unfolded foldables and tablets.
 */
@Composable
fun DeenApp(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { destination ->
                val selected = currentDestination?.hierarchy?.any { it.hasRoute(destination.routeClass) } == true
                item(
                    selected = selected,
                    onClick = { navController.navigateToTopLevel(destination) },
                    icon = {
                        Icon(
                            painter = painterResource(if (selected) destination.selectedIcon else destination.icon),
                            contentDescription = null
                        )
                    },
                    label = { Text(stringResource(destination.label)) }
                )
            }
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = TodayRoute,
            modifier = Modifier.fillMaxSize()
        ) {
            todayScreen()
            adhkarScreen()
            tasbihScreen()
            qiblaScreen()
            moreScreen()
        }
    }
}

/** Standard tab behaviour: one copy of each tab, and each tab keeps its own back stack. */
private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
