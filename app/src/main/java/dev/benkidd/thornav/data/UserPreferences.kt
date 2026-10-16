package dev.benkidd.thornav.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserPreferences(private val context: Context) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(PREFS_NAME)

    private fun getBool(key: String, default: Boolean) = context.dataStore.data.map { prefs ->
        prefs[booleanPreferencesKey(key)] ?: default
    }

    suspend fun setBool(key: String, value: Boolean) = context.dataStore.updateData { prefs ->
        prefs.toMutablePreferences().also {
            it[booleanPreferencesKey(key)] = value
        }
    }

    val isScreenOnEnabled: Flow<Boolean> = getBool(PREF_SCREEN_ON, UserDefaults.SCREEN_ON)

    suspend fun setScreenOnEnabled(enabled: Boolean) = setBool(PREF_SCREEN_ON, enabled)

    val isUserUnlockEnabled: Flow<Boolean> = getBool(PREF_USER_UNLOCK, UserDefaults.USER_UNLOCK)

    suspend fun setUserUnlockEnabled(enabled: Boolean) = setBool(PREF_USER_UNLOCK, enabled)

    val isBootEnabled: Flow<Boolean> = getBool(PREF_BOOT, UserDefaults.BOOT)

    suspend fun setBootEnabled(enabled: Boolean) = setBool(PREF_BOOT, enabled)

    companion object {
        private const val PREFS_NAME = "thor_nav_fix_prefs"
        private const val PREF_SCREEN_ON = "pref_screen_on"
        private const val PREF_USER_UNLOCK = "pref_user_unlock"
        private const val PREF_BOOT = "pref_boot"
    }
}

object UserDefaults {
    const val SCREEN_ON = true
    const val USER_UNLOCK = true
    const val BOOT = true
}
