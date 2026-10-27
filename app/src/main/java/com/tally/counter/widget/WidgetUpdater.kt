package com.tally.counter.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Throttled widget refresh: the first request updates immediately, further requests within
 * the next second are merged into a single trailing update.
 */
class WidgetUpdater(private val context: Context, scope: CoroutineScope) {

    private val requests = Channel<Unit>(Channel.CONFLATED)

    init {
        scope.launch {
            for (request in requests) {
                try {
                    TallyWidget().updateAll(context)
                } catch (e: Exception) {
                    Log.w("TallyWidget", "Widget update failed", e)
                }
                delay(THROTTLE_MS)
            }
        }
    }

    fun request() {
        requests.trySend(Unit)
    }

    private companion object {
        const val THROTTLE_MS = 1_000L
    }
}
