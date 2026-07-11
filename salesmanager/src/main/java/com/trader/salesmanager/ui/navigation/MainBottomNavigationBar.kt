package com.trader.salesmanager.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.trader.salesmanager.ui.theme.Emerald100
import com.trader.salesmanager.ui.theme.Emerald500
import com.trader.salesmanager.ui.theme.Emerald700
import com.trader.salesmanager.ui.theme.appColors

internal data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

internal val mainBottomNavItems = listOf(
    BottomNavItem(Screen.Home.route, "الرئيسية", Icons.Rounded.Home),
    BottomNavItem(Screen.CustomersList.route, "الزبائن", Icons.Rounded.People),
    BottomNavItem(Screen.Inventory.route, "المخزن", Icons.Rounded.Inventory2),
    BottomNavItem(Screen.Debts.route, "الديون", Icons.Rounded.Warning),
    BottomNavItem(Screen.Reports.route, "التقارير", Icons.Rounded.BarChart)
)

internal fun isMainBottomNavRoute(route: String?): Boolean =
    mainBottomNavItems.any { it.route == route }

internal fun NavHostController.navigateToMainTab(route: String, currentRoute: String?) {
    if (route == currentRoute) return

    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun MainBottomNavigationBar(
    currentRoute: String?,
    onTabSelected: (String) -> Unit
) {
    val colors = appColors
    val indicatorColor = if (colors.isDark) Emerald700 else Emerald100

    Column {
        HorizontalDivider(color = colors.divider)
        NavigationBar(
            containerColor = colors.cardBackground,
            tonalElevation = 2.dp
        ) {
            mainBottomNavItems.forEach { item ->
                val selected = currentRoute == item.route
                val contentColor by animateColorAsState(
                    targetValue = if (selected) Emerald500 else colors.textSubtle,
                    animationSpec = tween(240),
                    label = "bottom_navigation_color"
                )

                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        if (!selected) onTabSelected(item.route)
                    },
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = contentColor
                        )
                    },
                    label = {
                        Text(
                            text = item.label,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = contentColor
                        )
                    },
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Emerald500,
                        selectedTextColor = Emerald500,
                        unselectedIconColor = colors.textSubtle,
                        unselectedTextColor = colors.textSubtle,
                        indicatorColor = indicatorColor
                    )
                )
            }
        }
    }
}
