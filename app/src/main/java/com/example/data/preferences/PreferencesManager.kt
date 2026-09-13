package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "touch_grass_prefs")

data class AppSettings(
    val globalDailyLimitMinutes: Int = 30,
    val holdDurationSeconds: Int = 3,
    val sensitivity: String = "NORMAL",
    val funnyMessagesEnabled: Boolean = true,
    val soundVibrationEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val onboardingCompleted: Boolean = false,
    val interventionOverlayEnabled: Boolean = true,
    val testAdMode: Boolean = true,
    val strictLockEnabled: Boolean = true,
    val globalLockActive: Boolean = false
)

class PreferencesManager(private val context: Context) {

    private object Keys {
        val GLOBAL_LIMIT_MINUTES = intPreferencesKey("global_limit_minutes")
        val HOLD_DURATION_SECONDS = intPreferencesKey("hold_duration_seconds")
        val SENSITIVITY = stringPreferencesKey("sensitivity")
        val FUNNY_MESSAGES_ENABLED = booleanPreferencesKey("funny_messages_enabled")
        val SOUND_VIBRATION_ENABLED = booleanPreferencesKey("sound_vibration_enabled")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val INTERVENTION_OVERLAY_ENABLED = booleanPreferencesKey("intervention_overlay_enabled")
        val LAST_INTERVENTION_TIME = longPreferencesKey("last_intervention_time")
        val TEST_AD_MODE = booleanPreferencesKey("test_ad_mode")
        val STRICT_LOCK_ENABLED = booleanPreferencesKey("strict_lock_enabled")
        val GLOBAL_LOCK_ACTIVE = booleanPreferencesKey("global_lock_active")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            globalDailyLimitMinutes = prefs[Keys.GLOBAL_LIMIT_MINUTES] ?: 30,
            holdDurationSeconds = prefs[Keys.HOLD_DURATION_SECONDS] ?: 3,
            sensitivity = prefs[Keys.SENSITIVITY] ?: "NORMAL",
            funnyMessagesEnabled = prefs[Keys.FUNNY_MESSAGES_ENABLED] ?: true,
            soundVibrationEnabled = prefs[Keys.SOUND_VIBRATION_ENABLED] ?: true,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            interventionOverlayEnabled = prefs[Keys.INTERVENTION_OVERLAY_ENABLED] ?: true,
            testAdMode = prefs[Keys.TEST_AD_MODE] ?: true,
            strictLockEnabled = prefs[Keys.STRICT_LOCK_ENABLED] ?: true,
            globalLockActive = prefs[Keys.GLOBAL_LOCK_ACTIVE] ?: false
        )
    }

    suspend fun setOnboardingCompleted(completed: Boolean) { context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed } }
    suspend fun setGlobalDailyLimitMinutes(minutes: Int) { context.dataStore.edit { it[Keys.GLOBAL_LIMIT_MINUTES] = minutes } }
    suspend fun setHoldDurationSeconds(seconds: Int) { context.dataStore.edit { it[Keys.HOLD_DURATION_SECONDS] = seconds } }
    suspend fun setSensitivity(sensitivity: String) { context.dataStore.edit { it[Keys.SENSITIVITY] = sensitivity } }
    suspend fun setFunnyMessagesEnabled(enabled: Boolean) { context.dataStore.edit { it[Keys.FUNNY_MESSAGES_ENABLED] = enabled } }
    suspend fun setSoundVibrationEnabled(enabled: Boolean) { context.dataStore.edit { it[Keys.SOUND_VIBRATION_ENABLED] = enabled } }
    suspend fun setNotificationsEnabled(enabled: Boolean) { context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled } }
    suspend fun setInterventionOverlayEnabled(enabled: Boolean) { context.dataStore.edit { it[Keys.INTERVENTION_OVERLAY_ENABLED] = enabled } }
    suspend fun setLastInterventionTime(time: Long) { context.dataStore.edit { it[Keys.LAST_INTERVENTION_TIME] = time } }

    suspend fun setStrictLockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.STRICT_LOCK_ENABLED] = enabled; if (!enabled) it[Keys.GLOBAL_LOCK_ACTIVE] = false }
    }

    suspend fun setGlobalLockActive(active: Boolean) { context.dataStore.edit { it[Keys.GLOBAL_LOCK_ACTIVE] = active } }
    suspend fun clearGlobalLock() = setGlobalLockActive(false)

    suspend fun resetAllSettings() {
        context.dataStore.edit {
            it[Keys.GLOBAL_LIMIT_MINUTES] = 30
            it[Keys.HOLD_DURATION_SECONDS] = 3
            it[Keys.SENSITIVITY] = "NORMAL"
            it[Keys.FUNNY_MESSAGES_ENABLED] = true
            it[Keys.SOUND_VIBRATION_ENABLED] = true
            it[Keys.NOTIFICATIONS_ENABLED] = true
            it[Keys.INTERVENTION_OVERLAY_ENABLED] = true
            it[Keys.STRICT_LOCK_ENABLED] = true
            it[Keys.GLOBAL_LOCK_ACTIVE] = false
        }
    }
}
