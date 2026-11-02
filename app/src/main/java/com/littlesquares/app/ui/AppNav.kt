package com.littlesquares.app.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.littlesquares.app.data.AppData
import com.littlesquares.app.ui.components.LsIcon
import com.littlesquares.app.ui.screens.ArchivedScreen
import com.littlesquares.app.ui.screens.CalendarScreen
import com.littlesquares.app.ui.screens.DetailScreen
import com.littlesquares.app.ui.screens.EditHabitScreen
import com.littlesquares.app.ui.screens.OnboardingScreen
import com.littlesquares.app.ui.screens.SettingsScreen
import com.littlesquares.app.ui.screens.TodayScreen
import com.littlesquares.app.ui.theme.LocalLs
import com.littlesquares.app.ui.theme.ts

/** Where the app should go when opened from a notification or widget. */
sealed interface DeepLink {
    data class Habit(val id: String) : DeepLink
    data object Calendar : DeepLink
}

private data class Tab(val route: String, val label: String, val on: ImageVector, val off: ImageVector)

private val TABS = listOf(
    Tab("today", "Home", Icons.Rounded.Home, Icons.Outlined.Home),
    Tab("calendar", "Calendar", Icons.Rounded.CalendarMonth, Icons.Outlined.CalendarMonth),
    Tab("settings", "Settings", Icons.Rounded.Settings, Icons.Outlined.Settings),
)
private val TAB_ROUTES = TABS.map { it.route }.toSet()

private fun NavBackStackEntry.isTab() = destination.route?.substringBefore('?') in TAB_ROUTES

/** Ignore a second back tap while a screen is already leaving. */
private fun NavController.back() {
    if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) popBackStack()
}

private fun NavController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.enter(pop: Boolean): EnterTransition =
    if (initialState.isTab() && targetState.isTab()) fadeIn(tween(180))
    else if (pop) fadeIn(tween(220))
    else fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 10 }

private fun AnimatedContentTransitionScope<NavBackStackEntry>.exit(pop: Boolean): ExitTransition =
    if (initialState.isTab() && targetState.isTab()) fadeOut(tween(120))
    else if (pop) fadeOut(tween(160)) + slideOutHorizontally(tween(220)) { it / 10 }
    else fadeOut(tween(160))

@Composable
fun AppNav(data: AppData, deepLink: DeepLink?, onOpened: () -> Unit) {
    val nav = rememberNavController()
    val start = remember { if (data.settings.onboarded || data.habits.isNotEmpty()) "today" else "onboarding" }
    val entry by nav.currentBackStackEntryAsState()
    val currentTab = entry?.destination?.route?.substringBefore('?')?.takeIf { it in TAB_ROUTES }

    LaunchedEffect(deepLink) {
        val link = deepLink ?: return@LaunchedEffect
        if (nav.currentDestination?.route != "onboarding") {
            when (link) {
                is DeepLink.Habit -> if (data.habit(link.id) != null) nav.navigate("detail/${link.id}")
                DeepLink.Calendar -> nav.switchTab("calendar")
            }
        }
        onOpened()
    }

    Column(Modifier.fillMaxSize().background(LocalLs.current.bg)) {
        Box(Modifier.weight(1f)) {
            NavHost(
                navController = nav,
                startDestination = start,
                enterTransition = { enter(false) },
                exitTransition = { exit(false) },
                popEnterTransition = { enter(true) },
                popExitTransition = { exit(true) },
            ) {
                composable("onboarding") {
                    OnboardingScreen(onDone = {
                        nav.navigate("today") { popUpTo("onboarding") { inclusive = true } }
                    })
                }
                composable("today") {
                    TodayScreen(data, onOpen = { nav.navigate("detail/$it") }, onAdd = { nav.navigate("edit") })
                }
                composable("calendar") { CalendarScreen(data) }
                composable("settings") { SettingsScreen(data, onArchived = { nav.navigate("archived") }) }
                composable(
                    "edit?id={id}",
                    arguments = listOf(navArgument("id") { type = NavType.StringType; nullable = true; defaultValue = null }),
                ) { e ->
                    EditHabitScreen(
                        data,
                        e.arguments?.getString("id"),
                        onClose = { nav.back() },
                        onGone = { nav.popBackStack("today", inclusive = false) },
                    )
                }
                composable("detail/{id}") { e ->
                    DetailScreen(
                        data,
                        e.arguments?.getString("id").orEmpty(),
                        onBack = { nav.back() },
                        onEdit = { nav.navigate("edit?id=${e.arguments?.getString("id")}") },
                    )
                }
                composable("archived") { ArchivedScreen(data, onBack = { nav.back() }) }
            }
        }
        if (currentTab != null) BottomBar(currentTab) { nav.switchTab(it) }
    }
}

@Composable
private fun BottomBar(selected: String, onSelect: (String) -> Unit) {
    val ls = LocalLs.current
    Row(
        Modifier
            .fillMaxWidth()
            .background(ls.surface2)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .height(72.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TABS.forEach { tab ->
            val on = tab.route == selected
            val pill by animateColorAsState(if (on) ls.sel else Color.Transparent, tween(200), label = "navPill")
            Column(
                Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Tab,
                        onClickLabel = tab.label,
                    ) { onSelect(tab.route) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    Modifier
                        .size(56.dp, 32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(pill),
                    contentAlignment = Alignment.Center,
                ) { LsIcon(if (on) tab.on else tab.off, if (on) ls.ink else ls.ink2, 24.dp) }
                Text(tab.label, style = ts(12, 800, if (on) ls.ink else ls.ink2))
            }
        }
    }
}
