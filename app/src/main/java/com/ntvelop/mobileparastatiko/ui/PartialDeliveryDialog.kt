package com.ntvelop.mobileparastatiko.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ntvelop.mobileparastatiko.ui.theme.NeonGreen

data class DiscrepancyItem(
    val itemName: String = "",
    val quantity: Double = 0.0,
    val isDeficit: Boolean = true // true = Έλλειμμα (recType 7), false = Πλεόνασμα
)

@Composable
fun PartialDeliveryDialog(
    onDismiss: () -> Unit,
    onConfirm: (List<DiscrepancyItem>, withoutRecipient: Boolean) -> Unit
) {
    val items = remember { mutableStateListOf<DiscrepancyItem>() }
    var withoutRecipient by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Καταγραφή Μερικής Παραλαβής",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen
                )
                Text(
                    text = "Δηλώστε τα ελλείμματα ή πλεονάζοντα είδη (myData ΔΠΠ 10.1)",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Checkbox(
                        checked = withoutRecipient,
                        onCheckedChange = { withoutRecipient = it }
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Παράδοση χωρίς παρουσία παραλήπτη", fontSize = 13.sp)
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    itemsIndexed(items) { index, item ->
                        DiscrepancyRow(
                            item = item,
                            onUpdate = { updated -> items[index] = updated },
                            onDelete = { items.removeAt(index) }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }

                OutlinedButton(
                    onClick = { items.add(DiscrepancyItem()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = NeonGreen)
                    Spacer(Modifier.width(8.dp))
                    Text("Προσθήκη Είδους", color = NeonGreen)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("ΑΚΥΡΩΣΗ", color = Color.LightGray)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(items.toList(), withoutRecipient) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black)
                    ) {
                        Text("ΕΠΙΒΕΒΑΙΩΣΗ", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscrepancyRow(
    item: DiscrepancyItem,
    onUpdate: (DiscrepancyItem) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            OutlinedTextField(
                value = item.itemName,
                onValueChange = { onUpdate(item.copy(itemName = it)) },
                label = { Text("Περιγραφή Είδους / Κωδικός", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = if (item.quantity == 0.0) "" else item.quantity.toString(),
                    onValueChange = { onUpdate(item.copy(quantity = it.toDoubleOrNull() ?: 0.0)) },
                    label = { Text("Ποσότητα", fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = item.isDeficit,
                    onClick = { onUpdate(item.copy(isDeficit = !item.isDeficit)) },
                    label = { Text(if (item.isDeficit) "Έλλειμμα (7)" else "Πλεόνασμα", fontSize = 11.sp) }
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Διαγραφή", tint = Color.Red.copy(alpha = 0.7f))
                }
            }
        }
    }
}