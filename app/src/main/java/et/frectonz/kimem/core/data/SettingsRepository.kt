package et.frectonz.kimem.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import et.frectonz.kimem.core.model.Settings
import et.frectonz.kimem.core.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val store: DataStore<Preferences>) {
    private val router = stringPreferencesKey("router")
    private val username = stringPreferencesKey("username")
    private val password = stringPreferencesKey("password")
    private val theme = stringPreferencesKey("theme")
    private val lastRecipientKey = stringPreferencesKey("last_recipient")

    val settings: Flow<Settings> = store.data.map { prefs ->
        val defaults = Settings()
        Settings(
            router = prefs[router] ?: defaults.router,
            username = prefs[username] ?: defaults.username,
            password = prefs[password] ?: defaults.password,
            theme = prefs[theme]?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } } ?: defaults.theme,
        )
    }

    val lastRecipient: Flow<String> = store.data.map { it[lastRecipientKey] ?: "" }

    suspend fun save(updated: Settings) {
        store.edit {
            it[router] = updated.router
            it[username] = updated.username
            it[password] = updated.password
            it[theme] = updated.theme.name
        }
    }

    suspend fun rememberRecipient(number: String) {
        store.edit { it[lastRecipientKey] = number }
    }
}
