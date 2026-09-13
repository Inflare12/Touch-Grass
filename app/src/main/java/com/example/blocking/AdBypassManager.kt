package com.example.blocking

import android.content.Context

/** Persists short rewarded-ad grace periods for an app or the global strict lock. */
class AdBypassManager(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun grant(packageName: String, durationMillis: Long = DEFAULT_BYPASS_MILLIS) {
        if (packageName.isBlank()) return
        preferences.edit()
            .putLong(keyFor(packageName), System.currentTimeMillis() + durationMillis.coerceIn(1_000L, MAX_BYPASS_MILLIS))
            .apply()
    }

    fun grantGlobal(durationMillis: Long = DEFAULT_BYPASS_MILLIS) {
        preferences.edit()
            .putLong(GLOBAL_BYPASS_KEY, System.currentTimeMillis() + durationMillis.coerceIn(1_000L, MAX_BYPASS_MILLIS))
            .apply()
    }

    fun isActive(packageName: String, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (isGlobalActive(nowMillis)) return true
        if (packageName.isBlank()) return false
        val key = keyFor(packageName)
        val expiresAt = preferences.getLong(key, 0L)
        if (expiresAt <= nowMillis) {
            if (expiresAt != 0L) preferences.edit().remove(key).apply()
            return false
        }
        return true
    }

    fun isGlobalActive(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val expiresAt = preferences.getLong(GLOBAL_BYPASS_KEY, 0L)
        if (expiresAt <= nowMillis) {
            if (expiresAt != 0L) preferences.edit().remove(GLOBAL_BYPASS_KEY).apply()
            return false
        }
        return true
    }

    private fun keyFor(packageName: String) = "bypass_until_$packageName"

    private companion object {
        const val PREFS_NAME = "touch_grass_bypass"
        const val GLOBAL_BYPASS_KEY = "global_bypass_until"
        const val DEFAULT_BYPASS_MILLIS = 10 * 60 * 1000L
        const val MAX_BYPASS_MILLIS = 60 * 60 * 1000L
    }
}
