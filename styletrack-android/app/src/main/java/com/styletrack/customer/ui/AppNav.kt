package com.styletrack.customer.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "Home", Icons.Filled.Home),
    Tab("browse", "Browse", Icons.Filled.Search),
    Tab("bookings", "Bookings", Icons.Filled.DateRange),
    Tab("rewards", "Rewards", Icons.Filled.Star),
    Tab("profile", "Profile", Icons.Filled.Person),
)

/** Route to the booking form, optionally pre-filled. */
fun bookRoute(serviceId: Long? = null, stylistId: Long? = null, home: Boolean = false) =
    "book?serviceId=${serviceId ?: -1L}&stylistId=${stylistId ?: -1L}&home=$home"

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val backStack by nav.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val onTab = tabs.any { t -> destination?.hierarchy?.any { it.route == t.route } == true }

    CompositionLocalProvider(LocalSnackbar provides snackbar) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                if (onTab) {
                    NavigationBar {
                        tabs.forEach { tab ->
                            NavigationBarItem(
                                selected = destination?.hierarchy?.any { it.route == tab.route } == true,
                                onClick = {
                                    nav.navigate(tab.route) {
                                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(tab.icon, contentDescription = null) },
                                label = { Text(tab.label) },
                            )
                        }
                    }
                }
            },
        ) { padding ->
            NavHost(nav, startDestination = "home", modifier = Modifier.padding(padding)) {
                composable("home") { HomeScreen(nav) }
                composable("browse") { BrowseScreen(nav) }
                composable("bookings") { BookingsScreen(nav) }
                composable("rewards") { RewardsScreen(nav) }
                composable("profile") { ProfileScreen() }

                composable("service/{id}", listOf(navArgument("id") { type = NavType.LongType })) {
                    ServiceDetailScreen(nav, it.arguments?.getLong("id") ?: -1L)
                }
                composable("stylist/{id}", listOf(navArgument("id") { type = NavType.LongType })) {
                    StylistDetailScreen(nav, it.arguments?.getLong("id") ?: -1L)
                }
                composable(
                    "book?serviceId={serviceId}&stylistId={stylistId}&home={home}",
                    listOf(
                        navArgument("serviceId") { type = NavType.LongType; defaultValue = -1L },
                        navArgument("stylistId") { type = NavType.LongType; defaultValue = -1L },
                        navArgument("home") { type = NavType.BoolType; defaultValue = false },
                    ),
                ) {
                    val args = it.arguments
                    BookScreen(
                        nav,
                        presetService = args?.getLong("serviceId")?.takeIf { id -> id > 0 },
                        presetStylist = args?.getLong("stylistId")?.takeIf { id -> id > 0 },
                        presetHome = args?.getBoolean("home") ?: false,
                    )
                }
                composable("booking/{id}", listOf(navArgument("id") { type = NavType.LongType })) {
                    BookingDetailScreen(nav, it.arguments?.getLong("id") ?: -1L)
                }
                composable("reschedule/{id}", listOf(navArgument("id") { type = NavType.LongType })) {
                    RescheduleScreen(nav, it.arguments?.getLong("id") ?: -1L)
                }
                composable("notifications") { NotificationsScreen(nav) }
            }
        }
    }
}
