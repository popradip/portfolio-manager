package com.example.portfoliomanager.data.remote

import com.example.portfoliomanager.data.model.PortfolioData
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import java.io.IOException
import java.util.concurrent.TimeUnit

open class IbkrFlexApiClient(
    private val primaryBaseUrl: String = "https://ndcdyn.interactivebrokers.com/"
) {
    companion object {
        private const val TAG = "IbkrFlexApiClient"
        private val FALLBACK_BASE_URLS = listOf(
            "https://ndcdyn.interactivebrokers.com/",
            "https://flex.interactivebrokers.com/",
            "https://www.interactivebrokers.com/"
        )
    }

    private val xmlParser = IbkrFlexXmlParser()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Java/1.8.0")
                .header("Accept", "application/xml, text/xml, */*")
                .build()
            chain.proceed(request)
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private fun createService(baseUrl: String): IbkrFlexService {
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(okHttpClient)
            .build()
            .create(IbkrFlexService::class.java)
    }

    private var currentService: IbkrFlexService = createService(primaryBaseUrl)

    /**
     * Executes the full IBKR Flex Web Service workflow:
     * 1. Send request with Token and Query ID to get Reference Code (and optional direct statement URL).
     *    Primary endpoint is AccountManagement/FlexWebService/SendRequest on https://ndcdyn.interactivebrokers.com/.
     *    If HTTP 404 or connection error occurs, automatically retries on legacy endpoints/fallback URLs.
     * 2. Poll SendRequest if statement is generating (Code 1018 / Status Warn).
     * 3. Fetch statement XML with Reference Code and Token (using primary AccountManagement/FlexWebService/GetStatement or returned URL if available).
     * 4. Parse statement into PortfolioData domain model or throw descriptive exception.
     */
    open suspend fun fetchPortfolioData(token: String, queryId: String): PortfolioData {
        require(token.isNotBlank()) { "IBKR Flex Token must not be empty" }
        require(queryId.isNotBlank()) { "IBKR Query ID must not be empty" }

        val cleanToken = token.trim()
        val cleanQueryId = queryId.trim()

        AppLogger.d(TAG, "Starting IBKR Flex fetch for Query ID: $cleanQueryId")

        var referenceCode: String? = null
        var customStatementUrl: String? = null
        var usedLegacyPath = false
        var sendAttempts = 0
        val maxSendAttempts = 3

        while (referenceCode == null) {
            sendAttempts++
            AppLogger.d(TAG, "SendRequest attempt $sendAttempts of $maxSendAttempts")

            val (response, legacyUsed) = executeSendRequestWithFallback(cleanToken, cleanQueryId)
            usedLegacyPath = legacyUsed

            val httpCode = response.code()
            val xmlText = response.body()?.string() ?: ""

            AppLogger.d(TAG, "SendRequest HTTP $httpCode Response Body (usedLegacyPath=$usedLegacyPath):\n$xmlText")

            if (!response.isSuccessful) {
                val errorMsg = "IBKR Network Error: HTTP $httpCode ${response.message()}. Body: ${xmlText.take(300)}"
                AppLogger.e(TAG, errorMsg)
                throw IllegalStateException(errorMsg)
            }

            if (xmlText.isBlank()) {
                val msg = "Empty response body received from IBKR SendRequest (HTTP $httpCode)"
                AppLogger.e(TAG, msg)
                throw IllegalStateException(msg)
            }

            val sendResult = xmlParser.parseSendRequestResponse(xmlText)
            AppLogger.d(TAG, "Parsed SendRequestResult: status=${sendResult.status}, refCode=${sendResult.referenceCode}, url=${sendResult.url}, errorCode=${sendResult.errorCode}, errorMessage=${sendResult.errorMessage}")

            if (!sendResult.referenceCode.isNullOrEmpty()) {
                referenceCode = sendResult.referenceCode
                customStatementUrl = sendResult.url
                AppLogger.d(TAG, "SendRequest provided ReferenceCode: $referenceCode, StatementUrl: $customStatementUrl (errorCode=${sendResult.errorCode})")
            } else if (sendResult.errorCode == "1018" || sendResult.status.equals("Warn", ignoreCase = true) || sendResult.errorMessage?.contains("generating", ignoreCase = true) == true) {
                AppLogger.d(TAG, "SendRequest returned Code 1018 / Warn without ReferenceCode. Attempt $sendAttempts of $maxSendAttempts. Waiting 5s...")
                if (sendAttempts >= maxSendAttempts) {
                    val msg = formatIbkrErrorMessage("1018", sendResult.errorMessage)
                    AppLogger.e(TAG, "Max SendRequest attempts reached. $msg")
                    throw IllegalStateException(msg)
                }
                delay(5000)
            } else {
                val friendlyMsg = formatIbkrErrorMessage(sendResult.errorCode, sendResult.errorMessage ?: xmlText.take(300))
                AppLogger.e(TAG, "SendRequest failed: $friendlyMsg. Raw XML: $xmlText")
                throw IllegalStateException(friendlyMsg)
            }
        }

        val refCode = referenceCode

        var statementAttempts = 0
        val maxStatementAttempts = 12
        val pollDelayMs = 5000L

        while (true) {
            statementAttempts++
            AppLogger.d(TAG, "GetStatement attempt $statementAttempts of $maxStatementAttempts for RefCode: $refCode")

            val statementResponse = executeGetStatementWithFallback(refCode, cleanToken, customStatementUrl, usedLegacyPath)

            val httpCode = statementResponse.code()
            val statementXml = statementResponse.body()?.string() ?: ""

            AppLogger.d(TAG, "GetStatement HTTP $httpCode Response Body (length ${statementXml.length}):\n${statementXml.take(1000)}")

            if (!statementResponse.isSuccessful) {
                val errorMsg = "IBKR Network Error: HTTP $httpCode ${statementResponse.message()}. Body: ${statementXml.take(300)}"
                AppLogger.e(TAG, errorMsg)
                throw IllegalStateException(errorMsg)
            }

            if (statementXml.isBlank()) {
                val msg = "Empty response body received from IBKR GetStatement (HTTP $httpCode)"
                AppLogger.e(TAG, msg)
                throw IllegalStateException(msg)
            }

            try {
                val data = xmlParser.parseGetStatementResponse(statementXml)
                AppLogger.d(TAG, "Successfully parsed IBKR portfolio data: ${data.positions.size} positions, accountId: ${data.accountSummary.accountId}")
                return data
            } catch (e: IbkrApiException) {
                if (e.code == "1018" || e.message?.contains("generating", ignoreCase = true) == true) {
                    AppLogger.d(TAG, "GetStatement returned Code 1018 / Statement generating. Attempt $statementAttempts of $maxStatementAttempts. Waiting 5s...")
                    if (statementAttempts >= maxStatementAttempts) {
                        val msg = e.message ?: "IBKR Error 1018: Statement is generating, please wait a few seconds and try again."
                        AppLogger.e(TAG, "Max GetStatement attempts ($maxStatementAttempts) reached: $msg")
                        throw IllegalStateException(msg)
                    }
                    delay(pollDelayMs)
                } else {
                    AppLogger.e(TAG, "GetStatement IBKR API Error: ${e.message}. Raw XML: $statementXml")
                    throw IllegalStateException(e.message ?: "IBKR API Error")
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "GetStatement error: ${e.message}. Raw XML snippet: ${statementXml.take(500)}", e)
                throw e
            }
        }
    }

    private suspend fun executeSendRequestWithFallback(
        cleanToken: String,
        cleanQueryId: String
    ): Pair<Response<ResponseBody>, Boolean> {
        val urlsToTry = listOf(primaryBaseUrl) + FALLBACK_BASE_URLS.filter { !it.equals(primaryBaseUrl, ignoreCase = true) }
        var lastException: Exception? = null
        var isFallbackNeeded = false

        for (url in urlsToTry) {
            try {
                val service = if (url == primaryBaseUrl) currentService else createService(url)
                val response = service.sendRequest(token = cleanToken, queryId = cleanQueryId)
                if (response.isSuccessful) {
                    if (url != primaryBaseUrl) {
                        currentService = service
                    }
                    return Pair(response, false)
                } else if (response.code() == 404) {
                    AppLogger.d(TAG, "Primary SendRequest (AccountManagement/FlexWebService/SendRequest) returned HTTP 404 on $url. Retrying on legacy path...")
                    isFallbackNeeded = true
                    break
                } else {
                    return Pair(response, false)
                }
            } catch (e: IOException) {
                AppLogger.e(TAG, "Network IO exception on primary SendRequest ($url): ${e.message}. Trying next URL or legacy path...", e)
                lastException = e
                isFallbackNeeded = true
            }
        }

        if (isFallbackNeeded) {
            AppLogger.d(TAG, "Retrying SendRequest on legacy path (Universal/servlet/FlexStatementService.SendRequest)...")
            for (url in urlsToTry) {
                try {
                    val service = createService(url)
                    val response = service.sendRequestLegacy(token = cleanToken, queryId = cleanQueryId)
                    if (response.isSuccessful || response.code() != 404) {
                        currentService = service
                        return Pair(response, true)
                    }
                } catch (e: IOException) {
                    AppLogger.e(TAG, "Network IO exception on legacy path ($url): ${e.message}", e)
                    lastException = e
                }
            }
        }

        throw IllegalStateException("Network failure connecting to IBKR API: ${lastException?.message ?: "Connection failed"}")
    }

    private suspend fun executeGetStatementWithFallback(
        refCode: String,
        cleanToken: String,
        customStatementUrl: String?,
        usedLegacyPath: Boolean
    ): Response<ResponseBody> {
        return executeWithFallback { service ->
            val stmtUrl = customStatementUrl
            if (!stmtUrl.isNullOrBlank()) {
                service.getStatementByUrl(url = stmtUrl, referenceCode = refCode, token = cleanToken)
            } else if (usedLegacyPath) {
                service.getStatementLegacy(referenceCode = refCode, token = cleanToken)
            } else {
                service.getStatement(referenceCode = refCode, token = cleanToken)
            }
        }
    }

    private suspend fun executeWithFallback(
        call: suspend (IbkrFlexService) -> Response<ResponseBody>
    ): Response<ResponseBody> {
        val urlsToTry = listOf(primaryBaseUrl) + FALLBACK_BASE_URLS.filter { !it.equals(primaryBaseUrl, ignoreCase = true) }
        var lastException: Exception? = null

        for (url in urlsToTry) {
            try {
                val service = if (url == primaryBaseUrl) currentService else createService(url)
                val response = call(service)
                if (url != primaryBaseUrl) {
                    currentService = service
                }
                return response
            } catch (e: IOException) {
                AppLogger.e(TAG, "Network IO exception connecting to $url: ${e.message}. Trying next fallback domain...", e)
                lastException = e
            }
        }

        throw IllegalStateException("Network failure connecting to IBKR API: ${lastException?.message ?: "Connection failed"}")
    }
}
