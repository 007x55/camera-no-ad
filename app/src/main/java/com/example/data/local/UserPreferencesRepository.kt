package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "clearcam_prefs")

data class AppPreferences(
  val isDarkTheme: Boolean = true,
  val defaultForceTcp: Boolean = true,
  val showHudOverlay: Boolean = true,
)

class UserPreferencesRepository(private val context: Context) {
  private object Keys {
    val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
    val DEFAULT_FORCE_TCP = booleanPreferencesKey("default_force_tcp")
    val SHOW_HUD_OVERLAY = booleanPreferencesKey("show_hud_overlay")
  }

  val preferencesFlow: Flow<AppPreferences> =
    context.dataStore.data.map { prefs ->
      AppPreferences(
        isDarkTheme = prefs[Keys.IS_DARK_THEME] ?: true,
        defaultForceTcp = prefs[Keys.DEFAULT_FORCE_TCP] ?: true,
        showHudOverlay = prefs[Keys.SHOW_HUD_OVERLAY] ?: true,
      )
    }

  suspend fun setDarkTheme(enabled: Boolean) {
    context.dataStore.edit { it[Keys.IS_DARK_THEME] = enabled }
  }

  suspend fun setDefaultForceTcp(enabled: Boolean) {
    context.dataStore.edit { it[Keys.DEFAULT_FORCE_TCP] = enabled }
  }

  suspend fun setShowHudOverlay(enabled: Boolean) {
    context.dataStore.edit { it[Keys.SHOW_HUD_OVERLAY] = enabled }
  }
}
