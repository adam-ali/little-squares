package com.littlesquares.app

import android.app.Application
import com.littlesquares.app.data.Repo
import com.littlesquares.app.reminders.Effects
import com.littlesquares.app.reminders.Notifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

class LittleSquaresApp : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @OptIn(FlowPreview::class)
    override fun onCreate() {
        super.onCreate()
        Repo.init(this)
        Notifier.createChannels(this)
        // Any change in the app shows up in widgets, alarms and notifications.
        scope.launch {
            Repo.state.debounce(250).collect { Effects.refresh(this@LittleSquaresApp) }
        }
    }
}
