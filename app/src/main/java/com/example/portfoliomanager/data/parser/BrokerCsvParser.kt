package com.example.portfoliomanager.data.parser

import com.example.portfoliomanager.data.model.PortfolioData

/**
 * Interface for parsing broker-exported CSV files into domain [PortfolioData].
 */
interface BrokerCsvParser {
    /**
     * Display name of the broker (e.g. "Interactive Brokers", "XTB", "Trading212").
     */
    val brokerName: String

    /**
     * Determines whether this parser can handle the provided CSV content based on unique column headers or structure.
     */
    fun canParse(csvContent: String): Boolean

    /**
     * Parses the CSV content and transforms it into a [PortfolioData] object.
     * @throws IllegalArgumentException if the CSV structure is invalid or required fields are missing.
     */
    fun parse(csvContent: String): PortfolioData
}
