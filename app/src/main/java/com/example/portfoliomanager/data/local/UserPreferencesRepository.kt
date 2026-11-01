package com.example.portfoliomanager.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class IbkrCredentials(
    val token: String = "",
    val queryId: String = ""
)

open class UserPreferencesRepository(private val context: Context? = null) {

    companion object {
        private val IBKR_TOKEN_KEY = stringPreferencesKey("ibkr_token")
        private val IBKR_QUERY_ID_KEY = stringPreferencesKey("ibkr_query_id")
        private val IMPORTED_CSV_KEY = stringPreferencesKey("imported_csv_content")
        private val MANUAL_TRADES_KEY = stringPreferencesKey("manual_trades_json")

        @Volatile
        private var INSTANCE: UserPreferencesRepository? = null

        fun getInstance(context: Context): UserPreferencesRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserPreferencesRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    open val credentialsFlow: Flow<IbkrCredentials> = context?.dataStore?.data?.map { preferences ->
        IbkrCredentials(
            token = preferences[IBKR_TOKEN_KEY] ?: "",
            queryId = preferences[IBKR_QUERY_ID_KEY] ?: ""
        )
    } ?: flowOf(IbkrCredentials("", ""))

    open val importedCsvFlow: Flow<String?> = context?.dataStore?.data?.map { preferences ->
        preferences[IMPORTED_CSV_KEY]?.ifBlank { null }
    } ?: flowOf(null)

    open val manualTradesFlow: Flow<String?> = context?.dataStore?.data?.map { preferences ->
        preferences[MANUAL_TRADES_KEY]?.ifBlank { null }
    } ?: flowOf(null)

    open suspend fun saveCredentials(token: String, queryId: String) {
        context?.dataStore?.edit { preferences ->
            preferences[IBKR_TOKEN_KEY] = token.trim()
            preferences[IBKR_QUERY_ID_KEY] = queryId.trim()
        }
    }

    open suspend fun clearCredentials() {
        context?.dataStore?.edit { preferences ->
            preferences.remove(IBKR_TOKEN_KEY)
            preferences.remove(IBKR_QUERY_ID_KEY)
        }
    }

    open suspend fun saveImportedCsv(csvContent: String) {
        context?.dataStore?.edit { preferences ->
            preferences[IMPORTED_CSV_KEY] = csvContent
        }
    }

    open suspend fun clearImportedCsv() {
        context?.dataStore?.edit { preferences ->
            preferences.remove(IMPORTED_CSV_KEY)
        }
    }

    open suspend fun saveManualTrades(json: String) {
        context?.dataStore?.edit { preferences ->
            preferences[MANUAL_TRADES_KEY] = json
        }
    }

    open suspend fun clearManualTrades() {
        context?.dataStore?.edit { preferences ->
            preferences.remove(MANUAL_TRADES_KEY)
        }
    }
}
