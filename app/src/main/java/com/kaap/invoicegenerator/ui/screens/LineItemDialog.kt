package com.kaap.invoicegenerator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kaap.invoicegenerator.data.LineItemGroup
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LineItemDialog(
    group: LineItemGroup? = null,
    onDismiss: () -> Unit,
    onConfirm: (LineItemGroup) -> Unit
) {
    val isEdit = group != null
    val dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    var particulars by remember { mutableStateOf(group?.particulars ?: "") }
    var subParticular by remember { mutableStateOf(group?.subParticular ?: "") }
    var hsnCode by remember { mutableStateOf(group?.hsnCode ?: "996337") }
    var quantity by remember { mutableStateOf(group?.quantity?.toString() ?: "") }
    var rate by remember { mutableStateOf(group?.rate?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    var startDate by remember { mutableStateOf(group?.startDate ?: LocalDate.now()) }
    var endDate by remember { mutableStateOf(group?.endDate ?: LocalDate.now()) }

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    var particularsError by remember { mutableStateOf(false) }
    var quantityError by remember { mutableStateOf(false) }
    var rateError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Edit Line Item" else "Add Line Item") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = particulars,
                    onValueChange = { particulars = it; particularsError = false },
                    label = { Text("Particulars *") },
                    placeholder = { Text("e.g. LDC") },
                    isError = particularsError,
                    supportingText = if (particularsError) { { Text("Required") } } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = subParticular,
                    onValueChange = { subParticular = it },
                    label = { Text("Sub-Particular (optional)") },
                    placeholder = { Text("e.g. Breakfast, Dinner") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = hsnCode,
                    onValueChange = { hsnCode = it },
                    label = { Text("HSN Code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startDate.format(dateFmt),
                        onValueChange = {},
                        label = { Text("Start Date") },
                        readOnly = true,
                        modifier = Modifier
                            .weight(1f),
                        trailingIcon = {
                            TextButton(onClick = { showStartPicker = true }) {
                                Text("Pick")
                            }
                        }
                    )
                    OutlinedTextField(
                        value = endDate.format(dateFmt),
                        onValueChange = {},
                        label = { Text("End Date") },
                        readOnly = true,
                        modifier = Modifier.weight(1f),
                        trailingIcon = {
                            TextButton(onClick = { showEndPicker = true }) {
                                Text("Pick")
                            }
                        }
                    )
                }

                val dayCount = if (!startDate.isAfter(endDate))
                    (endDate.toEpochDay() - startDate.toEpochDay() + 1).toString()
                else "Invalid range"
                Text(
                    text = "Days: $dayCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (startDate.isAfter(endDate))
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it; quantityError = false },
                        label = { Text("Quantity *") },
                        isError = quantityError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = rate,
                        onValueChange = { rate = it; rateError = false },
                        label = { Text("Rate *") },
                        isError = rateError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                val qty = quantity.toIntOrNull() ?: 0
                val r = rate.toDoubleOrNull() ?: 0.0
                if (qty > 0 && r > 0) {
                    val amt = qty * r
                    val cgst = amt * 0.025
                    val sgst = amt * 0.025
                    val net = amt + cgst + sgst
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Per day preview:", style = MaterialTheme.typography.labelSmall)
                            Text("AMT: %.2f  CGST: %.2f  SGST: %.2f  NET: %.2f".format(amt, cgst, sgst, net),
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                particularsError = particulars.isBlank()
                quantityError = quantity.toIntOrNull() == null || quantity.toInt() <= 0
                rateError = rate.toDoubleOrNull() == null
                val dateError = startDate.isAfter(endDate)
                if (!particularsError && !quantityError && !rateError && !dateError) {
                    onConfirm(
                        (group ?: LineItemGroup()).copy(
                            particulars = particulars.trim(),
                            subParticular = subParticular.trim(),
                            hsnCode = hsnCode.trim().ifBlank { "996337" },
                            startDate = startDate,
                            endDate = endDate,
                            quantity = quantity.toInt(),
                            rate = rate.toDouble()
                        )
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showStartPicker) {
        DatePickerModal(
            initialDate = startDate,
            onDateSelected = { startDate = it; showStartPicker = false },
            onDismiss = { showStartPicker = false }
        )
    }

    if (showEndPicker) {
        DatePickerModal(
            initialDate = endDate,
            onDateSelected = {
                endDate = it
                if (it.isBefore(startDate)) startDate = it
                showEndPicker = false
            },
            onDismiss = { showEndPicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModal(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.toEpochDay() * 86_400_000L
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = state.selectedDateMillis
                if (millis != null) {
                    onDateSelected(LocalDate.ofEpochDay(millis / 86_400_000L))
                } else {
                    onDismiss()
                }
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) {
        DatePicker(state = state)
    }
}
