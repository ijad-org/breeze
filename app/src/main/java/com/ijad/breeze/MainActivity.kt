package com.ijad.breeze

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ijad.breeze.data.AppRepository
import com.ijad.breeze.data.ThemeMode
import com.ijad.breeze.ir.IrTransmitter
import com.ijad.breeze.ui.navigation.BreezeNavHost
import com.ijad.breeze.ui.theme.BreezeTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val repository = remember { AppRepository(context.applicationContext) }
            val irTransmitter = remember { IrTransmitter(context.applicationContext) }
            val themeMode by repository.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val scope = rememberCoroutineScope()

            BreezeTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BreezeNavHost(
                        repository = repository,
                        irTransmitter = irTransmitter,
                        themeMode = themeMode,
                        onThemeChange = { mode ->
                            scope.launch { repository.setThemeMode(mode) }
                        }
                    )
                }
            }
        }
    }
}
