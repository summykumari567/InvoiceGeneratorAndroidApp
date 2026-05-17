package com.kaap.invoicegenerator.data

data class InvoiceTotals(
    val totalAmt: Double,
    val totalTaxableAmt: Double,
    val totalCgst: Double,
    val totalSgst: Double,
    val totalNet: Double,
    val roundOff: Double,
    val grossAmt: Double
)
