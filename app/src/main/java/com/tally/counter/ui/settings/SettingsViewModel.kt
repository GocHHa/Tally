package com.tally.counter.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tally.counter.AppContainer
import com.tally.counter.data.apps.AppMeta
import com.tally.counter.data.settings.TallySettings
import com.tally.counter.data.settings.ThemeMode
import com.tally.counter.service.ListenerControl
import com.tally.counter.service.ListenerStatus
import com.tally.counter.util.PermissionChecker
import com.tally.counter.util.PermissionSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val settings: TallySettings = TallySettings(),
    val permissions: PermissionSnapshot = PermissionSnapshot(),
    val listenerConnected: Boolean = false
)

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    private val context = container.context
    private val permissions = MutableStateFlow(PermissionChecker.snapshot(context))

    val state: StateFlow<SettingsUiState> = combine(
        container.settingsRepository.settings,
        permissions,
        ListenerStatus.connected
    ) { settings, perms, connected ->
        SettingsUiState(settings, perms, connected)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun refreshPermissions() {
        val snapshot = PermissionChecker.snapshot(context)
        permissions.value = snapshot
        if (snapshot.notificationAccess && !ListenerStatus.connected.value) {
            ListenerControl.requestRebind(context)
        }
    }

    fun fixListener() = ListenerControl.forceRestart(context)

    fun setTheme(mode: ThemeMode) = runAction { container.settingsRepository.setTheme(mode) }

    fun setResetOnClick(enabled: Boolean) = runAction { container.settingsRepository.setResetOnClick(enabled) }

    fun setResetOnAppCancel(enabled: Boolean) = runAction { container.settingsRepository.setResetOnAppCancel(enabled) }

    fun setResetOnOpen(enabled: Boolean) = runAction {
        container.settingsRepository.setResetOnOpen(enabled)
        if (enabled) container.usageResetChecker.check(force = true)
    }

    fun setStoreText(enabled: Boolean) = runAction {
        container.settingsRepository.setStoreText(enabled)
        if (!enabled) container.countRepository.clearStoredText()
    }

    fun clearAll() = runAction { container.countRepository.clearAll() }

    fun completeOnboarding() = runAction { container.settingsRepository.setOnboardingDone(true) }

    private fun runAction(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}

data class AppOptionUi(
    val meta: AppMeta,
    val counted: Boolean,
    val countMessages: Boolean,
    val resetOnAppCancel: Boolean
)

data class AppsUiState(
    val loading: Boolean = true,
    val apps: List<AppOptionUi> = emptyList(),
    val query: String = "",
    val showSystem: Boolean = false,
    val hiddenSystemCount: Int = 0
)

/** Include/exclude apps and per-app options. */
class AppsViewModel(private val container: AppContainer) : ViewModel() {

    private val allApps = MutableStateFlow<List<AppMeta>?>(null)
    private val query = MutableStateFlow("")
    private val showSystem = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) {
                val provider = container.appInfoProvider
                val settings = container.settingsRepository.settings.first()
                val packages = LinkedHashSet<String>()
                provider.launcherApps().forEach { packages += it.packageName }
                packages += container.countRepository.knownPackages()
                packages += settings.includedPackages
                packages += settings.excludedPackages
                packages
                    .filter { it != container.context.packageName }
                    .map { provider.meta(it) }
                    .sortedBy { it.label.lowercase() }
            }
            allApps.value = list
        }
    }

    val state: StateFlow<AppsUiState> = combine(
        allApps,
        container.settingsRepository.settings,
        query,
        showSystem
    ) { apps, settings, q, system ->
        if (apps == null) {
            AppsUiState(loading = true, query = q, showSystem = system)
        } else {
            val provider = container.appInfoProvider
            val trimmed = q.trim()
            val visible = apps.filter { meta ->
                (system || !meta.defaultExcluded || meta.packageName in settings.includedPackages) &&
                    (trimmed.isEmpty() || meta.label.contains(trimmed, ignoreCase = true) ||
                        meta.packageName.contains(trimmed, ignoreCase = true))
            }
            AppsUiState(
                loading = false,
                apps = visible.map { meta ->
                    AppOptionUi(
                        meta = meta,
                        counted = !provider.isExcluded(meta.packageName, settings),
                        countMessages = meta.packageName !in settings.notificationModePackages,
                        resetOnAppCancel = meta.packageName !in settings.appCancelDisabledPackages
                    )
                },
                query = q,
                showSystem = system,
                hiddenSystemCount = if (system) 0 else apps.count { it.defaultExcluded && it.packageName !in settings.includedPackages }
            )
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppsUiState())

    fun setQuery(value: String) {
        query.value = value
    }

    fun setShowSystem(value: Boolean) {
        showSystem.value = value
    }

    fun setCounted(packageName: String, counted: Boolean) {
        viewModelScope.launch {
            container.settingsRepository.setAppCounted(packageName, counted)
            if (!counted) container.countRepository.resetApp(packageName)
        }
    }

    fun setCountMessages(packageName: String, enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setCountMessages(packageName, enabled) }
    }

    fun setResetOnAppCancel(packageName: String, enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setAppCancelReset(packageName, enabled) }
    }
}

/** Shared by the onboarding and permission status screens. */
class PermissionsViewModel(private val container: AppContainer) : ViewModel() {

    private val context = container.context
    private val permissions = MutableStateFlow(PermissionChecker.snapshot(context))

    val state: StateFlow<SettingsUiState> = combine(
        container.settingsRepository.settings,
        permissions,
        ListenerStatus.connected
    ) { settings, perms, connected ->
        SettingsUiState(settings, perms, connected)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState(permissions = permissions.value))

    fun refresh() {
        val snapshot = PermissionChecker.snapshot(context)
        permissions.value = snapshot
        if (snapshot.notificationAccess && !ListenerStatus.connected.value) {
            ListenerControl.requestRebind(context)
        }
        if (snapshot.usageAccess) {
            viewModelScope.launch { container.usageResetChecker.check(force = true) }
        }
    }

    fun fixListener() = ListenerControl.forceRestart(context)

    fun completeOnboarding() {
        viewModelScope.launch { container.settingsRepository.setOnboardingDone(true) }
    }
}
