package com.tally.counter.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tally.counter.AppContainer
import com.tally.counter.service.ListenerControl
import com.tally.counter.service.ListenerStatus
import com.tally.counter.ui.AppVisual
import com.tally.counter.ui.visual
import com.tally.counter.util.PermissionChecker
import com.tally.counter.util.PermissionSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppRowUi(
    val visual: AppVisual,
    val count: Int,
    val lastNotificationAt: Long
)

data class HomeUiState(
    val loading: Boolean = true,
    val apps: List<AppRowUi> = emptyList(),
    val total: Int = 0,
    val permissions: PermissionSnapshot = PermissionSnapshot(),
    val listenerConnected: Boolean = false,
    val resetOnOpenEnabled: Boolean = true
)

class HomeViewModel(private val container: AppContainer) : ViewModel() {

    private val context = container.context
    private val permissions = MutableStateFlow(PermissionChecker.snapshot(context))

    private val rows = container.countRepository.observeActive()
        .map { list ->
            list.map { row ->
                AppRowUi(
                    visual = container.appInfoProvider.visual(row.packageName),
                    count = row.unreadCount,
                    lastNotificationAt = row.lastNotificationAt
                )
            }
        }
        .flowOn(Dispatchers.IO)

    val state: StateFlow<HomeUiState> = combine(
        rows,
        permissions,
        ListenerStatus.connected,
        container.settingsRepository.settings
    ) { apps, perms, connected, settings ->
        HomeUiState(
            loading = false,
            apps = apps,
            total = apps.sumOf { it.count },
            permissions = perms,
            listenerConnected = connected,
            resetOnOpenEnabled = settings.resetOnOpen
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    /** Called whenever Tally comes to the foreground. */
    fun onResume() {
        val snapshot = PermissionChecker.snapshot(context)
        permissions.value = snapshot
        if (snapshot.notificationAccess && !ListenerStatus.connected.value) {
            ListenerControl.requestRebind(context)
        }
        viewModelScope.launch { container.usageResetChecker.check(force = true) }
    }

    fun reset(packageName: String) {
        viewModelScope.launch { container.countRepository.resetApp(packageName) }
    }

    fun clearAll() {
        viewModelScope.launch { container.countRepository.clearAll() }
    }

    fun fixListener() {
        ListenerControl.forceRestart(context)
    }
}
