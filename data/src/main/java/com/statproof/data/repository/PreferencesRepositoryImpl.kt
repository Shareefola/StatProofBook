package com.statproof.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.statproof.domain.repository.PreferencesRepository
import com.statproof.proofengine.models.ProofMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "statproof_preferences"
)

@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : PreferencesRepository {

    private val dataStore = context.dataStore

    private object Keys {
        val PREFERRED_MODE = stringPreferencesKey("preferred_proof_mode")
        val DARK_MODE_OVERRIDE = stringPreferencesKey("dark_mode_override") // "true", "false", or "system"
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val DYNAMIC_COLOR_ENABLED = booleanPreferencesKey("dynamic_color_enabled")
        val SHOW_HINTS_BY_DEFAULT = booleanPreferencesKey("show_hints_by_default")
    }

    override fun getPreferredMode(): Flow<ProofMode> = dataStore.data.map { prefs ->
        val name = prefs[Keys.PREFERRED_MODE] ?: ProofMode.STANDARD.name
        runCatching { ProofMode.valueOf(name) }.getOrDefault(ProofMode.STANDARD)
    }

    override suspend fun setPreferredMode(mode: ProofMode) {
        dataStore.edit { prefs -> prefs[Keys.PREFERRED_MODE] = mode.name }
    }

    override fun getDarkModeOverride(): Flow<Boolean?> = dataStore.data.map { prefs ->
        when (prefs[Keys.DARK_MODE_OVERRIDE]) {
            "true" -> true
            "false" -> false
            else -> null
        }
    }

    override suspend fun setDarkModeOverride(isDark: Boolean?) {
        dataStore.edit { prefs ->
            prefs[Keys.DARK_MODE_OVERRIDE] = isDark?.toString() ?: "system"
        }
    }

    override fun getFontScale(): Flow<Float> = dataStore.data.map { prefs ->
        prefs[Keys.FONT_SCALE] ?: DEFAULT_FONT_SCALE
    }

    override suspend fun setFontScale(scale: Float) {
        val clamped = scale.coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE)
        dataStore.edit { prefs -> prefs[Keys.FONT_SCALE] = clamped }
    }

    override fun getDynamicColorEnabled(): Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.DYNAMIC_COLOR_ENABLED] ?: true
    }

    override suspend fun setDynamicColorEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.DYNAMIC_COLOR_ENABLED] = enabled }
    }

    override fun getShowHintsByDefault(): Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.SHOW_HINTS_BY_DEFAULT] ?: false
    }

    override suspend fun setShowHintsByDefault(show: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.SHOW_HINTS_BY_DEFAULT] = show }
    }

    companion object {
        const val DEFAULT_FONT_SCALE = 1.0f
        const val MIN_FONT_SCALE = 0.8f
        const val MAX_FONT_SCALE = 1.6f
    }
}
