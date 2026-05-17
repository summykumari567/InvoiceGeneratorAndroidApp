package com.kaap.invoicegenerator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.kaap.invoicegenerator.data.InvoiceHeader
import com.kaap.invoicegenerator.data.LineItemGroup
import com.kaap.invoicegenerator.ui.InvoiceViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceFormScreen(
    viewModel: InvoiceViewModel,
    onPreview: () -> Unit
) {
    val header by viewModel.header.collectAsState()
    val lineItemGroups by viewModel.lineItemGroups.collectAsState()
    val expandedRows by viewModel.expandedRows.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingGroup by remember { mutableStateOf<LineItemGroup?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("KAAP Invoice Generator") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Line Item") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                HeaderSection(header = header, onHeaderChange = viewModel::updateHeader)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Line Items",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (expandedRows.isNotEmpty()) {
                        Text(
                            text = "${expandedRows.size} rows",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (lineItemGroups.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No line items yet. Tap + to add.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(lineItemGroups, key = { it.id }) { group ->
                LineItemGroupCard(
                    group = group,
                    onEdit = { editingGroup = group },
                    onDelete = { viewModel.removeLineItemGroup(group.id) }
                )
            }

            item { Spacer(Modifier.height(72.dp)) }

            item {
                Button(
                    onClick = onPreview,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = lineItemGroups.isNotEmpty() &&
                            header.invoiceNo.isNotBlank() &&
                            header.invoiceDate.isNotBlank()
                ) {
                    Text("Preview & Generate Invoice")
                }
            }
        }
    }

    if (showAddDialog) {
        LineItemDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = {
                viewModel.addLineItemGroup(it)
                showAddDialog = false
            }
        )
    }

    editingGroup?.let { grp ->
        LineItemDialog(
            group = grp,
            onDismiss = { editingGroup = null },
            onConfirm = {
                viewModel.updateLineItemGroup(it)
                editingGroup = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HeaderSection(
    header: InvoiceHeader,
    onHeaderChange: (InvoiceHeader) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var invoiceDateLocal by remember { mutableStateOf(
        try {
            val parts = header.invoiceDate.split("/")
            java.time.LocalDate.of(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
        } catch (_: Exception) { java.time.LocalDate.now() }
    ) }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "Invoice Header",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            OutlinedTextField(
                value = header.invoiceNo,
                onValueChange = { onHeaderChange(header.copy(invoiceNo = it)) },
                label = { Text("Invoice Number *") },
                placeholder = { Text("e.g. 238") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = header.invoiceDate,
                onValueChange = {},
                label = { Text("Invoice Date *") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    TextButton(onClick = { showDatePicker = true }) { Text("Pick") }
                }
            )
            OutlinedTextField(
                value = header.letterNo,
                onValueChange = { onHeaderChange(header.copy(letterNo = it)) },
                label = { Text("Letter Number") },
                placeholder = { Text("e.g. 1535/18/03/2026") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = header.orderBy,
                onValueChange = { onHeaderChange(header.copy(orderBy = it)) },
                label = { Text("Order By") },
                placeholder = { Text("e.g. Mr.Chet Narayan Rai") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showDatePicker) {
        DatePickerModal(
            initialDate = invoiceDateLocal,
            onDateSelected = {
                invoiceDateLocal = it
                val fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                onHeaderChange(header.copy(invoiceDate = it.format(fmt)))
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
private fun LineItemGroupCard(
    group: LineItemGroup,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val dayCount = (group.endDate.toEpochDay() - group.startDate.toEpochDay() + 1)
    val amt = group.quantity * group.rate
    val net = amt * 1.05  // 2.5% CGST + 2.5% SGST

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = buildString {
                        append(group.particulars)
                        if (group.subParticular.isNotBlank()) append(" (${group.subParticular})")
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${group.startDate.format(dateFmt)} → ${group.endDate.format(dateFmt)}  ($dayCount day${if (dayCount > 1) "s" else ""})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Qty: ${group.quantity}  Rate: %.2f  Net/day: %.2f".format(group.rate, net),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "HSN: ${group.hsnCode}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
