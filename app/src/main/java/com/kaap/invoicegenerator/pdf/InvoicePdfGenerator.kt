package com.kaap.invoicegenerator.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.kaap.invoicegenerator.data.InvoiceHeader
import com.kaap.invoicegenerator.data.InvoiceRow
import com.kaap.invoicegenerator.data.InvoiceTotals
import com.kaap.invoicegenerator.utils.NumberToWords
import java.io.File
import java.io.FileOutputStream

class InvoicePdfGenerator(private val context: Context) {

    // ── Page dimensions (A4 @ 72dpi) ────────────────────────────────────────
    private val PW = 595f
    private val PH = 842f
    private val LM = 18f                   // left margin
    private val RE = PW - LM              // right edge = 577
    private val CW = RE - LM              // content width = 559

    // ── Table column X start positions ──────────────────────────────────────
    // Widths: 22|125|45|22|42|48|26|52|24|37|24|37|55  = 559 total
    private val CX = floatArrayOf(
        18f,   // 0  SL N.
        40f,   // 1  PARTICULARS
        165f,  // 2  HSN ACS
        210f,  // 3  QTY
        232f,  // 4  RATE
        274f,  // 5  AMT
        322f,  // 6  DISC%
        348f,  // 7  TAXABLE AMT
        400f,  // 8  CGST RATE%
        424f,  // 9  CGST AMT
        461f,  // 10 SGST RATE%
        485f,  // 11 SGST AMT
        522f,  // 12 NET AMT
        577f   // 13 right edge
    )

    // ── Paints ───────────────────────────────────────────────────────────────
    private fun boldPaint(size: Float) = Paint().apply {
        color = Color.BLACK; textSize = size; typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }
    private fun normalPaint(size: Float) = Paint().apply {
        color = Color.BLACK; textSize = size; isAntiAlias = true
    }
    private fun linePaint(width: Float = 0.5f) = Paint().apply {
        color = Color.BLACK; strokeWidth = width; style = Paint.Style.STROKE
    }
    private fun borderPaint() = linePaint(0.8f)
    private fun thinLinePaint() = linePaint(0.4f)

    // ── Fixed company / receiver constants ───────────────────────────────────
    private val COMPANY_NAME = "KAAP Hospitality & Consultancy Pvt. Ltd"
    private val COMPANY_ADDR = "Domuhan chok, opp lumbini, Bodhgaya, Gaya Bihar - 824231"
    private val COMPANY_PH   = "PH : 9304113640 , 9709449125"
    private val COMPANY_GST  = "GST NO:-10AAKCK2402P1ZO"

    private val RECV_NAME    = "BIPARD SKILL PARK PATNA"
    private val RECV_ADDR    = "WALMI CAMPUS, PATNA - 801505"
    private val RECV_GSTIN   = "10PTNB0270E1DEBIHA"
    private val RECV_STATE   = "Bihar"
    private val RECV_CODE    = "10"

    private val BANK_NAME    = "BANDHAN BANK (GAYA)"
    private val BANK_ACC     = "20100076128926"
    private val BANK_IFSC    = "BDBL0001543"

    // ── Row heights ──────────────────────────────────────────────────────────
    private val DATA_ROW_H  = 11f
    private val HEADER_ROW1_H = 15f   // first table header row
    private val HEADER_ROW2_H = 11f   // RATE%/AMT sub-row

    // ── Entry point ──────────────────────────────────────────────────────────
    fun generate(header: InvoiceHeader, rows: List<InvoiceRow>, totals: InvoiceTotals): File {
        val doc = PdfDocument()

        // Calculate layout breakpoints
        val tableStartY  = computeTableStartY()    // where data rows begin
        val footerHeight = 210f
        val footerStartY = PH - LM - footerHeight  // ~614

        val rowsOnPage1 = ((footerStartY - tableStartY) / DATA_ROW_H).toInt().coerceAtLeast(1)
        val rowsOnOtherPages = ((PH - LM - footerHeight - LM - HEADER_ROW1_H - HEADER_ROW2_H - 2f) / DATA_ROW_H).toInt().coerceAtLeast(1)

        var pageNum = 1
        var rowsLeft = rows.toMutableList()

        // ── Page 1 ────────────────────────────────────────────────────────────
        val p1Info = PdfDocument.PageInfo.Builder(PW.toInt(), PH.toInt(), pageNum).create()
        val p1 = doc.startPage(p1Info)
        val c1 = p1.canvas
        drawFullBorder(c1)
        var y = drawCompanyHeader(c1)
        y = drawInvoiceDetails(c1, header, y)
        y = drawReceiverSection(c1, header, y)
        y = drawTableHeader(c1, y)

        val page1Rows = rowsLeft.take(rowsOnPage1)
        rowsLeft = rowsLeft.drop(rowsOnPage1).toMutableList()
        y = drawDataRows(c1, page1Rows, y)

        if (rowsLeft.isEmpty()) {
            drawFooter(c1, totals, y, footerStartY)
        }
        doc.finishPage(p1)

        // ── Extra pages ───────────────────────────────────────────────────────
        while (rowsLeft.isNotEmpty()) {
            pageNum++
            val pInfo = PdfDocument.PageInfo.Builder(PW.toInt(), PH.toInt(), pageNum).create()
            val p = doc.startPage(pInfo)
            val c = p.canvas
            drawFullBorder(c)
            var py = LM + 2f
            py = drawTableHeader(c, py)
            val pageRows = rowsLeft.take(rowsOnOtherPages)
            rowsLeft = rowsLeft.drop(rowsOnOtherPages).toMutableList()
            py = drawDataRows(c, pageRows, py)
            if (rowsLeft.isEmpty()) {
                drawFooter(c, totals, py, PH - LM - footerHeight)
            }
            doc.finishPage(p)
        }

        // ── Save ───────────────────────────────────────────────────────────────
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir
        dir.mkdirs()
        val safeNo = header.invoiceNo.ifBlank { "draft" }.replace("[/\\\\:*?\"<>|]".toRegex(), "_")
        val file = File(dir, "Invoice_No.${safeNo}.pdf")
        doc.writeTo(FileOutputStream(file))
        doc.close()
        return file
    }

    // ── Company header ────────────────────────────────────────────────────────
    private fun drawCompanyHeader(c: Canvas): Float {
        var y = LM

        // Row 1: GST NO left | TAX INVOICE box center
        val row1Bottom = y + 17f
        c.drawLine(LM, y, RE, y, borderPaint())
        c.drawLine(LM, row1Bottom, RE, row1Bottom, thinLinePaint())

        val lp = normalPaint(6.5f)
        c.drawText(COMPANY_GST, LM + 2f, y + 12f, lp)

        // TAX INVOICE box
        val boxW = 80f; val boxH = 15f
        val boxX = (PW - boxW) / 2f
        c.drawRect(boxX, y + 1f, boxX + boxW, y + 1f + boxH, borderPaint())
        val tp = boldPaint(8f); tp.textAlign = Paint.Align.CENTER
        c.drawText("TAX INVOICE", PW / 2f, y + 12f, tp)

        y = row1Bottom

        // Company name
        val namePaint = boldPaint(13f); namePaint.textAlign = Paint.Align.CENTER
        y += 16f
        c.drawText(COMPANY_NAME, PW / 2f, y, namePaint)

        // Address
        val addrPaint = normalPaint(7.5f); addrPaint.textAlign = Paint.Align.CENTER
        y += 12f
        c.drawText(COMPANY_ADDR, PW / 2f, y, addrPaint)

        // Phone
        y += 11f
        c.drawText(COMPANY_PH, PW / 2f, y, addrPaint)

        y += 5f
        c.drawLine(LM, y, RE, y, thinLinePaint())
        return y
    }

    // ── Invoice header details (Invoice No, Date, Letter No, etc.) ────────────
    private fun drawInvoiceDetails(c: Canvas, header: InvoiceHeader, startY: Float): Float {
        var y = startY
        val labelP = normalPaint(7f)
        val valueP = boldPaint(7f)
        val rowH = 14f

        // Column split
        val mid = LM + CW / 2f
        c.drawLine(mid, y, mid, y + rowH * 3f, thinLinePaint())

        // Row 1: Invoice No | Letter No
        y += rowH
        c.drawText("Invoice No", LM + 2f, y, labelP)
        c.drawText(":", LM + 38f, y, labelP)
        c.drawText(header.invoiceNo, LM + 44f, y, valueP)

        c.drawText("Letter No.", mid + 2f, y, labelP)
        c.drawText(":", mid + 38f, y, labelP)
        c.drawText(header.letterNo, mid + 44f, y, valueP)

        // Row 2: Invoice Date | INV TYPE
        y += rowH
        c.drawText("Invoice Date", LM + 2f, y, labelP)
        c.drawText(":", LM + 38f, y, labelP)
        c.drawText(header.invoiceDate, LM + 44f, y, valueP)

        c.drawText("INV TYPE", mid + 2f, y, labelP)
        c.drawText(":", mid + 38f, y, labelP)
        c.drawText("CREDIT", mid + 44f, y, valueP)

        // Row 3: State | ORDER BY
        y += rowH
        c.drawText("State", LM + 2f, y, labelP)
        c.drawText(":", LM + 38f, y, labelP)
        c.drawText("Bihar", LM + 44f, y, normalPaint(7f))
        c.drawText("State Code : 10", LM + 72f, y, normalPaint(7f))

        c.drawText("ORDER BY", mid + 2f, y, labelP)
        c.drawText(":", mid + 38f, y, labelP)
        c.drawText(header.orderBy, mid + 44f, y, valueP)

        y += 4f
        c.drawLine(LM, y, RE, y, thinLinePaint())
        return y
    }

    // ── Receiver section (Billed to / Shipped to) ─────────────────────────────
    private fun drawReceiverSection(c: Canvas, header: InvoiceHeader, startY: Float): Float {
        var y = startY
        val labelP = normalPaint(6.5f)
        val boldP  = boldPaint(7f)
        val mid = LM + CW / 2f
        val rowH = 12f

        // Header row
        val hdrH = 13f
        y += hdrH
        val hdrP = boldPaint(7f)
        c.drawText("Details of Receiver Billed to", LM + 2f, y, hdrP)
        c.drawLine(mid, startY, mid, startY + hdrH, thinLinePaint())
        c.drawText("Details of Receiver Shipped to", mid + 2f, y, hdrP)

        c.drawLine(LM, y + 2f, RE, y + 2f, thinLinePaint())

        // Rows
        val rows = listOf(
            Triple("Name", "$RECV_NAME", "$RECV_NAME"),
            Triple("Address", "$RECV_ADDR", "$RECV_ADDR"),
            Triple("GSTIN", "$RECV_GSTIN", "$RECV_GSTIN"),
            Triple("State", "$RECV_STATE  State Code : $RECV_CODE", "$RECV_STATE  State Code : $RECV_CODE")
        )

        for (row in rows) {
            y += rowH
            c.drawText(row.first, LM + 2f, y, labelP)
            c.drawText(":", LM + 24f, y, labelP)
            c.drawText(row.second, LM + 30f, y, boldP)

            c.drawLine(mid, y - rowH + 2f, mid, y + 2f, thinLinePaint())
            c.drawText(row.first, mid + 2f, y, labelP)
            c.drawText(":", mid + 24f, y, labelP)
            c.drawText(row.third, mid + 30f, y, boldP)
        }

        y += 4f
        c.drawLine(LM, y, RE, y, thinLinePaint())
        return y
    }

    // ── Table header (2-row) ──────────────────────────────────────────────────
    private fun drawTableHeader(c: Canvas, startY: Float): Float {
        var y = startY
        val hp = boldPaint(6f); hp.textAlign = Paint.Align.CENTER

        // Row 1 - span headers
        y += HEADER_ROW1_H
        val centerOf = { i: Int -> (CX[i] + CX[i + 1]) / 2f }
        c.drawText("SL\nN.", centerOf(0), y - 4f, hp)
        c.drawText("PARTICULARS", centerOf(1), y, hp)
        c.drawText("HSN ACS", centerOf(2), y, hp)
        c.drawText("QTY", centerOf(3), y, hp)
        c.drawText("RATE", centerOf(4), y, hp)
        c.drawText("AMT", centerOf(5), y, hp)
        c.drawText("DISC%", centerOf(6), y, hp)
        c.drawText("TAXABLE\nAMT", centerOf(7), y - 4f, hp)

        // CGST spanning cols 8-9
        val cgstCenter = (CX[8] + CX[10]) / 2f
        c.drawText("CGST", cgstCenter, y - 3f, hp)
        c.drawLine(CX[8], startY + HEADER_ROW1_H - 2f, CX[10], startY + HEADER_ROW1_H - 2f, thinLinePaint())

        // SGST spanning cols 10-11
        val sgstCenter = (CX[10] + CX[12]) / 2f
        c.drawText("SGST", sgstCenter, y - 3f, hp)
        c.drawLine(CX[10], startY + HEADER_ROW1_H - 2f, CX[12], startY + HEADER_ROW1_H - 2f, thinLinePaint())

        c.drawText("NET AMT", centerOf(12), y, hp)

        // Row 2 - RATE%/AMT sub-headers
        y += HEADER_ROW2_H
        c.drawText("RATE%", centerOf(8), y, hp)
        c.drawText("AMT", centerOf(9), y, hp)
        c.drawText("RATE%", centerOf(10), y, hp)
        c.drawText("AMT", centerOf(11), y, hp)

        // Vertical column lines for header
        for (i in 0..13) c.drawLine(CX[i], startY, CX[i], y + 2f, thinLinePaint())
        // Horizontal separator between header rows
        c.drawLine(CX[7], startY + HEADER_ROW1_H - 3f, CX[12], startY + HEADER_ROW1_H - 3f, thinLinePaint())

        y += 2f
        c.drawLine(LM, y, RE, y, borderPaint())
        return y
    }

    // ── Data rows ─────────────────────────────────────────────────────────────
    private fun drawDataRows(c: Canvas, rows: List<InvoiceRow>, startY: Float): Float {
        var y = startY
        val np = normalPaint(6.5f); np.textAlign = Paint.Align.CENTER
        val lp = normalPaint(6.5f)  // left aligned for particulars

        for (row in rows) {
            y += DATA_ROW_H
            val cy = y  // baseline

            c.drawText(row.slNo.toString(), mid(0), cy, np)
            // Particulars: left-aligned, truncated
            val partP = normalPaint(6f)
            c.drawText(truncate(row.particularsText, CX[2] - CX[1] - 4f, partP), CX[1] + 2f, cy, partP)
            c.drawText(row.hsnAcs, mid(2), cy, np)
            c.drawText(row.qty.toString(), mid(3), cy, np)
            c.drawText(fmt2(row.rate), mid(4), cy, np)
            c.drawText(fmt2(row.amt), mid(5), cy, np)
            c.drawText(fmt2(row.discPct), mid(6), cy, np)
            c.drawText(fmt2(row.taxableAmt), mid(7), cy, np)
            c.drawText(fmt2(row.cgstRate), mid(8), cy, np)
            c.drawText(fmt2(row.cgstAmt), mid(9), cy, np)
            c.drawText(fmt2(row.sgstRate), mid(10), cy, np)
            c.drawText(fmt2(row.sgstAmt), mid(11), cy, np)
            c.drawText(fmt2(row.netAmt), mid(12), cy, np)

            // Vertical lines
            for (i in 0..13) c.drawLine(CX[i], y - DATA_ROW_H + 1f, CX[i], y + 1f, thinLinePaint())
            c.drawLine(LM, y + 1f, RE, y + 1f, thinLinePaint())
        }
        return y
    }

    // ── Footer: TOTAL row + bank details + summary ────────────────────────────
    private fun drawFooter(c: Canvas, totals: InvoiceTotals, lastRowY: Float, footerStartY: Float) {
        val np = normalPaint(6.5f); np.textAlign = Paint.Align.CENTER
        val bp = boldPaint(7f); bp.textAlign = Paint.Align.CENTER

        // Blank rows between last data row and TOTAL
        val blankAreaEnd = footerStartY
        // Draw the outer vertical lines along blank area
        for (i in 0..13) c.drawLine(CX[i], lastRowY + 1f, CX[i], blankAreaEnd, thinLinePaint())

        // ── TOTAL row ──────────────────────────────────────────────────────────
        val totalY = blankAreaEnd + 11f
        c.drawLine(LM, blankAreaEnd, RE, blankAreaEnd, borderPaint())

        val tBoldC = boldPaint(7f); tBoldC.textAlign = Paint.Align.CENTER
        c.drawText("TOTAL", (CX[0] + CX[3]) / 2f, totalY, tBoldC)

        // total row values
        c.drawText(fmt2(totals.totalAmt),          mid(5), totalY, tBoldC)
        c.drawText("",                             mid(6), totalY, tBoldC)
        c.drawText(fmt2(totals.totalTaxableAmt),   mid(7), totalY, tBoldC)
        c.drawText("",                             mid(8), totalY, tBoldC)
        c.drawText(fmt2(totals.totalCgst),         mid(9), totalY, tBoldC)
        c.drawText("",                             mid(10), totalY, tBoldC)
        c.drawText(fmt2(totals.totalSgst),         mid(11), totalY, tBoldC)
        c.drawText(fmt2(totals.totalNet),          mid(12), totalY, tBoldC)

        for (i in 0..13) c.drawLine(CX[i], blankAreaEnd, CX[i], totalY + 3f, thinLinePaint())
        c.drawLine(LM, totalY + 3f, RE, totalY + 3f, borderPaint())

        // ── Bank Details (left) + Summary (right) ─────────────────────────────
        val sectionTop = totalY + 3f
        val summaryX = LM + CW * 0.52f   // divider between bank and summary
        val summaryRight = RE

        // "BANK DETAILS" header
        val bdH = boldPaint(8f); bdH.textAlign = Paint.Align.CENTER
        val bdTop = sectionTop + 12f
        c.drawText("BANK DETAILS", (LM + summaryX) / 2f, bdTop, bdH)
        c.drawLine(LM, bdTop + 3f, summaryX, bdTop + 3f, thinLinePaint())

        // Bank rows
        val bl = normalPaint(7f)
        val bv = boldPaint(7f)
        val rowH = 12f
        var by = bdTop + 3f + 10f

        fun bankRow(label: String, colon: String, value: String) {
            c.drawText(label, LM + 2f, by, bl)
            c.drawText(colon, LM + 40f, by, bl)
            c.drawText(value, LM + 48f, by, bv)
            by += rowH
        }

        bankRow("BANK NAME", ":", BANK_NAME)
        bankRow("A/C NO.", ":", BANK_ACC)
        bankRow("IFSC CODE", ":", BANK_IFSC)

        // Vertical divider between bank and summary
        c.drawLine(summaryX, sectionTop, summaryX, by + 20f, thinLinePaint())

        // Summary rows (right side)
        val sl = normalPaint(7f)
        val sv = normalPaint(7f); sv.textAlign = Paint.Align.RIGHT
        var sy = sectionTop + 11f

        fun summaryRow(label: String, value: String) {
            c.drawText(label, summaryX + 2f, sy, sl)
            c.drawText(":", summaryX + 110f, sy, sl)
            c.drawText(value, summaryRight - 2f, sy, sv)
            sy += rowH
        }

        summaryRow("Total Amount Before", fmt2(totals.totalTaxableAmt))
        summaryRow("Add : CGST", fmt2(totals.totalCgst))
        summaryRow("Add : SGST", fmt2(totals.totalSgst))
        summaryRow("Prod DISC", "0.00")
        summaryRow("Tax Amount : GST", fmt2(totals.totalCgst + totals.totalSgst))
        summaryRow("Round Off", fmt2(totals.roundOff))

        // Gross Amt (bold)
        val gBold = boldPaint(7.5f); gBold.textAlign = Paint.Align.RIGHT
        c.drawText("Gross Amt", summaryX + 2f, sy, boldPaint(7.5f))
        c.drawText(":", summaryX + 110f, sy, sl)
        c.drawText(fmt0(totals.grossAmt), summaryRight - 2f, sy, gBold)
        sy += 3f

        val footerLineY = maxOf(by, sy) + 2f
        c.drawLine(LM, footerLineY, RE, footerLineY, borderPaint())

        // ── Rupees in words ────────────────────────────────────────────────────
        val rupeesY = footerLineY + 14f
        val rupeesP = boldPaint(7f)
        c.drawText(
            "Rupees In Words Rs. :${NumberToWords.convert(totals.grossAmt)}",
            LM + 2f, rupeesY, rupeesP
        )
        c.drawLine(LM, rupeesY + 4f, RE, rupeesY + 4f, thinLinePaint())

        // ── Signature ──────────────────────────────────────────────────────────
        val sigStart = rupeesY + 4f
        val sigP = normalPaint(7f); sigP.textAlign = Paint.Align.RIGHT
        val sigBold = boldPaint(7f); sigBold.textAlign = Paint.Align.RIGHT
        c.drawText("For $COMPANY_NAME", RE - 2f, sigStart + 22f, sigP)
        c.drawText("Authorized Signatory", RE - 2f, sigStart + 40f, sigBold)

    }

    // ── Full page outer border ────────────────────────────────────────────────
    private fun drawFullBorder(c: Canvas) {
        val bp = borderPaint()
        c.drawRect(LM, LM, RE, PH - LM, bp)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private fun mid(col: Int) = (CX[col] + CX[col + 1]) / 2f

    private fun fmt2(v: Double) = "%.2f".format(v)
    private fun fmt0(v: Double) = "%.0f".format(v)

    private fun computeTableStartY(): Float {
        // GST row(17) + company(16+12+11+5) + invoice details(14×3+4) + receiver(13+12×4+4+11+4) + table header(15+11+2)
        return 17f + (16f + 12f + 11f + 5f) + (14f * 3f + 4f) +
                (13f + 12f * 4f + 4f + 11f + 4f) + (HEADER_ROW1_H + HEADER_ROW2_H + 2f)
    }

    private fun truncate(text: String, maxWidth: Float, paint: Paint): String {
        if (paint.measureText(text) <= maxWidth) return text
        var result = text
        while (result.isNotEmpty() && paint.measureText("$result…") > maxWidth) {
            result = result.dropLast(1)
        }
        return "$result…"
    }
}
