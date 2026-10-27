package com.tally.counter.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.tally.counter.appContainer
import com.tally.counter.data.settings.TallySettings
import com.tally.counter.ui.navigation.TallyNavHost
import com.tally.counter.ui.theme.TallyTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = appContainer

        // Opening Tally is one of the moments we check whether apps were opened since their last notification.
        lifecycleScope.launch { container.usageResetChecker.check(force = true) }

        setContent {
            val settings: TallySettings? by container.settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = null)
            val current = settings
            if (current == null) {
                // DataStore loads in a few ms; keep the dark window background meanwhile.
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0A0A0D))
                )
            } else {
                TallyTheme(themeMode = current.theme) {
                    TallyNavHost(container = container, onboardingDone = current.onboardingDone)
                }
            }
        }
    }
}
