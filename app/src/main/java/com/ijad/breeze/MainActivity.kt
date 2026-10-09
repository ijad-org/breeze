package com.ijad.breeze

import android.app.UiModeManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ijad.breeze.data.AppRepository
import com.ijad.breeze.data.ThemeMode
import com.ijad.breeze.ir.IrTransmitter
import com.ijad.breeze.timer.TimerScheduler
import com.ijad.breeze.ui.navigation.BreezeNavHost
import com.ijad.breeze.ui.theme.BreezeTheme
import com.ijad.breeze.ui.theme.surfaceColor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val repository = remember { AppRepository(context.applicationContext) }
            val irTransmitter = remember { IrTransmitter(context.applicationContext) }
            val timerScheduler = remember { TimerScheduler(context.applicationContext) }
            val scope = rememberCoroutineScope()

            val themeMode by repository.themeMode.collectAsStateWithLifecycle(initialValue = null)
            // Decide the start screen once, so returning users never flash the Welcome screen.
            val startOnRemote by produceState<Boolean?>(initialValue = null) {
                value = repository.onboardingDone.first() && repository.devices.first().isNotEmpty()
            }
            val reduceMotion = remember {
                Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
            }

            val mode = themeMode ?: ThemeMode.System
            val dark = when (mode) {
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
                ThemeMode.System -> isSystemInDarkTheme()
            }

            // Tell the system too, so the next launch's splash window uses the in-app theme.
            if (themeMode != null) LaunchedEffect(mode) { setSystemNightMode(mode) }

            // Status/nav bar icons follow the in-app theme, not just the system setting.
            DisposableEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }

            BreezeTheme(dark = dark, reduceMotion = reduceMotion) {
                Box(Modifier.fillMaxSize().background(surfaceColor())) {
                    val start = startOnRemote
                    if (themeMode != null && start != null) {
                        BreezeNavHost(
                            startOnRemote = start,
                            repository = repository,
                            irTransmitter = irTransmitter,
                            timerScheduler = timerScheduler,
                            themeMode = mode,
                            onThemeChange = { m -> scope.launch { repository.setThemeMode(m) } }
                        )
                    }
                }
            }
        }
    }

    /**
     * API 31+ keeps a per-app night mode that the system uses for the launch splash. Changing it
     * sends a uiMode config change, which the manifest handles in place, so Compose just recomposes.
     */
    private fun setSystemNightMode(mode: ThemeMode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        getSystemService(UiModeManager::class.java).setApplicationNightMode(
            when (mode) {
                ThemeMode.Light -> UiModeManager.MODE_NIGHT_NO
                ThemeMode.Dark -> UiModeManager.MODE_NIGHT_YES
                ThemeMode.System -> UiModeManager.MODE_NIGHT_AUTO
            }
        )
    }
}
