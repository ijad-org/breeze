package com.ijad.breeze.ui.navigation

import android.widget.Toast
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ijad.breeze.data.AppRepository
import com.ijad.breeze.data.Brands
import com.ijad.breeze.data.ThemeMode
import com.ijad.breeze.ir.IrTransmitter
import com.ijad.breeze.timer.TimerScheduler
import com.ijad.breeze.ui.brand.BrandSelectScreen
import com.ijad.breeze.ui.legal.LegalDoc
import com.ijad.breeze.ui.legal.LegalScreen
import com.ijad.breeze.ui.pairing.PairingScreen
import com.ijad.breeze.ui.remote.RemoteScreen
import com.ijad.breeze.ui.settings.SettingsScreen
import com.ijad.breeze.ui.splash.SplashScreen
import com.ijad.breeze.ui.theme.LocalReduceMotion
import com.ijad.breeze.ui.timer.TimerScreen
import kotlinx.coroutines.launch

object Routes {
    const val Splash = "splash"
    const val Brand = "brand"
    const val Pairing = "pairing/{brandId}"
    const val Remote = "remote"
    const val Timer = "timer"
    const val Settings = "settings"
    const val Privacy = "privacy"
    const val Terms = "terms"

    fun pairing(brandId: String) = "pairing/$brandId"
}

/** Prototype push transition: 0.3s cubic-bezier(0.35, 0, 0.2, 1). */
private val PushEasing = CubicBezierEasing(0.35f, 0f, 0.2f, 1f)
private const val PushMillis = 300

@Composable
fun BreezeNavHost(
    startOnRemote: Boolean,
    repository: AppRepository,
    irTransmitter: IrTransmitter,
    timerScheduler: TimerScheduler,
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val devices by repository.devices.collectAsStateWithLifecycle(initialValue = emptyList())
    val activeId by repository.activeDeviceId.collectAsStateWithLifecycle(initialValue = null)
    val timers by repository.timers.collectAsStateWithLifecycle(initialValue = emptyList())
    val tempAlerts by repository.tempAlerts.collectAsStateWithLifecycle(initialValue = true)
    val timerReminders by repository.timerReminders.collectAsStateWithLifecycle(initialValue = true)
    val ecoTips by repository.ecoTips.collectAsStateWithLifecycle(initialValue = false)

    val activeDevice = devices.firstOrNull { it.id == activeId } ?: devices.firstOrNull()
    val activeTimer = activeDevice?.let { d -> timers.firstOrNull { it.deviceId == d.id } }

    val reduceMotion = LocalReduceMotion.current

    fun cancelTimer(deviceId: String) {
        timerScheduler.cancel(deviceId)
        scope.launch { repository.removeTimer(deviceId) }
    }

    NavHost(
        navController = navController,
        startDestination = if (startOnRemote) Routes.Remote else Routes.Splash,
        // Forward: new screen slides in from the right over the static previous one.
        enterTransition = {
            if (reduceMotion) EnterTransition.None
            else slideInHorizontally(tween(PushMillis, easing = PushEasing)) { it }
        },
        exitTransition = { if (reduceMotion) ExitTransition.None else holdStill() },
        // Back: destination slides in from the left; the popped screen stays put and is
        // revealed away in sync (see [pushScreen]), matching the prototype's push-in-left.
        popEnterTransition = {
            if (reduceMotion) EnterTransition.None
            else slideInHorizontally(tween(PushMillis, easing = PushEasing)) { -it }
        },
        popExitTransition = { if (reduceMotion) ExitTransition.None else holdStill() }
    ) {
        screen(Routes.Splash, navController, reduceMotion) {
            SplashScreen(onGetStarted = { navController.navigate(Routes.Brand) })
        }
        screen(Routes.Brand, navController, reduceMotion) {
            BrandSelectScreen(
                onBack = { navController.backOr(Routes.Splash) },
                onContinue = { brand -> navController.navigate(Routes.pairing(brand.id)) }
            )
        }
        screen(
            Routes.Pairing,
            navController,
            reduceMotion,
            arguments = listOf(navArgument("brandId") { type = NavType.StringType })
        ) { entry ->
            val brandId = entry.arguments?.getString("brandId").orEmpty()
            val brand = Brands.byId(brandId) ?: Brands.all.first()
            PairingScreen(
                brand = brand,
                existingDevices = devices,
                repository = repository,
                irTransmitter = irTransmitter,
                onBack = { navController.popBackStack() },
                onPaired = {
                    navController.navigate(Routes.Remote) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        screen(Routes.Remote, navController, reduceMotion) {
            RemoteScreen(
                devices = devices,
                activeDevice = activeDevice,
                timer = activeTimer,
                tempAlerts = tempAlerts,
                ecoTips = ecoTips,
                irTransmitter = irTransmitter,
                onSelectDevice = { id -> scope.launch { repository.setActiveDevice(id) } },
                onStateChange = { id, state -> scope.launch { repository.updateDeviceState(id, state) } },
                onOpenSettings = { navController.navigate(Routes.Settings) },
                onOpenTimer = { navController.navigate(Routes.Timer) },
                onCancelTimer = { id -> cancelTimer(id) },
                onAddAc = { navController.navigate(Routes.Brand) }
            )
        }
        screen(Routes.Timer, navController, reduceMotion) {
            TimerScreen(
                device = activeDevice,
                existing = activeTimer,
                timerReminders = timerReminders,
                scheduler = timerScheduler,
                onBack = { navController.popBackStack() },
                onSet = { timer ->
                    scope.launch {
                        repository.setTimer(timer)
                        timerScheduler.schedule(timer)
                    }
                    Toast.makeText(context, "Timer set", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                },
                onCancel = { activeDevice?.let { cancelTimer(it.id) } }
            )
        }
        screen(Routes.Settings, navController, reduceMotion) {
            SettingsScreen(
                devices = devices,
                themeMode = themeMode,
                tempAlerts = tempAlerts,
                timerReminders = timerReminders,
                ecoTips = ecoTips,
                onBack = { navController.popBackStack() },
                onAddAc = { navController.navigate(Routes.Brand) },
                onRename = { id, name -> scope.launch { repository.updateDeviceName(id, name) } },
                onDelete = { id ->
                    timerScheduler.cancel(id)
                    scope.launch { repository.removeDevice(id) }
                },
                onThemeChange = onThemeChange,
                onTempAlerts = { v -> scope.launch { repository.setTempAlerts(v) } },
                onTimerReminders = { v -> scope.launch { repository.setTimerReminders(v) } },
                onEcoTips = { v -> scope.launch { repository.setEcoTips(v) } },
                onPrivacy = { navController.navigate(Routes.Privacy) },
                onTerms = { navController.navigate(Routes.Terms) }
            )
        }
        screen(Routes.Privacy, navController, reduceMotion) {
            LegalScreen(LegalDoc.Privacy, onBack = { navController.popBackStack() })
        }
        screen(Routes.Terms, navController, reduceMotion) {
            LegalScreen(LegalDoc.Terms, onBack = { navController.popBackStack() })
        }
    }
}

/** Keeps the outgoing screen fully visible for the length of the push. */
private fun holdStill(): ExitTransition = fadeOut(tween(PushMillis), targetAlpha = 0.999f)

/** [composable] wrapped so a popped screen is clipped away from the left as the destination slides in. */
private fun NavGraphBuilder.screen(
    route: String,
    navController: NavHostController,
    reduceMotion: Boolean,
    arguments: List<NamedNavArgument> = emptyList(),
    content: @Composable (NavBackStackEntry) -> Unit
) {
    composable(route, arguments = arguments) { entry ->
        val backStack by navController.currentBackStack.collectAsState()
        val popping = !reduceMotion &&
            transition.targetState == EnterExitState.PostExit &&
            backStack.none { it.id == entry.id }
        val reveal by transition.animateFloat(
            transitionSpec = { tween(PushMillis, easing = PushEasing) },
            label = "popReveal"
        ) { state -> if (state == EnterExitState.PostExit && popping) 1f else 0f }
        Box(
            Modifier
                .fillMaxSize()
                .drawWithContent {
                    if (reveal <= 0f) drawContent()
                    else clipRect(left = size.width * reveal) { this@drawWithContent.drawContent() }
                }
        ) {
            content(entry)
        }
    }
}

/** Pop if there's somewhere to go back to; otherwise replace the stack with [fallback]. */
private fun NavHostController.backOr(fallback: String) {
    if (previousBackStackEntry != null) {
        popBackStack()
    } else {
        navigate(fallback) {
            popUpTo(0) { inclusive = true }
        }
    }
}
