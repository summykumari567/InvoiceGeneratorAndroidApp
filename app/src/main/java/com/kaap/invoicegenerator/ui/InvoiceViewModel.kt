package com.kaap.invoicegenerator.ui

import androidx.lifecycle.ViewModel
import com.kaap.invoicegenerator.data.InvoiceHeader
import com.kaap.invoicegenerator.data.InvoiceRow
import com.kaap.invoicegenerator.data.InvoiceTotals
import com.kaap.invoicegenerator.data.LineItemGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.format.DateTimeFormatter
import kotlin.math.roundToLong

class InvoiceViewModel : ViewModel() {

    private val _header = MutableStateFlow(InvoiceHeader())
    val header: StateFlow<InvoiceHeader> = _header.asStateFlow()

    private val _lineItemGroups = MutableStateFlow<List<LineItemGroup>>(emptyList())
    val lineItemGroups: StateFlow<List<LineItemGroup>> = _lineItemGroups.asStateFlow()

    private val _expandedRows = MutableStateFlow<List<InvoiceRow>>(emptyList())
    val expandedRows: StateFlow<List<InvoiceRow>> = _expandedRows.asStateFlow()

    private val _totals = MutableStateFlow(InvoiceTotals(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0))
    val totals: StateFlow<InvoiceTotals> = _totals.asStateFlow()

    private val dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun updateHeader(header: InvoiceHeader) {
        _header.value = header
    }

    fun addLineItemGroup(group: LineItemGroup) {
        _lineItemGroups.value = _lineItemGroups.value + group
        recalculate()
    }

    fun updateLineItemGroup(group: LineItemGroup) {
        _lineItemGroups.value = _lineItemGroups.value.map { if (it.id == group.id) group else it }
        recalculate()
    }

    fun removeLineItemGroup(id: String) {
        _lineItemGroups.value = _lineItemGroups.value.filter { it.id != id }
        recalculate()
    }

    private fun recalculate() {
        val rows = expandGroups(_lineItemGroups.value)
        _expandedRows.value = rows
        _totals.value = computeTotals(rows)
    }

    private fun expandGroups(groups: List<LineItemGroup>): List<InvoiceRow> {
        val rawRows = mutableListOf<InvoiceRow>()
        for (group in groups) {
            var date = group.startDate
            while (!date.isAfter(group.endDate)) {
                val dateStr = date.format(dateFmt)
                val particularsText = buildString {
                    append(group.particulars)
                    append(" ")
                    append(dateStr)
                    if (group.subParticular.isNotBlank()) {
                        append(" ")
                        append(group.subParticular)
                    }
                }
                val amt = group.quantity * group.rate
                val taxableAmt = amt  // DISC% is always 0
                val cgstAmt = taxableAmt * 2.5 / 100.0
                val sgstAmt = taxableAmt * 2.5 / 100.0
                val netAmt = taxableAmt + cgstAmt + sgstAmt

                rawRows.add(
                    InvoiceRow(
                        slNo = 0,
                        date = date,
                        particularsText = particularsText,
                        hsnAcs = group.hsnCode,
                        qty = group.quantity,
                        rate = group.rate,
                        amt = amt,
                        discPct = 0.0,
                        taxableAmt = taxableAmt,
                        cgstRate = 2.5,
                        cgstAmt = cgstAmt,
                        sgstRate = 2.5,
                        sgstAmt = sgstAmt,
                        netAmt = netAmt
                    )
                )
                date = date.plusDays(1)
            }
        }
        return rawRows
            .sortedBy { it.date }
            .mapIndexed { i, r -> r.copy(slNo = i + 1) }
    }

    private fun computeTotals(rows: List<InvoiceRow>): InvoiceTotals {
        val totalAmt = rows.sumOf { it.amt }
        val totalTaxableAmt = rows.sumOf { it.taxableAmt }
        val totalCgst = rows.sumOf { it.cgstAmt }
        val totalSgst = rows.sumOf { it.sgstAmt }
        val totalNet = rows.sumOf { it.netAmt }
        val grossAmt = totalNet.roundToLong().toDouble()
        val roundOff = grossAmt - totalNet
        return InvoiceTotals(
            totalAmt = totalAmt,
            totalTaxableAmt = totalTaxableAmt,
            totalCgst = totalCgst,
            totalSgst = totalSgst,
            totalNet = totalNet,
            roundOff = roundOff,
            grossAmt = grossAmt
        )
    }
}
