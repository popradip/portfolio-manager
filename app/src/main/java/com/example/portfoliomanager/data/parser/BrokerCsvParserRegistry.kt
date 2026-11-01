package com.example.portfoliomanager.data.parser

import com.example.portfoliomanager.data.model.PortfolioData

/**
 * Registry managing supported broker CSV parsers and automatically detecting the correct parser for a CSV file.
 */
class BrokerCsvParserRegistry(
    private val parsers: List<BrokerCsvParser> = listOf(
        IbkrFlexCsvParser(),
        XtbCsvParser(),
        Trading212CsvParser()
    )
) {

    /**
     * Finds the first matching parser for the provided CSV string.
     */
    fun findParser(csvContent: String): BrokerCsvParser? {
        return parsers.firstOrNull { it.canParse(csvContent) }
    }

    /**
     * Parses CSV content using the automatically detected parser.
     * @throws IllegalArgumentException if no matching broker format is detected.
     */
    fun parse(csvContent: String): ParseResult {
        val parser = findParser(csvContent)
            ?: throw IllegalArgumentException("Nerozoznaný formát CSV. Podporované sú výpisy z IBKR, XTB a Trading 212.")

        val portfolioData = parser.parse(csvContent)
        return ParseResult(
            brokerName = parser.brokerName,
            portfolioData = portfolioData
        )
    }

    data class ParseResult(
        val brokerName: String,
        val portfolioData: PortfolioData
    )
}
