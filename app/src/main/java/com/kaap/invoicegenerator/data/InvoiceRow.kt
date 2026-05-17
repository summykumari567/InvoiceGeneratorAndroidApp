package com.kaap.invoicegenerator.data

import java.time.LocalDate

data class InvoiceRow(
    val slNo: Int,
    val date: LocalDate,
    val particularsText: String,
    val hsnAcs: String,
    val qty: Int,
    val rate: Double,
    val amt: Double,          // qty * rate
    val discPct: Double = 0.0,
    val taxableAmt: Double,   // amt (since disc=0)
    val cgstRate: Double = 2.5,
    val cgstAmt: Double,
    val sgstRate: Double = 2.5,
    val sgstAmt: Double,
    val netAmt: Double        // taxable + cgst + sgst
)
