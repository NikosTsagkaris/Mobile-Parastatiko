package com.ntvelop.mobileparastatiko.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ntvelop.mobileparastatiko.ui.theme.DarkSurface
import com.ntvelop.mobileparastatiko.ui.theme.NeonGreen

data class PrintOptions(
    val printQrCode: Boolean = true,
    val printItemsList: Boolean = true,
    val printCarrierDetails: Boolean = true,
    val paperSize: String = "58mm" // "58mm" or "80mm"
)

@Composable
fun ThermalPrintDialog(
    docMark: Long,
    onDismiss: () -> Unit,
    onPrintConfirmed: (PrintOptions) -> Unit
) {
    var printQrCode by remember { mutableStateOf(true) }
    var printItemsList by remember { mutableStateOf(true) }
    var printCarrierDetails by remember { mutableStateOf(true) }
    var paperSize by remember { mutableStateOf("58mm") }

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
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Εκτύπωση Παραστατικού",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreen
                        )
                        Text(
                            text = "MARK: ${if (docMark != 0L) docMark else "Εκκρεμεί"}",
                            fontSize = 12.sp,
                            color = Color.LightGray
                        )
                    }
                    Icon(
                        Icons.Default.Print,
                        contentDescription = null,
                        tint = NeonGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    thickness = 0.5.dp,
                    color = Color.DarkGray
                )

                Text(
                    text = "Μέγεθος Χαρτιού Εκτυπωτή:",
                    fontSize = 12.sp,
                    color = Color.LightGray
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = paperSize == "58mm",
                        onClick = { paperSize = "58mm" },
                        label = { Text("58mm (Φορητός)") }
                    )
                    FilterChip(
                        selected = paperSize == "80mm",
                        onClick = { paperSize = "80mm" },
                        label = { Text("80mm (Desktop)") }
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Επιλογές Περιεχομένου
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = printQrCode,
                        onCheckedChange = { printQrCode = it }
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Εκτύπωση AADE QR Code", fontSize = 13.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = printItemsList,
                        onCheckedChange = { printItemsList = it }
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Αναλυτική λίστα ειδών & ποσοτήτων", fontSize = 13.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = printCarrierDetails,
                        onCheckedChange = { printCarrierDetails = it }
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Στοιχεία οχήματος & μεταφορέα", fontSize = 13.sp)
                }

                Spacer(Modifier.height(18.dp))

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
                            onPrintConfirmed(
                                PrintOptions(
                                    printQrCode = printQrCode,
                                    printItemsList = printItemsList,
                                    printCarrierDetails = printCarrierDetails,
                                    paperSize = paperSize
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black)
                    ) {
                        Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("ΕΚΤΥΠΩΣΗ", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}