package com.kaap.invoicegenerator.data

data class InvoiceHeader(
    val invoiceNo: String = "",
    val invoiceDate: String = "",   // DD/MM/YYYY
    val letterNo: String = "",
    val orderBy: String = ""
)
