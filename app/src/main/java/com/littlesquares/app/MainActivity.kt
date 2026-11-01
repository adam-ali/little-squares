package com.littlesquares.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.littlesquares.app.data.Repo
import com.littlesquares.app.reminders.Notifier
import com.littlesquares.app.ui.AppNav
import com.littlesquares.app.ui.theme.LittleSquaresTheme
import com.littlesquares.app.ui.theme.isDark

class MainActivity : ComponentActivity() {
    private var openHabit by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Repo.init(this)
        if (savedInstanceState == null) openHabit = intent?.getStringExtra(Notifier.EXTRA_HABIT)
        setContent {
            val data by Repo.state.collectAsState()
            val dark = isDark(data.settings.theme)
            DisposableEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            LittleSquaresTheme(data.settings.theme) {
                AppNav(data, openHabit) { openHabit = null }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(Notifier.EXTRA_HABIT)?.let { openHabit = it }
    }
}
