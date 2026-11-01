package com.example.portfoliomanager.data.parser

import com.example.portfoliomanager.data.model.AccountSummary
import com.example.portfoliomanager.data.model.AssetCategory
import com.example.portfoliomanager.data.model.Balances
import com.example.portfoliomanager.data.model.GeographyAllocation
import com.example.portfoliomanager.data.model.MarginRequirements
import com.example.portfoliomanager.data.model.PortfolioData
import com.example.portfoliomanager.data.model.Position
import com.example.portfoliomanager.data.model.SectorAllocation
import com.example.portfoliomanager.data.model.Trade
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

private data class ClosedPositionInfo(
    val ticker: String,
    val instrumentName: String,
    val categoryStr: String,
    var totalProfit: Double = 0.0,
    var totalPurchaseVal: Double = 0.0,
    var totalSaleVal: Double = 0.0,
    val trades: MutableList<Trade> = mutableListOf()
)

/**
 * Parser for XTB statement exports in both XLSX (OpenXML format) and CSV formats.
 */
class XtbCsvParser : BrokerCsvParser {

    override val brokerName: String = "XTB"

    override fun canParse(csvContent: String): Boolean {
        if (csvContent.isBlank()) return false
        val trimmed = csvContent.trim()

        // 1. Check if XLSX format (Base64 or Zip PK header)
        val xlsxBytes = extractXlsxBytes(trimmed)
        if (xlsxBytes != null) {
            return canParseXlsx(xlsxBytes)
        }

        // 2. Check CSV format
        val firstLines = trimmed.lines().take(10).joinToString("\n").lowercase()
        return firstLines.contains("xtb") ||
                (firstLines.contains("position") && firstLines.contains("open price") && firstLines.contains("close price")) ||
                (firstLines.contains("ticket") && firstLines.contains("profit") && firstLines.contains("symbol")) ||
                (firstLines.contains("instrument") && firstLines.contains("volume") && firstLines.contains("profit"))
    }

    override fun parse(csvContent: String): PortfolioData {
        require(canParse(csvContent)) { "Supplied file does not match XTB statement format (XLSX or CSV)." }

        val trimmed = csvContent.trim()
        val xlsxBytes = extractXlsxBytes(trimmed)

        return if (xlsxBytes != null) {
            parseXlsx(xlsxBytes)
        } else {
            parseCsv(trimmed)
        }
    }

    private fun extractXlsxBytes(content: String): ByteArray? {
        if (content.startsWith("BASE64_XLSX:")) {
            val b64 = content.substring("BASE64_XLSX:".length).trim()
            return decodeBase64(b64)
        }

        // Direct Base64 check (starts with standard Zip magic in base64: "UEsDB" or "UEsBA")
        if (content.length > 50 && (content.startsWith("UEsDB") || content.startsWith("UEsBA"))) {
            val decoded = decodeBase64(content)
            if (decoded != null && isZipMagic(decoded)) return decoded
        }

        // Raw bytes check (ISO_8859_1 string representation)
        val rawBytes = content.toByteArray(Charsets.ISO_8859_1)
        if (isZipMagic(rawBytes)) {
            return rawBytes
        }

        return null
    }

    private fun isZipMagic(bytes: ByteArray): Boolean {
        return bytes.size >= 4 &&
                bytes[0] == 0x50.toByte() && // 'P'
                bytes[1] == 0x4B.toByte() && // 'K'
                bytes[2] == 0x03.toByte() &&
                bytes[3] == 0x04.toByte()
    }

    private fun decodeBase64(b64Str: String): ByteArray? {
        val cleanB64 = b64Str.replace("\r", "").replace("\n", "").trim()
        return try {
            java.util.Base64.getDecoder().decode(cleanB64)
        } catch (_: Exception) {
            try {
                java.util.Base64.getMimeDecoder().decode(cleanB64)
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun canParseXlsx(bytes: ByteArray): Boolean {
        return try {
            val entries = unzipEntries(bytes)
            if (entries.isEmpty()) return false
            val sharedStrings = parseSharedStrings(entries["xl/sharedStrings.xml"])
            val sheetMap = parseSheetMap(entries["xl/workbook.xml"], entries["xl/_rels/workbook.xml.rels"])

            val hasWorksheets = entries.keys.any { it.startsWith("xl/worksheets/") }
            if (!hasWorksheets) return false

            val sharedStringsText = sharedStrings.joinToString(" ").lowercase()
            val hasXtbKeywords = sharedStringsText.contains("open positions") ||
                    sharedStringsText.contains("closed positions") ||
                    sharedStringsText.contains("cash operations") ||
                    sharedStringsText.contains("xtb")

            hasXtbKeywords || sheetMap.containsKey("Open Positions") || sheetMap.containsKey("Cash Operations")
        } catch (_: Exception) {
            false
        }
    }

    private fun parseXlsx(bytes: ByteArray): PortfolioData {
        val entries = unzipEntries(bytes)
        val sharedStrings = parseSharedStrings(entries["xl/sharedStrings.xml"])
        val sheetMap = parseSheetMap(entries["xl/workbook.xml"], entries["xl/_rels/workbook.xml.rels"])

        val openPositionsSheetPath = sheetMap["Open Positions"]
            ?: sheetMap.entries.firstOrNull { (name, _) ->
                val lower = name.lowercase()
                lower.contains("open") || lower.contains("otvorene") || lower.contains("otvorené")
            }?.value
            ?: entries.keys.firstOrNull { it.contains("worksheets/sheet3.xml") }
            ?: entries.keys.firstOrNull { it.contains("worksheets/sheet") }
            ?: throw IllegalArgumentException("XLSX neobsahuje hárok 'Open Positions'.")

        val cashOperationsSheetPath = sheetMap["Cash Operations"]
            ?: sheetMap.entries.firstOrNull { (name, _) ->
                val lower = name.lowercase()
                lower.contains("cash") || lower.contains("hotovost") || lower.contains("hotovosť") || lower.contains("operacie") || lower.contains("operácie")
            }?.value
            ?: entries.keys.firstOrNull { it.contains("worksheets/sheet2.xml") }

        val closedPositionsSheetPath = sheetMap["Closed Positions"]
            ?: sheetMap.entries.firstOrNull { (name, _) ->
                val lower = name.lowercase()
                lower.contains("closed") || lower.contains("zatvorene") || lower.contains("zatvorené") ||
                lower.contains("uzavrete") || lower.contains("uzavreté") || lower.contains("uzavrene") ||
                lower.contains("uzavřené") || lower.contains("zamknięte")
            }?.value
            ?: entries.keys.firstOrNull { it !in listOfNotNull(openPositionsSheetPath, cashOperationsSheetPath) && it.startsWith("xl/worksheets/") }

        val openPosRows = parseSheetRows(entries[openPositionsSheetPath], sharedStrings)
        val cashOpRows = cashOperationsSheetPath?.let { parseSheetRows(entries[it], sharedStrings) } ?: emptyList()
        val closedPosRows = closedPositionsSheetPath?.let { parseSheetRows(entries[it], sharedStrings) } ?: emptyList()

        println("DEBUG openPosRows size: ${openPosRows.size}, cashOpRows size: ${cashOpRows.size}, closedPosRows size: ${closedPosRows.size}")

        var accountId = "50198829"
        var currency = "EUR"

        // Parse top metadata from Open Positions sheet
        for (row in openPosRows.take(10)) {
            if (row.size >= 2 && row[0].equals("Account number", ignoreCase = true)) {
                accountId = row[1].ifBlank { accountId }
            }
            if (row.size >= 4 && row[1].contains("position value", ignoreCase = true) && row[3].isNotBlank()) {
                currency = row[3]
            }
        }

        // Calculate total cash from Cash Operations sheet
        var totalCash = 0.0
        val cashHeaderIdx = cashOpRows.indexOfFirst { r -> r.any { it.equals("Type", ignoreCase = true) } && r.any { it.equals("Amount", ignoreCase = true) } }
        val amountColIdx = if (cashHeaderIdx != -1) cashOpRows[cashHeaderIdx].indexOfFirst { it.equals("Amount", ignoreCase = true) } else 5

        val totalRow = cashOpRows.find { r -> r.isNotEmpty() && r[0].equals("Total", ignoreCase = true) }
        if (totalRow != null && totalRow.size > amountColIdx) {
            totalCash = totalRow[amountColIdx].toDoubleOrNull() ?: 0.0
        } else if (cashHeaderIdx != -1 && amountColIdx != -1) {
            for (r in cashOpRows.drop(cashHeaderIdx + 1)) {
                if (r.isNotEmpty() && r[0].equals("Total", ignoreCase = true)) continue
                if (r.size > amountColIdx) {
                    val amt = r[amountColIdx].toDoubleOrNull()
                    if (amt != null) {
                        totalCash += amt
                    }
                }
            }
        }

        val positions = mutableListOf<Position>()
        val tradesMap = mutableMapOf<String, MutableList<Trade>>()

        val headerIdx = openPosRows.indexOfFirst { r ->
            r.size > 4 && (r[0].equals("Product", ignoreCase = true) || r[1].equals("Instrument/Position", ignoreCase = true) || r[2].equals("Ticker", ignoreCase = true))
        }

        if (headerIdx != -1) {
            val headerRow = openPosRows[headerIdx].map { it.lowercase() }
            val tickerIdx = headerRow.indexOf("ticker").let { if (it != -1) it else 2 }
            val nameIdx = headerRow.indexOf("instrument/position").let { if (it != -1) it else 1 }
            val catIdx = headerRow.indexOf("category").let { if (it != -1) it else 3 }
            val typeIdx = headerRow.indexOf("type").let { if (it != -1) it else 4 }
            val volIdx = headerRow.indexOf("volume").let { if (it != -1) it else 5 }
            val valIdx = headerRow.indexOf("value").let { if (it != -1) it else 6 }
            val openPriceIdx = headerRow.indexOf("open price").let { if (it != -1) it else 8 }
            val openTimeIdx = headerRow.indexOfFirst { it.contains("open time") || it == "time" || it == "date" }.let { if (it != -1) it else 9 }
            val netPnlIdx = headerRow.indexOf("net profit").let { if (it != -1) it else 13 }
            val netPnlPctIdx = headerRow.indexOf("net profit %").let { if (it != -1) it else 12 }
            val commIdx = headerRow.indexOfFirst { it.contains("commission") || it == "fee" }.let { if (it != -1) it else 16 }

            var currentTicker = ""

            for (r in openPosRows.drop(headerIdx + 1)) {
                if (r.size <= maxOf(tickerIdx, volIdx, valIdx)) continue

                val typeVal = r.getOrElse(typeIdx) { "" }.trim()
                val ticker = r.getOrElse(tickerIdx) { "" }.trim().ifBlank { currentTicker }
                val vol = r.getOrElse(volIdx) { "" }.toDoubleOrNull() ?: 0.0
                val openPrice = r.getOrElse(openPriceIdx) { "" }.toDoubleOrNull() ?: 0.0

                if (typeVal.isBlank()) {
                    // Summary row for position
                    if (ticker.isBlank()) continue
                    currentTicker = ticker

                    val name = r.getOrElse(nameIdx) { "" }.ifBlank { ticker }
                    val catStr = r.getOrElse(catIdx) { "" }
                    val marketValue = r.getOrElse(valIdx) { "" }.toDoubleOrNull() ?: 0.0
                    val netPnl = r.getOrElse(netPnlIdx) { "" }.toDoubleOrNull() ?: 0.0
                    val netPnlPct = r.getOrElse(netPnlPctIdx) { "" }.toDoubleOrNull()

                    if (vol <= 0.0 && marketValue <= 0.0) continue

                    val assetCategory = mapXtbCategory(catStr, ticker)
                    val markPrice = if (vol > 0.0) marketValue / vol else openPrice
                    val costBasisTotal = openPrice * vol
                    val pnlPercentage = netPnlPct ?: if (costBasisTotal != 0.0) (netPnl / costBasisTotal) * 100.0 else 0.0
                    val posCurrency = detectCurrencyFromTicker(ticker, currency)

                    val position = Position(
                        id = "xtb_pos_${positions.size + 1}_$ticker",
                        symbol = ticker,
                        description = name,
                        assetCategory = assetCategory,
                        quantity = vol,
                        costBasisPrice = openPrice,
                        markPrice = markPrice,
                        marketValue = marketValue,
                        dailyPnl = 0.0,
                        totalPnl = netPnl,
                        pnlPercentage = pnlPercentage,
                        sector = getSectorForSymbol(ticker, assetCategory),
                        country = getCountryForSymbol(ticker),
                        currency = posCurrency
                    )
                    positions.add(position)
                } else {
                    // Lot row (Trade) e.g., BUY / SELL
                    if (ticker.isBlank()) continue
                    val positionId = r.getOrElse(nameIdx) { "" }.trim()
                    val tradeId = if (positionId.isNotBlank()) "xtb_trade_$positionId" else "xtb_trade_${ticker}_${tradesMap.values.sumOf { it.size } + 1}"
                    val rawDate = r.getOrElse(openTimeIdx) { "" }
                    val formattedDate = parseExcelDate(rawDate)
                    val commission = r.getOrElse(commIdx) { "" }.toDoubleOrNull() ?: 0.0

                    val trade = Trade(
                        id = tradeId,
                        symbol = ticker,
                        date = formattedDate,
                        buySell = typeVal.uppercase(),
                        quantity = vol,
                        price = openPrice,
                        commission = commission,
                        totalAmount = openPrice * vol
                    )
                    tradesMap.getOrPut(ticker.uppercase()) { mutableListOf() }.add(trade)
                }
            }
        }

        // Process Closed Positions sheet for closed trades & realized P&L
        val closedTickerMap = mutableMapOf<String, ClosedPositionInfo>()
        var totalRealizedPnl = 0.0

        val closedHeaderIdx = closedPosRows.indexOfFirst { r ->
            r.any { cell ->
                val lower = cell.lowercase()
                lower.contains("profit") || lower.contains("zisk") || lower.contains("pnl") ||
                lower.contains("close price") || lower.contains("zatvaracia") || lower.contains("uzatvaracia")
            }
        }

        if (closedHeaderIdx != -1) {
            val closedHeader = closedPosRows[closedHeaderIdx].map { it.lowercase() }
            val instrumentIdx = closedHeader.indexOfFirst { it.contains("instrument") || it.contains("position") }.let { if (it != -1) it else 0 }
            val tickerIdx = closedHeader.indexOfFirst { it.contains("ticker") || it.contains("symbol") }.let { if (it != -1) it else 1 }
            val catIdx = closedHeader.indexOfFirst { it.contains("category") }.let { if (it != -1) it else 2 }
            val typeIdx = closedHeader.indexOfFirst { it.contains("type") }.let { if (it != -1) it else 3 }
            val volIdx = closedHeader.indexOfFirst { it.contains("volume") || it.contains("shares") }.let { if (it != -1) it else 4 }
            val openPriceIdx = closedHeader.indexOfFirst { it.contains("open price") }.let { if (it != -1) it else 5 }
            val openTimeIdx = closedHeader.indexOfFirst { it.contains("open time") || it.contains("open date") }.let { if (it != -1) it else 6 }
            val closePriceIdx = closedHeader.indexOfFirst { it.contains("close price") }.let { if (it != -1) it else 7 }
            val closeTimeIdx = closedHeader.indexOfFirst { it.contains("close time") || it.contains("close date") }.let { if (it != -1) it else 8 }
            val netPnlIdx = closedHeader.indexOfFirst { col ->
                col.contains("net profit") || col.contains("profit/loss") || col.contains("čistý zisk") ||
                col.contains("cisty zisk") || col.contains("zysk netto") || col.contains("nettogewinn") ||
                col.contains("profit") || col.contains("zisk") || col.contains("pnl")
            }.let { if (it != -1) it else 10 }
            val purchaseValIdx = closedHeader.indexOfFirst { it.contains("purchase value") || it.contains("buy value") }.let { if (it != -1) it else 12 }
            val saleValIdx = closedHeader.indexOfFirst { it.contains("sale value") || it.contains("sell value") }.let { if (it != -1) it else 13 }
            val commIdx = closedHeader.indexOfFirst { it.contains("commission") || it == "fee" }.let { if (it != -1) it else 16 }
            val posIdIdx = closedHeader.indexOfFirst { it.contains("position id") || it == "id" }.let { if (it != -1) it else 23 }

            for (r in closedPosRows.drop(closedHeaderIdx + 1)) {
                if (r.isEmpty()) continue
                val firstCell = r[0].trim().lowercase()

                if (firstCell.contains("profit") || firstCell.contains("total") || firstCell.contains("súčet") ||
                    firstCell.contains("spolu") || firstCell.contains("celkom") || firstCell.contains("zisk")) {
                    val summaryVal = if (netPnlIdx != -1 && r.size > netPnlIdx) r[netPnlIdx].cleanDoubleOrNull() else null
                    val fallbackVal = r.drop(1).firstNotNullOfOrNull { it.cleanDoubleOrNull() }
                    if (summaryVal != null && summaryVal != 0.0) {
                        totalRealizedPnl = summaryVal
                    } else if (fallbackVal != null && fallbackVal != 0.0) {
                        totalRealizedPnl = fallbackVal
                    }
                    continue
                }

                if (r.size <= maxOf(tickerIdx, volIdx)) continue

                val ticker = r.getOrElse(tickerIdx) { "" }.trim()
                if (ticker.isBlank()) continue

                val instrumentName = r.getOrElse(instrumentIdx) { "" }.trim().ifBlank { ticker }
                val catStr = r.getOrElse(catIdx) { "" }.trim()
                val vol = r.getOrElse(volIdx) { "" }.cleanDoubleOrNull() ?: 0.0
                val openPrice = r.getOrElse(openPriceIdx) { "" }.cleanDoubleOrNull() ?: 0.0
                val openTimeRaw = r.getOrElse(openTimeIdx) { "" }
                val openTime = parseExcelDate(openTimeRaw)
                val closePrice = r.getOrElse(closePriceIdx) { "" }.cleanDoubleOrNull() ?: 0.0
                val closeTimeRaw = r.getOrElse(closeTimeIdx) { "" }
                val closeTime = parseExcelDate(closeTimeRaw)
                val profit = r.getOrElse(netPnlIdx) { "" }.cleanDoubleOrNull() ?: 0.0
                val purchaseVal = r.getOrElse(purchaseValIdx) { "" }.cleanDoubleOrNull() ?: (vol * openPrice)
                val saleVal = r.getOrElse(saleValIdx) { "" }.cleanDoubleOrNull() ?: (vol * closePrice)
                val commission = r.getOrElse(commIdx) { "" }.cleanDoubleOrNull() ?: 0.0
                val posId = r.getOrElse(posIdIdx) { "" }.trim()

                if (vol <= 0.0) continue

                val sellTradeId = if (posId.isNotBlank()) "xtb_sell_$posId" else "xtb_sell_${ticker}_${closeTime}"
                val sellTrade = Trade(
                    id = sellTradeId,
                    symbol = ticker,
                    date = closeTime,
                    buySell = "SELL",
                    quantity = vol,
                    price = closePrice,
                    commission = commission,
                    totalAmount = saleVal
                )

                val buyTradeId = if (posId.isNotBlank()) "xtb_buy_$posId" else "xtb_buy_${ticker}_${openTime}"
                val buyTrade = Trade(
                    id = buyTradeId,
                    symbol = ticker,
                    date = openTime,
                    buySell = "BUY",
                    quantity = vol,
                    price = openPrice,
                    commission = 0.0,
                    totalAmount = purchaseVal
                )

                tradesMap.getOrPut(ticker.uppercase()) { mutableListOf() }.add(sellTrade)
                tradesMap.getOrPut(ticker.uppercase()) { mutableListOf() }.add(buyTrade)

                val closedInfo = closedTickerMap.getOrPut(ticker.uppercase()) {
                    ClosedPositionInfo(
                        ticker = ticker,
                        instrumentName = instrumentName,
                        categoryStr = catStr
                    )
                }
                closedInfo.totalProfit += profit
                closedInfo.totalPurchaseVal += purchaseVal
                closedInfo.totalSaleVal += saleVal
                closedInfo.trades.add(sellTrade)
                closedInfo.trades.add(buyTrade)
            }
        }

        if (totalRealizedPnl == 0.0 && closedTickerMap.isNotEmpty()) {
            totalRealizedPnl = closedTickerMap.values.sumOf { it.totalProfit }
        }

        // Add closed positions (quantity = 0) for tickers that are NOT in open positions
        for ((upperTicker, info) in closedTickerMap) {
            val existsInOpen = positions.any { pos ->
                pos.symbol.equals(info.ticker, ignoreCase = true) ||
                (pos.description.isNotBlank() && pos.description.equals(info.instrumentName, ignoreCase = true))
            }
            if (!existsInOpen) {
                val assetCategory = mapXtbCategory(info.categoryStr, info.ticker)
                val posCurrency = detectCurrencyFromTicker(info.ticker, currency)
                val pnlPct = if (info.totalPurchaseVal > 0.0) (info.totalProfit / info.totalPurchaseVal) * 100.0 else 0.0

                val closedPosition = Position(
                    id = "xtb_closed_pos_${info.ticker}",
                    symbol = info.ticker,
                    description = info.instrumentName,
                    assetCategory = assetCategory,
                    quantity = 0.0,
                    costBasisPrice = if (info.trades.isNotEmpty()) info.trades.filter { it.buySell == "BUY" }.map { it.price }.average().let { if (it.isNaN()) 0.0 else it } else 0.0,
                    markPrice = if (info.trades.isNotEmpty()) info.trades.filter { it.buySell == "SELL" }.map { it.price }.average().let { if (it.isNaN()) 0.0 else it } else 0.0,
                    marketValue = 0.0,
                    dailyPnl = 0.0,
                    totalPnl = info.totalProfit,
                    pnlPercentage = pnlPct,
                    sector = getSectorForSymbol(info.ticker, assetCategory),
                    country = getCountryForSymbol(info.ticker),
                    currency = posCurrency,
                    trades = info.trades.sortedByDescending { it.date }
                )
                positions.add(closedPosition)
            }
        }

        val positionsWithTrades = positions.map { pos ->
            val posSymbolUpper = pos.symbol.uppercase()
            val posDescLower = pos.description.trim().lowercase()

            val symbolTrades = tradesMap[posSymbolUpper] ?: emptyList()
            val descTrades = closedTickerMap.values
                .filter { info ->
                    info.ticker.uppercase() == posSymbolUpper ||
                    (posDescLower.isNotBlank() && info.instrumentName.trim().lowercase() == posDescLower)
                }
                .flatMap { it.trades }

            val mergedTrades = (pos.trades + symbolTrades + descTrades)
                .distinctBy { it.id }
                .sortedByDescending { it.date }

            pos.copy(trades = mergedTrades)
        }

        val totalMarketValue = positionsWithTrades.sumOf { it.marketValue }
        val netLiquidation = totalMarketValue + totalCash
        val openPositionsList = positionsWithTrades.filter { it.quantity > 0.0 }
        val totalUnrealizedPnl = openPositionsList.sumOf { it.totalPnl }
        val combinedTotalPnl = totalUnrealizedPnl + totalRealizedPnl
        val totalCostBasis = openPositionsList.sumOf { it.costBasisPrice * it.quantity }
        val totalPnlPct = if (totalCostBasis > 0.0) (combinedTotalPnl / totalCostBasis) * 100.0 else 0.0

        return PortfolioData(
            accountSummary = AccountSummary(
                accountId = "XTB-$accountId",
                accountName = "XTB Account ($accountId)",
                currency = currency,
                netLiquidation = netLiquidation,
                totalCashValue = totalCash,
                settledCash = totalCash,
                accruedCash = 0.0,
                buyingPower = totalCash,
                dailyPnl = 0.0,
                dailyPnlPercentage = 0.0,
                totalPnl = combinedTotalPnl,
                totalPnlPercentage = totalPnlPct
            ),
            balances = Balances(
                totalCash = totalCash,
                settledCash = totalCash,
                accruedCash = 0.0,
                unrealizedPnl = totalUnrealizedPnl,
                realizedPnl = totalRealizedPnl,
                netLiquidation = netLiquidation
            ),
            marginSummary = MarginRequirements(0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
            positions = positionsWithTrades,
            sectorAllocations = calculateSectorAllocations(positionsWithTrades),
            geographyAllocations = calculateGeographyAllocations(positionsWithTrades),
            isMockData = false,
            referenceCode = "XTB-XLSX-IMPORT"
        )
    }

    private fun parseCsv(csvContent: String): PortfolioData {
        val lines = csvContent.lines().map { it.trim() }.filter { it.isNotBlank() }
        val positions = mutableListOf<Position>()
        val totalCash = 0.0

        val headerIdx = lines.indexOfFirst {
            it.contains("symbol", ignoreCase = true) || it.contains("ticker", ignoreCase = true)
        }

        if (headerIdx != -1) {
            val headers = lines[headerIdx].split(";").map { it.lowercase().trim('"') }
            val tickerIdx = headers.indexOfFirst { it == "ticker" || it == "symbol" }
            val nameIdx = headers.indexOfFirst { it == "instrument" || it == "name" }
            val volIdx = headers.indexOfFirst { it == "volume" || it == "volume/shares" || it == "shares" }
            val valIdx = headers.indexOfFirst { it == "value" || it == "market value" }
            val priceIdx = headers.indexOfFirst { it == "open price" || it == "cost price" }
            val pnlIdx = headers.indexOfFirst { it == "profit" || it == "net profit" }

            for (line in lines.drop(headerIdx + 1)) {
                val tokens = line.split(";").map { it.trim('"').trim() }
                if (tokens.size <= tickerIdx || tickerIdx == -1) continue

                val ticker = tokens[tickerIdx]
                if (ticker.isBlank()) continue

                val name = if (nameIdx != -1 && tokens.size > nameIdx) tokens[nameIdx] else ticker
                val vol = if (volIdx != -1 && tokens.size > volIdx) tokens[volIdx].toDoubleOrNull() ?: 0.0 else 0.0
                val mktVal = if (valIdx != -1 && tokens.size > valIdx) tokens[valIdx].toDoubleOrNull() ?: 0.0 else 0.0
                val openPrice = if (priceIdx != -1 && tokens.size > priceIdx) tokens[priceIdx].toDoubleOrNull() ?: 0.0 else 0.0
                val pnl = if (pnlIdx != -1 && tokens.size > pnlIdx) tokens[pnlIdx].toDoubleOrNull() ?: 0.0 else 0.0

                if (vol <= 0.0 && mktVal <= 0.0) continue

                val category = mapXtbCategory("", ticker)
                val markPrice = if (vol > 0.0) mktVal / vol else openPrice

                positions.add(
                    Position(
                        id = "xtb_csv_pos_${positions.size + 1}_$ticker",
                        symbol = ticker,
                        description = name,
                        assetCategory = category,
                        quantity = vol,
                        costBasisPrice = openPrice,
                        markPrice = markPrice,
                        marketValue = mktVal,
                        dailyPnl = 0.0,
                        totalPnl = pnl,
                        pnlPercentage = if (openPrice * vol != 0.0) (pnl / (openPrice * vol)) * 100.0 else 0.0,
                        sector = getSectorForSymbol(ticker, category),
                        country = getCountryForSymbol(ticker)
                    )
                )
            }
        }

        val totalMarketValue = positions.sumOf { it.marketValue }
        val netLiquidation = totalMarketValue + totalCash
        val totalPnl = positions.sumOf { it.totalPnl }

        return PortfolioData(
            accountSummary = AccountSummary(
                accountId = "XTB-Account",
                accountName = "XTB Account",
                currency = "EUR",
                netLiquidation = netLiquidation,
                totalCashValue = totalCash,
                settledCash = totalCash,
                accruedCash = 0.0,
                buyingPower = totalCash,
                dailyPnl = 0.0,
                dailyPnlPercentage = 0.0,
                totalPnl = totalPnl,
                totalPnlPercentage = if (netLiquidation - totalPnl != 0.0) (totalPnl / (netLiquidation - totalPnl)) * 100.0 else 0.0
            ),
            balances = Balances(totalCash, totalCash, 0.0, totalPnl, 0.0, netLiquidation),
            marginSummary = MarginRequirements(0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
            positions = positions,
            sectorAllocations = calculateSectorAllocations(positions),
            geographyAllocations = calculateGeographyAllocations(positions),
            isMockData = false,
            referenceCode = "XTB-CSV-IMPORT"
        )
    }

    // --- XML / OpenXML XLSX Helper Functions ---

    private fun unzipEntries(bytes: ByteArray): Map<String, ByteArray> {
        val entries = mutableMapOf<String, ByteArray>()
        var tempFile: java.io.File? = null
        try {
            tempFile = java.io.File.createTempFile("xtb_import_", ".xlsx")
            tempFile.writeBytes(bytes)
            java.util.zip.ZipFile(tempFile).use { zipFile ->
                val enumeration = zipFile.entries()
                while (enumeration.hasMoreElements()) {
                    val entry = enumeration.nextElement()
                    if (!entry.isDirectory) {
                        zipFile.getInputStream(entry).use { input ->
                            entries[entry.name] = input.readBytes()
                        }
                    }
                }
            }
        } catch (_: Exception) {
        } finally {
            tempFile?.delete()
        }
        return entries
    }

    private fun parseSharedStrings(xmlBytes: ByteArray?): List<String> {
        if (xmlBytes == null) return emptyList()
        val strings = mutableListOf<String>()
        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            val doc = factory.newDocumentBuilder().parse(ByteArrayInputStream(xmlBytes))
            val siList = doc.getElementsByTagNameNS("*", "si")

            for (i in 0 until siList.length) {
                val si = siList.item(i) as Element
                val tList = si.getElementsByTagNameNS("*", "t")
                val sb = StringBuilder()
                for (j in 0 until tList.length) {
                    sb.append(tList.item(j).textContent)
                }
                strings.add(sb.toString())
            }
        } catch (_: Exception) {}
        return strings
    }

    private fun parseSheetMap(workbookBytes: ByteArray?, relsBytes: ByteArray?): Map<String, String> {
        val sheetMap = mutableMapOf<String, String>()
        val relMap = mutableMapOf<String, String>()

        if (relsBytes != null) {
            try {
                val factory = DocumentBuilderFactory.newInstance()
                factory.isNamespaceAware = true
                val doc = factory.newDocumentBuilder().parse(ByteArrayInputStream(relsBytes))
                val rels = doc.getElementsByTagNameNS("*", "Relationship")
                for (i in 0 until rels.length) {
                    val elem = rels.item(i) as Element
                    val id = elem.getAttribute("Id")
                    var target = elem.getAttribute("Target")
                    if (id.isNotBlank() && target.isNotBlank()) {
                        if (!target.startsWith("xl/")) {
                            target = "xl/" + target.trimStart('/')
                        }
                        relMap[id] = target
                    }
                }
            } catch (_: Exception) {}
        }

        if (workbookBytes != null) {
            try {
                val factory = DocumentBuilderFactory.newInstance()
                factory.isNamespaceAware = true
                val doc = factory.newDocumentBuilder().parse(ByteArrayInputStream(workbookBytes))
                val sheets = doc.getElementsByTagNameNS("*", "sheet")
                for (i in 0 until sheets.length) {
                    val elem = sheets.item(i) as Element
                    val name = elem.getAttribute("name")
                    var rId = elem.getAttributeNS("http://schemas.openxmlformats.org/officeDocument/2006/relationships", "id")
                    if (rId.isBlank()) {
                        rId = elem.getAttribute("r:id")
                    }
                    val target = relMap[rId] ?: "xl/worksheets/sheet${i + 1}.xml"
                    if (name.isNotBlank()) {
                        sheetMap[name] = target
                    }
                }
            } catch (_: Exception) {}
        }

        return sheetMap
    }

    private fun parseSheetRows(sheetBytes: ByteArray?, sharedStrings: List<String>): List<List<String>> {
        if (sheetBytes == null) return emptyList()
        val rows = mutableListOf<List<String>>()

        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            val doc = factory.newDocumentBuilder().parse(ByteArrayInputStream(sheetBytes))
            val rowNodes = doc.getElementsByTagNameNS("*", "row")

            for (i in 0 until rowNodes.length) {
                val rowElem = rowNodes.item(i) as Element
                val cNodes = rowElem.getElementsByTagNameNS("*", "c")
                val rowMap = mutableMapOf<Int, String>()
                var maxColIdx = -1

                for (j in 0 until cNodes.length) {
                    val cElem = cNodes.item(j) as Element
                    val rRef = cElem.getAttribute("r")
                    val colIdx = if (rRef.isNotBlank()) getColIndex(rRef) else j

                    val tAttr = cElem.getAttribute("t")
                    val vNodes = cElem.getElementsByTagNameNS("*", "v")
                    var valStr = ""

                    if (vNodes.length > 0) {
                        val rawVal = vNodes.item(0).textContent.trim()
                        if (tAttr == "s") {
                            val idx = rawVal.toIntOrNull()
                            if (idx != null && idx >= 0 && idx < sharedStrings.size) {
                                valStr = sharedStrings[idx]
                            }
                        } else {
                            valStr = rawVal
                        }
                    }

                    if (colIdx >= 0) {
                        rowMap[colIdx] = valStr
                        if (colIdx > maxColIdx) maxColIdx = colIdx
                    }
                }

                val rowList = mutableListOf<String>()
                for (col in 0..maxOf(maxColIdx, 0)) {
                    rowList.add(rowMap[col] ?: "")
                }
                rows.add(rowList)
            }
        } catch (_: Exception) {}

        return rows
    }

    private fun getColIndex(cellRef: String): Int {
        val letters = cellRef.takeWhile { it.isLetter() }.uppercase()
        if (letters.isEmpty()) return -1
        var index = 0
        for (ch in letters) {
            index = index * 26 + (ch - 'A' + 1)
        }
        return index - 1
    }

    private fun String.cleanDoubleOrNull(): Double? {
        if (this.isBlank()) return null
        val cleaned = this.replace(" ", "")
            .replace("\u00A0", "")
            .replace("EUR", "", ignoreCase = true)
            .replace("USD", "", ignoreCase = true)
            .replace("€", "")
            .replace("$", "")
            .replace(',', '.')
        return cleaned.toDoubleOrNull()
    }

    private fun mapXtbCategory(catStr: String, ticker: String): AssetCategory {
        val upper = catStr.uppercase()
        return when {
            upper.contains("ETF") -> AssetCategory.ETF
            upper.contains("STOCK") || upper.contains("AKCIE") -> AssetCategory.STK
            upper.contains("CRYPTO") -> AssetCategory.CRYPTO
            upper.contains("BOND") -> AssetCategory.BOND
            upper.contains("OPTION") -> AssetCategory.OPT
            ticker.endsWith(".DE") || ticker.endsWith(".US") || ticker.endsWith(".UK") || ticker.endsWith(".FI") -> AssetCategory.STK
            else -> AssetCategory.STK
        }
    }

    private fun getCountryForSymbol(symbol: String): String {
        val upper = symbol.uppercase()
        return when {
            upper.endsWith(".US") -> "US"
            upper.endsWith(".DE") -> "DE"
            upper.endsWith(".UK") -> "GB"
            upper.endsWith(".FI") -> "FI"
            upper.endsWith(".FR") -> "FR"
            upper.endsWith(".NL") || upper.endsWith(".AS") -> "NL"
            else -> "US"
        }
    }

    private fun getSectorForSymbol(symbol: String, category: AssetCategory): String {
        if (category == AssetCategory.ETF) return "Broad ETF"
        val upper = symbol.uppercase()
        return when {
            upper.startsWith("AMD") || upper.startsWith("NVD") || upper.startsWith("AMAT") || upper.startsWith("LRCX") -> "Technology"
            upper.startsWith("AMZN") || upper.startsWith("MCD") -> "Consumer Discretionary"
            upper.startsWith("PEP") || upper.startsWith("O.") -> "Consumer Staples"
            upper.startsWith("NVO") || upper.startsWith("MPT") || upper.startsWith("MPW") -> "Healthcare"
            upper.startsWith("VZ") || upper.startsWith("NOKIA") -> "Telecommunications"
            else -> "Other"
        }
    }

    private fun calculateSectorAllocations(positions: List<Position>): List<SectorAllocation> {
        val totalValue = positions.sumOf { it.marketValue }
        if (totalValue <= 0.0) return emptyList()

        return positions.groupBy { it.sector }.map { (sector, posList) ->
            val sectorVal = posList.sumOf { it.marketValue }
            val pct = (sectorVal / totalValue) * 100.0
            SectorAllocation(
                sector = sector,
                value = sectorVal,
                percentage = pct,
                colorHex = mapSectorColor(sector)
            )
        }.sortedByDescending { it.value }
    }

    private fun calculateGeographyAllocations(positions: List<Position>): List<GeographyAllocation> {
        val totalValue = positions.sumOf { it.marketValue }
        if (totalValue <= 0.0) return emptyList()

        return positions.groupBy { it.country }.map { (country, posList) ->
            val countryVal = posList.sumOf { it.marketValue }
            val pct = (countryVal / totalValue) * 100.0
            GeographyAllocation(
                country = country,
                value = countryVal,
                percentage = pct
            )
        }.sortedByDescending { it.value }
    }

    private fun mapSectorColor(sector: String): String {
        return when (sector) {
            "Technology" -> "#4CAF50"
            "Consumer Discretionary" -> "#2196F3"
            "Consumer Staples" -> "#FF9800"
            "Healthcare" -> "#E91E63"
            "Telecommunications" -> "#9C27B0"
            "Broad ETF" -> "#00BCD4"
            else -> "#607D8B"
        }
    }

    private fun parseExcelDate(value: String): String {
        if (value.isBlank()) return ""
        val doubleVal = value.toDoubleOrNull()
        if (doubleVal != null && doubleVal > 10000.0) {
            val millis = ((doubleVal - 25569.0) * 86400000.0).toLong()
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            return sdf.format(java.util.Date(millis))
        }
        return value
    }

    private fun detectCurrencyFromTicker(ticker: String, defaultCurrency: String = "USD"): String {
        val upper = ticker.trim().uppercase()
        return when {
            upper.endsWith(".DE") || upper.endsWith(".PA") || upper.endsWith(".MI") || upper.endsWith(".AS") -> "EUR"
            upper.endsWith(".UK") || upper.endsWith(".L") -> "GBP"
            upper.endsWith(".WA") -> "PLN"
            upper.endsWith(".PR") -> "CZK"
            upper.endsWith(".US") || upper.contains("US.") -> "USD"
            else -> defaultCurrency
        }
    }
}
