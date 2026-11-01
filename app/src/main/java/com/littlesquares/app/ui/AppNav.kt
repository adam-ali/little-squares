package com.littlesquares.app.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.littlesquares.app.data.AppData
import com.littlesquares.app.ui.screens.ArchivedScreen
import com.littlesquares.app.ui.screens.CalendarScreen
import com.littlesquares.app.ui.screens.DetailScreen
import com.littlesquares.app.ui.screens.EditHabitScreen
import com.littlesquares.app.ui.screens.OnboardingScreen
import com.littlesquares.app.ui.screens.SettingsScreen
import com.littlesquares.app.ui.screens.TodayScreen
import com.littlesquares.app.ui.theme.LocalLs

/** Ignore a second back tap while a screen is already leaving. */
private fun NavController.back() {
    if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) popBackStack()
}

@Composable
fun AppNav(data: AppData, openHabit: String?, onOpened: () -> Unit) {
    val nav = rememberNavController()
    val start = remember { if (data.settings.onboarded || data.habits.isNotEmpty()) "today" else "onboarding" }

    LaunchedEffect(openHabit) {
        val id = openHabit ?: return@LaunchedEffect
        if (data.habit(id) != null && nav.currentDestination?.route != "onboarding") nav.navigate("detail/$id")
        onOpened()
    }

    Box(Modifier.fillMaxSize().background(LocalLs.current.bg)) {
        NavHost(
            navController = nav,
            startDestination = start,
            enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 10 } },
            exitTransition = { fadeOut(tween(160)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(160)) + slideOutHorizontally(tween(220)) { it / 10 } },
        ) {
            composable("onboarding") {
                OnboardingScreen(onDone = {
                    nav.navigate("today") { popUpTo("onboarding") { inclusive = true } }
                })
            }
            composable("today") {
                TodayScreen(
                    data,
                    onOpen = { nav.navigate("detail/$it") },
                    onAdd = { nav.navigate("edit") },
                    onCalendar = { nav.navigate("calendar") },
                    onSettings = { nav.navigate("settings") },
                    onMonth = { nav.navigate("calendar?month=true") },
                )
            }
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
            composable(
                "calendar?month={month}",
                arguments = listOf(navArgument("month") { type = NavType.BoolType; defaultValue = false }),
            ) { e -> CalendarScreen(data, onBack = { nav.back() }, startOnMonth = e.arguments?.getBoolean("month") == true) }
            composable("settings") {
                SettingsScreen(data, onBack = { nav.back() }, onArchived = { nav.navigate("archived") })
            }
            composable("archived") { ArchivedScreen(data, onBack = { nav.back() }) }
        }
    }
}
