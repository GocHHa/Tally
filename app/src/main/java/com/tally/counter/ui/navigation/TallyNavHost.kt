package com.tally.counter.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tally.counter.AppContainer
import com.tally.counter.ui.detail.DetailScreen
import com.tally.counter.ui.detail.DetailViewModel
import com.tally.counter.ui.factoryOf
import com.tally.counter.ui.home.HomeScreen
import com.tally.counter.ui.home.HomeViewModel
import com.tally.counter.ui.onboarding.OnboardingScreen
import com.tally.counter.ui.settings.AppsScreen
import com.tally.counter.ui.settings.AppsViewModel
import com.tally.counter.ui.settings.PermissionsScreen
import com.tally.counter.ui.settings.PermissionsViewModel
import com.tally.counter.ui.settings.SettingsScreen
import com.tally.counter.ui.settings.SettingsViewModel

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val APPS = "apps"
    const val PERMISSIONS = "permissions"
    private const val DETAIL_PREFIX = "detail/"

    fun detail(packageName: String) = DETAIL_PREFIX + packageName
    fun detailPackage(route: String): String? =
        if (route.startsWith(DETAIL_PREFIX)) route.removePrefix(DETAIL_PREFIX) else null
}

private val BackStackSaver = Saver<List<String>, String>(
    save = { it.joinToString("\n") },
    restore = { saved -> saved.split("\n").filter { it.isNotEmpty() } }
)

/**
 * Tiny back-stack navigator with spring-driven slide transitions. Avoids a navigation
 * dependency and keeps transitions fully custom.
 */
@Composable
fun TallyNavHost(container: AppContainer, onboardingDone: Boolean) {
    var stack by rememberSaveable(stateSaver = BackStackSaver) {
        mutableStateOf(listOf(if (onboardingDone) Routes.HOME else Routes.ONBOARDING))
    }

    fun push(route: String) {
        stack = stack + route
    }

    fun pop() {
        if (stack.size > 1) stack = stack.dropLast(1)
    }

    BackHandler(enabled = stack.size > 1) { pop() }

    AnimatedContent(
        targetState = stack,
        contentKey = { it.last() },
        transitionSpec = {
            val forward = targetState.size >= initialState.size
            val slideSpec = spring<androidx.compose.ui.unit.IntOffset>(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
            if (forward) {
                (slideInHorizontally(slideSpec) { full -> full / 3 } + fadeIn(spring(stiffness = Spring.StiffnessMediumLow)))
                    .togetherWith(slideOutHorizontally(slideSpec) { full -> -full / 8 } + fadeOut(spring(stiffness = Spring.StiffnessMedium)))
            } else {
                (slideInHorizontally(slideSpec) { full -> -full / 8 } + fadeIn(spring(stiffness = Spring.StiffnessMediumLow)))
                    .togetherWith(slideOutHorizontally(slideSpec) { full -> full / 3 } + fadeOut(spring(stiffness = Spring.StiffnessMedium)))
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        label = "nav"
    ) { targetStack ->
        val route = targetStack.last()
        Box(Modifier.fillMaxSize()) {
            val detailPackage = Routes.detailPackage(route)
            when {
                detailPackage != null -> {
                    val vm: DetailViewModel = viewModel(
                        key = "detail-$detailPackage",
                        factory = factoryOf { DetailViewModel(container, detailPackage) }
                    )
                    DetailScreen(viewModel = vm, onBack = ::pop)
                }

                route == Routes.ONBOARDING -> {
                    val vm: PermissionsViewModel = viewModel(
                        key = "permissions",
                        factory = factoryOf { PermissionsViewModel(container) }
                    )
                    OnboardingScreen(
                        viewModel = vm,
                        onFinished = { stack = listOf(Routes.HOME) }
                    )
                }

                route == Routes.SETTINGS -> {
                    val vm: SettingsViewModel = viewModel(factory = factoryOf { SettingsViewModel(container) })
                    SettingsScreen(
                        viewModel = vm,
                        onBack = ::pop,
                        onOpenApps = { push(Routes.APPS) },
                        onOpenPermissions = { push(Routes.PERMISSIONS) }
                    )
                }

                route == Routes.APPS -> {
                    val vm: AppsViewModel = viewModel(factory = factoryOf { AppsViewModel(container) })
                    AppsScreen(viewModel = vm, appInfoProvider = container.appInfoProvider, onBack = ::pop)
                }

                route == Routes.PERMISSIONS -> {
                    val vm: PermissionsViewModel = viewModel(
                        key = "permissions",
                        factory = factoryOf { PermissionsViewModel(container) }
                    )
                    PermissionsScreen(viewModel = vm, onBack = ::pop)
                }

                else -> {
                    val vm: HomeViewModel = viewModel(factory = factoryOf { HomeViewModel(container) })
                    HomeScreen(
                        viewModel = vm,
                        onOpenDetail = { push(Routes.detail(it)) },
                        onOpenSettings = { push(Routes.SETTINGS) },
                        onOpenPermissions = { push(Routes.PERMISSIONS) }
                    )
                }
            }
        }
    }
}
