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
        private val TICKER_OVERRIDES_KEY = stringPreferencesKey("ticker_overrides_json")

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

    open val tickerOverridesFlow: Flow<Map<String, String>> = context?.dataStore?.data?.map { preferences ->
        parseTickerOverrides(preferences[TICKER_OVERRIDES_KEY])
    } ?: flowOf(emptyMap())

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

    open suspend fun saveTickerOverride(originalSymbol: String, alternativeTicker: String) {
        val upperOriginal = originalSymbol.trim().uppercase()
        val upperAlt = alternativeTicker.trim().uppercase()
        context?.dataStore?.edit { preferences ->
            val currentMap = parseTickerOverrides(preferences[TICKER_OVERRIDES_KEY]).toMutableMap()
            if (upperAlt.isBlank() || upperAlt == upperOriginal) {
                currentMap.remove(upperOriginal)
            } else {
                currentMap[upperOriginal] = upperAlt
            }
            preferences[TICKER_OVERRIDES_KEY] = serializeTickerOverrides(currentMap)
        }
    }

    open suspend fun clearTickerOverrides() {
        context?.dataStore?.edit { preferences ->
            preferences.remove(TICKER_OVERRIDES_KEY)
        }
    }

    private fun parseTickerOverrides(jsonString: String?): Map<String, String> {
        if (jsonString.isNullOrBlank()) return emptyMap()
        val map = mutableMapOf<String, String>()
        try {
            val jsonObject = org.json.JSONObject(jsonString)
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = jsonObject.optString(key)
                if (value.isNotBlank()) {
                    map[key.uppercase()] = value.uppercase()
                }
            }
        } catch (_: Exception) {
        }
        return map
    }

    private fun serializeTickerOverrides(map: Map<String, String>): String {
        val jsonObject = org.json.JSONObject()
        for ((key, value) in map) {
            jsonObject.put(key, value)
        }
        return jsonObject.toString()
    }
}
