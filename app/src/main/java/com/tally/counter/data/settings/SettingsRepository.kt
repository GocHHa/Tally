package com.tally.counter.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class ThemeMode { DARK, LIGHT, SYSTEM }

data class TallySettings(
    val theme: ThemeMode = ThemeMode.DARK,
    /** Store notification title/text in history. OFF by default for privacy. */
    val storeText: Boolean = false,
    /** Reset an app's count when you tap one of its notifications. */
    val resetOnClick: Boolean = true,
    /** Reset an app's count when the app removes its own notification (usually: you read it). */
    val resetOnAppCancel: Boolean = true,
    /** Reset an app's count when you open the app (needs Usage access). */
    val resetOnOpen: Boolean = true,
    /** Apps the user explicitly turned ON (overrides the default system-app exclusion). */
    val includedPackages: Set<String> = emptySet(),
    /** Apps the user explicitly turned OFF. */
    val excludedPackages: Set<String> = emptySet(),
    /** Apps that count 1 per notification instead of 1 per message. */
    val notificationModePackages: Set<String> = emptySet(),
    /** Apps for which "reset when the app clears its notification" is turned off. */
    val appCancelDisabledPackages: Set<String> = emptySet(),
    val onboardingDone: Boolean = false
)

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "tally_settings")

class SettingsRepository(context: Context) {

    private val store = context.applicationContext.settingsDataStore

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val STORE_TEXT = booleanPreferencesKey("store_text")
        val RESET_ON_CLICK = booleanPreferencesKey("reset_on_click")
        val RESET_ON_APP_CANCEL = booleanPreferencesKey("reset_on_app_cancel")
        val RESET_ON_OPEN = booleanPreferencesKey("reset_on_open")
        val INCLUDED = stringSetPreferencesKey("included_packages")
        val EXCLUDED = stringSetPreferencesKey("excluded_packages")
        val NOTIFICATION_MODE = stringSetPreferencesKey("notification_mode_packages")
        val APP_CANCEL_DISABLED = stringSetPreferencesKey("app_cancel_disabled_packages")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    }

    val settings: Flow<TallySettings> = store.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { p ->
            TallySettings(
                theme = p[Keys.THEME]?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } } ?: ThemeMode.DARK,
                storeText = p[Keys.STORE_TEXT] ?: false,
                resetOnClick = p[Keys.RESET_ON_CLICK] ?: true,
                resetOnAppCancel = p[Keys.RESET_ON_APP_CANCEL] ?: true,
                resetOnOpen = p[Keys.RESET_ON_OPEN] ?: true,
                includedPackages = p[Keys.INCLUDED] ?: emptySet(),
                excludedPackages = p[Keys.EXCLUDED] ?: emptySet(),
                notificationModePackages = p[Keys.NOTIFICATION_MODE] ?: emptySet(),
                appCancelDisabledPackages = p[Keys.APP_CANCEL_DISABLED] ?: emptySet(),
                onboardingDone = p[Keys.ONBOARDING_DONE] ?: false
            )
        }
        .distinctUntilChanged()

    suspend fun current(): TallySettings = settings.first()

    suspend fun setTheme(mode: ThemeMode) {
        store.edit { it[Keys.THEME] = mode.name }
    }

    suspend fun setStoreText(enabled: Boolean) {
        store.edit { it[Keys.STORE_TEXT] = enabled }
    }

    suspend fun setResetOnClick(enabled: Boolean) {
        store.edit { it[Keys.RESET_ON_CLICK] = enabled }
    }

    suspend fun setResetOnAppCancel(enabled: Boolean) {
        store.edit { it[Keys.RESET_ON_APP_CANCEL] = enabled }
    }

    suspend fun setResetOnOpen(enabled: Boolean) {
        store.edit { it[Keys.RESET_ON_OPEN] = enabled }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        store.edit { it[Keys.ONBOARDING_DONE] = done }
    }

    /** Turn counting on or off for an app. */
    suspend fun setAppCounted(packageName: String, counted: Boolean) {
        store.edit { p ->
            val included = (p[Keys.INCLUDED] ?: emptySet()).toMutableSet()
            val excluded = (p[Keys.EXCLUDED] ?: emptySet()).toMutableSet()
            if (counted) {
                excluded.remove(packageName)
                included.add(packageName)
            } else {
                included.remove(packageName)
                excluded.add(packageName)
            }
            p[Keys.INCLUDED] = included
            p[Keys.EXCLUDED] = excluded
        }
    }

    /** true = count individual messages (MessagingStyle), false = count 1 per notification. */
    suspend fun setCountMessages(packageName: String, countMessages: Boolean) {
        store.edit { p ->
            val set = (p[Keys.NOTIFICATION_MODE] ?: emptySet()).toMutableSet()
            if (countMessages) set.remove(packageName) else set.add(packageName)
            p[Keys.NOTIFICATION_MODE] = set
        }
    }

    suspend fun setAppCancelReset(packageName: String, enabled: Boolean) {
        store.edit { p ->
            val set = (p[Keys.APP_CANCEL_DISABLED] ?: emptySet()).toMutableSet()
            if (enabled) set.remove(packageName) else set.add(packageName)
            p[Keys.APP_CANCEL_DISABLED] = set
        }
    }
}
