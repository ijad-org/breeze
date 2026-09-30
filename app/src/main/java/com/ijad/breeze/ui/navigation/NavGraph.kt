package com.ijad.breeze.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ijad.breeze.data.AppRepository
import com.ijad.breeze.data.Brands
import com.ijad.breeze.data.ThemeMode
import com.ijad.breeze.ir.IrTransmitter
import com.ijad.breeze.ui.brand.BrandSelectScreen
import com.ijad.breeze.ui.pairing.PairingScreen
import com.ijad.breeze.ui.remote.RemoteScreen
import com.ijad.breeze.ui.settings.SettingsScreen
import com.ijad.breeze.ui.splash.SplashScreen
import kotlinx.coroutines.launch

object Routes {
    const val Splash = "splash"
    const val Brand = "brand"
    const val Pairing = "pairing/{brandId}"
    const val Remote = "remote"
    const val Settings = "settings"

    fun pairing(brandId: String) = "pairing/$brandId"
}

@Composable
fun BreezeNavHost(
    repository: AppRepository,
    irTransmitter: IrTransmitter,
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val devices by repository.devices.collectAsStateWithLifecycle(initialValue = emptyList())
    val activeId by repository.activeDeviceId.collectAsStateWithLifecycle(initialValue = null)
    val onboardingDone by repository.onboardingDone.collectAsStateWithLifecycle(initialValue = false)
    val tempAlerts by repository.tempAlerts.collectAsStateWithLifecycle(initialValue = true)
    val timerReminders by repository.timerReminders.collectAsStateWithLifecycle(initialValue = true)
    val ecoTips by repository.ecoTips.collectAsStateWithLifecycle(initialValue = false)

    val activeDevice = devices.firstOrNull { it.id == activeId } ?: devices.firstOrNull()

    // Returning users: skip splash once DataStore reports paired devices.
    LaunchedEffect(onboardingDone, devices) {
        if (onboardingDone && devices.isNotEmpty()) {
            val route = navController.currentBackStackEntry?.destination?.route
            if (route == Routes.Splash) {
                navController.navigate(Routes.Remote) {
                    popUpTo(Routes.Splash) { inclusive = true }
                }
            }
        }
    }

    NavHost(navController = navController, startDestination = Routes.Splash) {
        composable(Routes.Splash) {
            SplashScreen(
                onGetStarted = {
                    navController.navigate(Routes.Brand) {
                        popUpTo(Routes.Splash) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.Brand) {
            BrandSelectScreen(
                onContinue = { brand ->
                    navController.navigate(Routes.pairing(brand.id))
                },
                onToggleThemeHint = {
                    val next = when (themeMode) {
                        ThemeMode.Light -> ThemeMode.Dark
                        ThemeMode.Dark -> ThemeMode.System
                        ThemeMode.System -> ThemeMode.Light
                    }
                    onThemeChange(next)
                }
            )
        }
        composable(
            Routes.Pairing,
            arguments = listOf(navArgument("brandId") { type = NavType.StringType })
        ) { entry ->
            val brandId = entry.arguments?.getString("brandId").orEmpty()
            val brand = Brands.byId(brandId) ?: Brands.all.first()
            PairingScreen(
                brand = brand,
                repository = repository,
                irTransmitter = irTransmitter,
                onBack = { navController.popBackStack() },
                onPaired = { deviceId ->
                    scope.launch { repository.setActiveDevice(deviceId) }
                    navController.navigate(Routes.Remote) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(Routes.Remote) {
            RemoteScreen(
                devices = devices,
                activeDevice = activeDevice,
                irTransmitter = irTransmitter,
                onSelectDevice = { id -> scope.launch { repository.setActiveDevice(id) } },
                onOpenSettings = { navController.navigate(Routes.Settings) },
                onAddAc = { navController.navigate(Routes.Brand) }
            )
        }
        composable(Routes.Settings) {
            SettingsScreen(
                devices = devices,
                repository = repository,
                themeMode = themeMode,
                tempAlerts = tempAlerts,
                timerReminders = timerReminders,
                ecoTips = ecoTips,
                onBack = { navController.popBackStack() },
                onAddAc = { navController.navigate(Routes.Brand) },
                onThemeChange = onThemeChange,
                onTempAlerts = { v -> scope.launch { repository.setTempAlerts(v) } },
                onTimerReminders = { v -> scope.launch { repository.setTimerReminders(v) } },
                onEcoTips = { v -> scope.launch { repository.setEcoTips(v) } }
            )
        }
    }
}
