package com.ntvelop.mobileparastatiko.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ntvelop.mobileparastatiko.api.MyDataValidator
import com.ntvelop.mobileparastatiko.ui.theme.DarkSurface
import com.ntvelop.mobileparastatiko.ui.theme.NeonGreen

data class TriangularTransferData(
    val entityType: Int = 4, // 4: Τελικός Παραλήπτης Αγαθών, 2: Ενδιάμεσος, 3: Μεταφορέας
    val thirdPartyVat: String,
    val branch: Int = 0,
    val deliveryAddress: String,
    val deliveryPostalCode: String,
    val deliveryCity: String,
    val transportType: Int = 1
)

@Composable
fun TriangularTransferDialog(
    onDismiss: () -> Unit,
    onConfirm: (TriangularTransferData) -> Unit
) {
    var entityType by remember { mutableIntStateOf(4) }
    var vat by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("0") }
    var address by remember { mutableStateOf("") }
    var postalCode by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var transportType by remember { mutableIntStateOf(1) }

    val isVatValid = remember(vat) {
        if (vat.length == 9) MyDataValidator.isValidGreekVat(vat) else false
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    text = "Τριγωνική Διακίνηση / Συσχετιζόμενος Φορέας",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen
                )
                Text(
                    text = "Ορισμός εγκατάστασης παράδοσης τρίτου (myData EntityType)",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Text("Ρόλος Φορέα:", fontSize = 12.sp, color = Color.LightGray)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = entityType == 4,
                        onClick = { entityType = 4 },
                        label = { Text("4 - Παραλήπτης", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = entityType == 2,
                        onClick = { entityType = 2 },
                        label = { Text("2 - Ενδιάμεσος", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = entityType == 3,
                        onClick = { entityType = 3 },
                        label = { Text("3 - Μεταφορέας", fontSize = 11.sp) }
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = vat,
                        onValueChange = { if (it.length <= 9) vat = it },
                        label = { Text("ΑΦΜ Φορέα (9 ψηφία)") },
                        isError = vat.length == 9 && !isVatValid,
                        supportingText = {
                            if (vat.length == 9 && !isVatValid) {
                                Text("Μη έγκυρο ΑΦΜ (Modulo 11)", color = Color.Red, fontSize = 10.sp)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(2f),
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = branch,
                        onValueChange = { branch = it },
                        label = { Text("Εγκατάσταση") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Διεύθυνση Παράδοσης / Οδός") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = postalCode,
                        onValueChange = { postalCode = it },
                        label = { Text("Τ.Κ.") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("Πόλη") },
                        modifier = Modifier.weight(1.5f),
                        singleLine = true
                    )
                }

                Spacer(Modifier.height(12.dp))
                Text("Μέσο Μεταφοράς:", fontSize = 12.sp, color = Color.LightGray)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = transportType == 1,
                        onClick = { transportType = 1 },
                        label = { Text("1 (Ι.Χ.)") }
                    )
                    FilterChip(
                        selected = transportType == 5,
                        onClick = { transportType = 5 },
                        label = { Text("5 (Δ.Χ.)") }
                    )
                    FilterChip(
                        selected = transportType == 7,
                        onClick = { transportType = 7 },
                        label = { Text("7 (Courier)") }
                    )
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("ΑΚΥΡΩΣΗ", color = Color.LightGray)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirm(
                                TriangularTransferData(
                                    entityType = entityType,
                                    thirdPartyVat = vat.trim(),
                                    branch = branch.toIntOrNull() ?: 0,
                                    deliveryAddress = address.trim(),
                                    deliveryPostalCode = postalCode.trim(),
                                    deliveryCity = city.trim(),
                                    transportType = transportType
                                )
                            )
                        },
                        enabled = isVatValid,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black)
                    ) {
                        Text("ΑΠΟΣΤΟΛΗ", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}