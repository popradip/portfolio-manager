package com.example.portfoliomanager.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class YahooMarketData(
    val symbol: String,
    val yahooSymbol: String,
    val currentPrice: Double,
    val previousClose: Double,
    val dailyChange: Double,
    val dailyChangePercentage: Double,
    val sparklinePoints: List<Double>
)

open class YahooFinanceApiClient(
    client: OkHttpClient? = null
) {

    private val okHttpClient: OkHttpClient by lazy {
        client ?: OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "application/json, text/plain, */*")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    /**
     * Resolves and fetches live market data for a given portfolio symbol from Yahoo Finance.
     */
    open suspend fun fetchMarketData(symbol: String): YahooMarketData? = withContext(Dispatchers.IO) {
        val candidates = getCandidateYahooSymbols(symbol)

        for (candidate in candidates) {
            val result = fetchChartDataForSymbol(symbol, candidate)
            if (result != null) {
                return@withContext result
            }
        }
        null
    }

    private fun fetchChartDataForSymbol(originalSymbol: String, yahooSymbol: String): YahooMarketData? {
        val url = "https://query1.finance.yahoo.com/v8/finance/chart/$yahooSymbol?interval=1d&range=1mo"
        AppLogger.d("YahooFinanceApiClient", "Fetching live market data for $originalSymbol ($yahooSymbol)...")
        val request = Request.Builder().url(url).build()

        return try {
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                AppLogger.d("YahooFinanceApiClient", "HTTP ${response.code} response for $yahooSymbol")
                return null
            }

            val bodyString = response.body?.string() ?: return null
            val jsonRoot = JSONObject(bodyString)
            val chartObj = jsonRoot.optJSONObject("chart") ?: return null
            val resultArr = chartObj.optJSONArray("result") ?: return null
            if (resultArr.length() == 0) return null

            val resultObj = resultArr.getJSONObject(0)
            val metaObj = resultObj.optJSONObject("meta") ?: return null

            val currentPrice = metaObj.optDouble("regularMarketPrice", 0.0)
            if (currentPrice <= 0.0) return null

            val previousClose = metaObj.optDouble("previousClose", metaObj.optDouble("chartPreviousClose", currentPrice))
            val dailyChange = currentPrice - previousClose
            val dailyChangePercentage = if (previousClose > 0.0) (dailyChange / previousClose) * 100.0 else 0.0

            AppLogger.d("YahooFinanceApiClient", "Successfully fetched $originalSymbol ($yahooSymbol): price=$currentPrice, change=$dailyChange")

            val sparklinePoints = mutableListOf<Double>()
            val indicatorsObj = resultObj.optJSONObject("indicators")
            val quoteArr = indicatorsObj?.optJSONArray("quote")
            if (quoteArr != null && quoteArr.length() > 0) {
                val quoteObj = quoteArr.getJSONObject(0)
                val closeArr = quoteObj.optJSONArray("close")
                if (closeArr != null) {
                    for (i in 0 until closeArr.length()) {
                        if (!closeArr.isNull(i)) {
                            val price = closeArr.optDouble(i, -1.0)
                            if (price > 0.0) {
                                sparklinePoints.add(price)
                            }
                        }
                    }
                }
            }

            YahooMarketData(
                symbol = originalSymbol,
                yahooSymbol = yahooSymbol,
                currentPrice = currentPrice,
                previousClose = previousClose,
                dailyChange = dailyChange,
                dailyChangePercentage = dailyChangePercentage,
                sparklinePoints = if (sparklinePoints.isNotEmpty()) sparklinePoints else listOf(currentPrice)
            )
        } catch (e: Exception) {
            AppLogger.e("YahooFinanceApiClient", "Failed to fetch market data for $yahooSymbol: ${e.message}", e)
            null
        }
    }

    private fun getCandidateYahooSymbols(symbol: String): List<String> {
        val upper = symbol.trim().uppercase()

        // Known exchange symbol suffixes for European ETFs and cryptos
        return when (upper) {
            "VWCE" -> listOf("VWCE.DE", "VWCE.MI", "VWRA.L", "VWCE")
            "IWDA" -> listOf("IWDA.AS", "IWDA.L", "EUNL.DE", "IWDA")
            "EUNL" -> listOf("EUNL.DE", "IWDA.AS", "EUNL")
            "SXR8" -> listOf("SXR8.DE", "CSPX.L", "SXR8")
            "CSPX" -> listOf("CSPX.L", "SXR8.DE", "CSPX")
            "BTC", "BITCOIN" -> listOf("BTC-USD", "BTC-EUR")
            "ETH", "ETHEREUM" -> listOf("ETH-USD", "ETH-EUR")
            else -> {
                if (!upper.contains(".")) {
                    listOf("$upper.DE", "$upper.L", upper)
                } else {
                    listOf(upper)
                }
            }
        }
    }
}
