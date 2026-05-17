package com.kaap.invoicegenerator.data

import java.time.LocalDate
import java.util.UUID

data class LineItemGroup(
    val id: String = UUID.randomUUID().toString(),
    val particulars: String = "",
    val subParticular: String = "",
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate = LocalDate.now(),
    val hsnCode: String = "996337",
    val quantity: Int = 1,
    val rate: Double = 0.0
)
