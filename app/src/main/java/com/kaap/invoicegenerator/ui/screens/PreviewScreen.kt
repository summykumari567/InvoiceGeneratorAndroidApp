package com.kaap.invoicegenerator.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.kaap.invoicegenerator.data.InvoiceRow
import com.kaap.invoicegenerator.data.InvoiceTotals
import com.kaap.invoicegenerator.pdf.InvoicePdfGenerator
import com.kaap.invoicegenerator.ui.InvoiceViewModel
import com.kaap.invoicegenerator.utils.NumberToWords
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    viewModel: InvoiceViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val header by viewModel.header.collectAsState()
    val rows by viewModel.expandedRows.collectAsState()
    val totals by viewModel.totals.collectAsState()
    val scope = rememberCoroutineScope()

    var isGenerating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invoice Preview") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = {
                        isGenerating = true
                        scope.launch {
                            try {
                                val file = withContext(Dispatchers.IO) {
                                    InvoicePdfGenerator(context).generate(header, rows, totals)
                                }
                                sharePdf(context, file)
                            } catch (e: Exception) {
                                errorMessage = "Failed to generate PDF: ${e.message}"
                            } finally {
                                isGenerating = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = !isGenerating
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Generating PDF...")
                    } else {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Generate & Share PDF")
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("KAAP Hospitality & Consultancy Pvt. Ltd",
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        Text("TAX INVOICE", style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        InfoRow("Invoice No", header.invoiceNo)
                        InfoRow("Invoice Date", header.invoiceDate)
                        InfoRow("Letter No.", header.letterNo)
                        InfoRow("Order By", header.orderBy)
                        InfoRow("INV TYPE", "CREDIT")
                        InfoRow("State", "Bihar  |  Code: 10")
                    }
                }
            }

            item {
                Text("Line Items (${rows.size} rows)", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
            }

            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        Column {
                            TableHeader()
                            rows.forEachIndexed { index, row ->
                                TableRow(row = row, isEven = index % 2 == 0)
                            }
                            TotalRow(totals = totals)
                        }
                    }
                }
            }

            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Summary", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        HorizontalDivider()
                        SummaryRow("Total Amount Before Tax", "%.2f".format(totals.totalTaxableAmt))
                        SummaryRow("Add: CGST (2.5%)", "%.2f".format(totals.totalCgst))
                        SummaryRow("Add: SGST (2.5%)", "%.2f".format(totals.totalSgst))
                        SummaryRow("Prod DISC", "0.00")
                        SummaryRow("Tax Amount (GST)", "%.2f".format(totals.totalCgst + totals.totalSgst))
                        SummaryRow("Round Off", "%.2f".format(totals.roundOff))
                        HorizontalDivider()
                        SummaryRow(
                            label = "Gross Amount",
                            value = "%.0f".format(totals.grossAmt),
                            bold = true
                        )
                        Text(
                            text = "Rupees: ${NumberToWords.convert(totals.grossAmt)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Error") },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { errorMessage = null }) { Text("OK") } }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(100.dp))
        Text(text = ": $value", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SummaryRow(label: String, value: String, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(value, style = MaterialTheme.typography.bodySmall,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun TableHeader() {
    val headerBg = Color(0xFF1565C0)
    val textColor = Color.White
    Row(
        modifier = Modifier
            .background(headerBg)
            .padding(vertical = 4.dp)
    ) {
        TableCell("SL", 36.dp, textColor, bold = true)
        TableCell("PARTICULARS", 180.dp, textColor, bold = true)
        TableCell("HSN", 60.dp, textColor, bold = true)
        TableCell("QTY", 36.dp, textColor, bold = true)
        TableCell("RATE", 60.dp, textColor, bold = true)
        TableCell("AMT", 65.dp, textColor, bold = true)
        TableCell("DISC%", 44.dp, textColor, bold = true)
        TableCell("TAXABLE", 65.dp, textColor, bold = true)
        TableCell("CGST%", 44.dp, textColor, bold = true)
        TableCell("CGST", 52.dp, textColor, bold = true)
        TableCell("SGST%", 44.dp, textColor, bold = true)
        TableCell("SGST", 52.dp, textColor, bold = true)
        TableCell("NET AMT", 72.dp, textColor, bold = true)
    }
}

@Composable
private fun TableRow(row: InvoiceRow, isEven: Boolean) {
    val bg = if (isEven) Color(0xFFF8F9FF) else Color.White
    Row(modifier = Modifier.background(bg).padding(vertical = 3.dp)) {
        TableCell(row.slNo.toString(), 36.dp)
        TableCell(row.particularsText, 180.dp, align = TextAlign.Start)
        TableCell(row.hsnAcs, 60.dp)
        TableCell(row.qty.toString(), 36.dp)
        TableCell("%.2f".format(row.rate), 60.dp)
        TableCell("%.2f".format(row.amt), 65.dp)
        TableCell("%.2f".format(row.discPct), 44.dp)
        TableCell("%.2f".format(row.taxableAmt), 65.dp)
        TableCell("%.2f".format(row.cgstRate), 44.dp)
        TableCell("%.2f".format(row.cgstAmt), 52.dp)
        TableCell("%.2f".format(row.sgstRate), 44.dp)
        TableCell("%.2f".format(row.sgstAmt), 52.dp)
        TableCell("%.2f".format(row.netAmt), 72.dp)
    }
    HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE0E0E0))
}

@Composable
private fun TotalRow(totals: InvoiceTotals) {
    Row(
        modifier = Modifier
            .background(Color(0xFFE3F2FD))
            .padding(vertical = 5.dp)
    ) {
        TableCell("", 36.dp, bold = true)
        TableCell("TOTAL", 180.dp, bold = true, align = TextAlign.Center)
        TableCell("", 60.dp)
        TableCell("", 36.dp)
        TableCell("", 60.dp)
        TableCell("%.2f".format(totals.totalAmt), 65.dp, bold = true)
        TableCell("", 44.dp)
        TableCell("%.2f".format(totals.totalTaxableAmt), 65.dp, bold = true)
        TableCell("", 44.dp)
        TableCell("%.2f".format(totals.totalCgst), 52.dp, bold = true)
        TableCell("", 44.dp)
        TableCell("%.2f".format(totals.totalSgst), 52.dp, bold = true)
        TableCell("%.2f".format(totals.totalNet), 72.dp, bold = true)
    }
}

@Composable
private fun TableCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    color: Color = Color.Unspecified,
    bold: Boolean = false,
    align: TextAlign = TextAlign.Center
) {
    Text(
        text = text,
        modifier = Modifier
            .width(width)
            .padding(horizontal = 3.dp),
        fontSize = 10.sp,
        color = color,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        textAlign = align,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

private fun sharePdf(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val chooser = Intent.createChooser(intent, "Open Invoice PDF")
    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
}
