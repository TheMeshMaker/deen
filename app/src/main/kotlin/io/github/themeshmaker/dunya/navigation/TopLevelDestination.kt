package io.github.themeshmaker.dunya.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.github.themeshmaker.dunya.R
import io.github.themeshmaker.dunya.core.designsystem.icon.DeenIcons
import io.github.themeshmaker.dunya.feature.adhkar.AdhkarRoute
import io.github.themeshmaker.dunya.feature.more.MoreRoute
import io.github.themeshmaker.dunya.feature.qibla.QiblaRoute
import io.github.themeshmaker.dunya.feature.tasbih.TasbihRoute
import io.github.themeshmaker.dunya.feature.today.TodayRoute
import kotlin.reflect.KClass

/** The five tabs of the bottom bar / navigation rail (docs/BLUEPRINT.md §16.2). */
enum class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
    @DrawableRes val selectedIcon: Int
) {
    TODAY(TodayRoute, TodayRoute::class, R.string.nav_today, DeenIcons.Today, DeenIcons.TodayFilled),
    ADHKAR(AdhkarRoute, AdhkarRoute::class, R.string.nav_adhkar, DeenIcons.Adhkar, DeenIcons.AdhkarFilled),
    TASBIH(TasbihRoute, TasbihRoute::class, R.string.nav_tasbih, DeenIcons.Tasbih, DeenIcons.TasbihFilled),
    QIBLA(QiblaRoute, QiblaRoute::class, R.string.nav_qibla, DeenIcons.Qibla, DeenIcons.QiblaFilled),
    MORE(MoreRoute, MoreRoute::class, R.string.nav_more, DeenIcons.More, DeenIcons.MoreFilled)
}
