package com.tally.counter

import android.content.Context
import com.tally.counter.data.apps.AppInfoProvider
import com.tally.counter.data.db.TallyDatabase
import com.tally.counter.data.repo.CountRepository
import com.tally.counter.data.settings.SettingsRepository
import com.tally.counter.service.NotificationProcessor
import com.tally.counter.usage.UsageResetChecker
import com.tally.counter.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Holds every long-lived object in the app. Created once in [TallyApp.onCreate].
 */
class AppContainer(appContext: Context) {

    val context: Context = appContext.applicationContext

    /** Scope that lives as long as the process. */
    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: TallyDatabase = TallyDatabase.build(context)

    val settingsRepository = SettingsRepository(context)

    val appInfoProvider = AppInfoProvider(context)

    val widgetUpdater = WidgetUpdater(context, applicationScope)

    val countRepository = CountRepository(
        countDao = database.countDao(),
        keyDao = database.keyDao(),
        historyDao = database.historyDao(),
        widgetUpdater = widgetUpdater
    )

    val usageResetChecker = UsageResetChecker(context, countRepository, settingsRepository)

    val notificationProcessor = NotificationProcessor(
        ownPackage = context.packageName,
        countRepository = countRepository,
        settingsRepository = settingsRepository,
        appInfoProvider = appInfoProvider
    )
}
