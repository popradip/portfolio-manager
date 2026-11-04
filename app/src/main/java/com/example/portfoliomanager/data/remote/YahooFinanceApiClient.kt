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
    val sparklinePoints: List<Double>,
    val currency: String = "USD"
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

    private val historicalFxCache = mutableMapOf<String, Map<String, Double>>()

    /**
     * Fetches live exchange rate from Yahoo Finance (e.g., USDEUR=X).
     * Returns how many [toCurrency] units 1 [fromCurrency] unit is worth.
     */
    open suspend fun fetchExchangeRate(fromCurrency: String, toCurrency: String = "EUR"): Double? = withContext(Dispatchers.IO) {
        val from = fromCurrency.trim().uppercase()
        val to = toCurrency.trim().uppercase()
        if (from == to) return@withContext 1.0

        val yahooPairSymbol = "${from}${to}=X"
        val data = fetchChartDataForSymbol(from, yahooPairSymbol)
        data?.currentPrice
    }

    /**
     * Fetches daily historical exchange rates map (date "YYYY-MM-DD" -> rate) for a currency pair from Yahoo Finance.
     */
    open suspend fun fetchHistoricalExchangeRates(fromCurrency: String, toCurrency: String = "EUR"): Map<String, Double> = withContext(Dispatchers.IO) {
        val from = fromCurrency.trim().uppercase()
        val to = toCurrency.trim().uppercase()
        if (from == to) return@withContext emptyMap()

        val pairKey = "${from}_${to}"
        synchronized(historicalFxCache) {
            historicalFxCache[pairKey]?.let { return@withContext it }
        }

        val yahooPairSymbol = "${from}${to}=X"
        val url = "https://query1.finance.yahoo.com/v8/finance/chart/$yahooPairSymbol?interval=1d&range=10y"
        AppLogger.d("YahooFinanceApiClient", "Fetching historical exchange rates for $yahooPairSymbol...")
        val request = Request.Builder().url(url).build()

        try {
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                AppLogger.d("YahooFinanceApiClient", "HTTP ${response.code} for $yahooPairSymbol history")
                return@withContext emptyMap()
            }

            val bodyString = response.body?.string() ?: return@withContext emptyMap()
            val jsonRoot = JSONObject(bodyString)
            val chartObj = jsonRoot.optJSONObject("chart") ?: return@withContext emptyMap()
            val resultArr = chartObj.optJSONArray("result") ?: return@withContext emptyMap()
            if (resultArr.length() == 0) return@withContext emptyMap()

            val resultObj = resultArr.getJSONObject(0)
            val timestampArr = resultObj.optJSONArray("timestamp") ?: return@withContext emptyMap()
            val indicatorsObj = resultObj.optJSONObject("indicators")
            val quoteArr = indicatorsObj?.optJSONArray("quote")
            val closeArr = quoteArr?.optJSONObject(0)?.optJSONArray("close") ?: return@withContext emptyMap()

            val ratesByDate = java.util.TreeMap<String, Double>()
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")

            for (i in 0 until timestampArr.length()) {
                if (!closeArr.isNull(i)) {
                    val rate = closeArr.optDouble(i, -1.0)
                    val ts = timestampArr.optLong(i, 0L)
                    if (rate > 0.0 && ts > 0L) {
                        val dateStr = sdf.format(java.util.Date(ts * 1000L))
                        ratesByDate[dateStr] = rate
                    }
                }
            }

            synchronized(historicalFxCache) {
                historicalFxCache[pairKey] = ratesByDate
            }
            AppLogger.d("YahooFinanceApiClient", "Loaded ${ratesByDate.size} historical FX rates for $yahooPairSymbol")
            ratesByDate
        } catch (e: Exception) {
            AppLogger.e("YahooFinanceApiClient", "Failed to fetch historical exchange rates for $yahooPairSymbol: ${e.message}", e)
            emptyMap()
        }
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

            var rawPrice = metaObj.optDouble("regularMarketPrice", 0.0)
            if (rawPrice <= 0.0) return null

            var rawCurrency = metaObj.optString("currency", "USD").uppercase()
            val isPence = rawCurrency == "GBP" || rawCurrency == "GBX"
            if (isPence) {
                rawCurrency = "GBP"
                rawPrice /= 100.0
            }

            val sparklinePoints = mutableListOf<Double>()
            val closePrices = mutableListOf<Double>()
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
                                val finalPrice = if (isPence) price / 100.0 else price
                                sparklinePoints.add(finalPrice)
                                closePrices.add(finalPrice)
                            }
                        }
                    }
                }
            }

            var rawPrevClose = metaObj.optDouble("previousClose", 0.0)
            if (isPence && rawPrevClose > 0.0) {
                rawPrevClose /= 100.0
            }

            // Fallback: If previousClose is missing in Yahoo meta or invalid, use second-to-last close price from daily series
            if (rawPrevClose <= 0.0) {
                if (closePrices.size >= 2) {
                    rawPrevClose = closePrices[closePrices.size - 2]
                } else if (closePrices.isNotEmpty()) {
                    rawPrevClose = closePrices.last()
                } else {
                    rawPrevClose = rawPrice
                }
            }

            val currentPrice = rawPrice
            val previousClose = rawPrevClose
            val dailyChange = currentPrice - previousClose
            val dailyChangePercentage = if (previousClose > 0.0) (dailyChange / previousClose) * 100.0 else 0.0

            AppLogger.d("YahooFinanceApiClient", "Successfully fetched $originalSymbol ($yahooSymbol): price=$currentPrice, prevClose=$previousClose, currency=$rawCurrency, change=$dailyChange")

            YahooMarketData(
                symbol = originalSymbol,
                yahooSymbol = yahooSymbol,
                currentPrice = currentPrice,
                previousClose = previousClose,
                dailyChange = dailyChange,
                dailyChangePercentage = dailyChangePercentage,
                sparklinePoints = if (sparklinePoints.isNotEmpty()) sparklinePoints else listOf(currentPrice),
                currency = rawCurrency
            )
        } catch (e: Exception) {
            AppLogger.e("YahooFinanceApiClient", "Failed to fetch market data for $yahooSymbol: ${e.message}", e)
            null
        }
    }

    private fun getGermanYahooTicker(base: String): String? {
        return when (base.uppercase()) {
            "AMZN" -> "AMZ.DE"
            "MSFT" -> "MSF.DE"
            "AAPL" -> "APC.DE"
            "TSLA" -> "TL0.DE"
            "GOOGL", "GOOG" -> "ABEA.DE"
            "META" -> "FB.DE"
            "NFLX" -> "NFC.DE"
            "INTC" -> "INL.DE"
            "PYPL" -> "2PP.DE"
            "DIS" -> "WD2.DE"
            "BA" -> "BCO.DE"
            "NVDA", "NVD" -> "NVD.DE"
            "MCD" -> "MDO.DE"
            "BRKB", "BRK.B" -> "381.DE"
            "AMD" -> "AMD.DE"
            else -> null
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
                when {
                    upper.endsWith(".US") -> {
                        val base = upper.removeSuffix(".US")
                        listOf(base, upper)
                    }
                    upper.endsWith(".UK") -> {
                        val base = upper.removeSuffix(".UK")
                        listOf("$base.L", base)
                    }
                    upper.endsWith(".PL") -> {
                        val base = upper.removeSuffix(".PL")
                        listOf("$base.WA", base)
                    }
                    upper.endsWith(".FI") -> {
                        val base = upper.removeSuffix(".FI")
                        listOf("$base.HE", base)
                    }
                    upper.endsWith(".FR") -> {
                        val base = upper.removeSuffix(".FR")
                        listOf("$base.PA", base)
                    }
                    upper.endsWith(".NL") -> {
                        val base = upper.removeSuffix(".NL")
                        listOf("$base.AS", base)
                    }
                    upper.endsWith(".DE") -> {
                        val base = upper.removeSuffix(".DE")
                        val germanTicker = getGermanYahooTicker(base)
                        val usMapped = if (base == "NVD") "NVDA" else base
                        listOfNotNull(upper, germanTicker, usMapped, base).distinct()
                    }
                    !upper.contains(".") -> {
                        listOf(upper, "$upper.DE", "$upper.L")
                    }
                    else -> listOf(upper)
                }
            }
        }
    }
}
