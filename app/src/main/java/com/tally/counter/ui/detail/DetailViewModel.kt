package com.tally.counter.ui.detail

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tally.counter.AppContainer
import com.tally.counter.data.db.HistoryEntity
import com.tally.counter.ui.AppVisual
import com.tally.counter.ui.visual
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetailUiState(
    val visual: AppVisual? = null,
    val count: Int = 0,
    val lastNotificationAt: Long = 0L,
    val history: List<HistoryEntity> = emptyList(),
    val storeText: Boolean = false,
    val countMessages: Boolean = true,
    val resetOnAppCancel: Boolean = true,
    val globalAppCancelEnabled: Boolean = true,
    val counted: Boolean = true,
    val canLaunch: Boolean = false
)

class DetailViewModel(
    private val container: AppContainer,
    val packageName: String
) : ViewModel() {

    private data class Static(val visual: AppVisual, val canLaunch: Boolean)

    private val static = flow {
        emit(
            Static(
                visual = container.appInfoProvider.visual(packageName),
                canLaunch = container.appInfoProvider.launchIntent(packageName) != null
            )
        )
    }.flowOn(Dispatchers.IO)

    val state: StateFlow<DetailUiState> = combine(
        static,
        container.countRepository.observeCount(packageName),
        container.countRepository.observeHistory(packageName),
        container.settingsRepository.settings
    ) { s, count, history, settings ->
        DetailUiState(
            visual = s.visual,
            count = count?.unreadCount ?: 0,
            lastNotificationAt = count?.lastNotificationAt ?: 0L,
            history = history,
            storeText = settings.storeText,
            countMessages = packageName !in settings.notificationModePackages,
            resetOnAppCancel = packageName !in settings.appCancelDisabledPackages,
            globalAppCancelEnabled = settings.resetOnAppCancel,
            counted = !container.appInfoProvider.isExcluded(packageName, settings),
            canLaunch = s.canLaunch
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    fun reset() {
        viewModelScope.launch { container.countRepository.resetApp(packageName) }
    }

    /** Opens the app and resets its count (same as tapping it in the widget). */
    fun openApp(context: Context): Boolean {
        val intent = container.appInfoProvider.launchIntent(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            reset()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun setCountMessages(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setCountMessages(packageName, enabled) }
    }

    fun setResetOnAppCancel(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setAppCancelReset(packageName, enabled) }
    }

    fun setCounted(counted: Boolean) {
        viewModelScope.launch {
            container.settingsRepository.setAppCounted(packageName, counted)
            if (!counted) container.countRepository.resetApp(packageName)
        }
    }
}
