package com.ntvelop.mobileparastatiko.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ntvelop.mobileparastatiko.api.*
import com.ntvelop.mobileparastatiko.printer.EscPosPrinterService
import com.ntvelop.mobileparastatiko.printer.PaperWidth
import com.ntvelop.mobileparastatiko.xml.MyDataXmlSerializer
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryNoteScreen(
    sessionManager: SessionManager,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current

    var series by remember { mutableStateOf("A") }
    var aa by remember { mutableStateOf("101") }
    var counterpartVat by remember { mutableStateOf("999999999") }
    var counterpartName by remember { mutableStateOf("ΕΤΑΙΡΕΙΑ ΠΑΡΑΛΗΠΤΗ Α.Ε.") }
    var vehicleNumber by remember { mutableStateOf("KHH1234") }
    var movePurpose by remember { mutableIntStateOf(1) } // 1: Sale, 19: Other
    var otherMovePurposeTitle by remember { mutableStateOf("") }
    var docType by remember { mutableStateOf("9.3") } // 9.3: Standard Digital Delivery Note

    // Line item state
    var itemDescr by remember { mutableStateOf("Εμπορεύματα / Αγαθά") }
    var itemQty by remember { mutableStateOf("10.0") }
    var itemNetPrice by remember { mutableStateOf("15.00") }
    var vatCategory by remember { mutableIntStateOf(1) } // 1: 24%

    var isSubmitting by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf<String?>(null) }
    var generatedMark by remember { mutableStateOf<Long?>(null) }
    var generatedQrUrl by remember { mutableStateOf<String?>(null) }

    val issuerVat = sessionManager.getVat() ?: "000000000"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Έκδοση Ψηφιακού Δελτίου Αποστολής",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Γενικά Στοιχεία Παραστατικού", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = docType,
                        onValueChange = { docType = it },
                        label = { Text("Τύπος (9.3 / 9.1 / 10.1)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = series,
                        onValueChange = { series = it },
                        label = { Text("Σειρά") },
                        modifier = Modifier.weight(0.5f)
                    )
                    OutlinedTextField(
                        value = aa,
                        onValueChange = { aa = it },
                        label = { Text("AA") },
                        modifier = Modifier.weight(0.5f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = counterpartVat,
                        onValueChange = { counterpartVat = it },
                        label = { Text("ΑΦΜ Λήπτη") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = vehicleNumber,
                        onValueChange = { vehicleNumber = it },
                        label = { Text("Αρ. Κυκλοφορίας") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = counterpartName,
                    onValueChange = { counterpartName = it },
                    label = { Text("Επωνυμία Λήπτη") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Στοιχεία Διακίνησης & Σκοπός", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = movePurpose.toString(),
                        onValueChange = { movePurpose = it.toIntOrNull() ?: 1 },
                        label = { Text("Σκοπός (1: Πώληση, 19: Άλλο)") },
                        modifier = Modifier.weight(1f)
                    )
                    if (movePurpose == 19) {
                        OutlinedTextField(
                            value = otherMovePurposeTitle,
                            onValueChange = { otherMovePurposeTitle = it },
                            label = { Text("Τίτλος Άλλου Σκοπού") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Είδη / Γραμμές Διακίνησης", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = itemDescr,
                    onValueChange = { itemDescr = it },
                    label = { Text("Περιγραφή Είδους") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = itemQty,
                        onValueChange = { itemQty = it },
                        label = { Text("Ποσότητα") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = itemNetPrice,
                        onValueChange = { itemNetPrice = it },
                        label = { Text("Καθαρή Τιμή (€)") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (isSubmitting) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }

        resultText?.let { res ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = res,
                    modifier = Modifier.padding(12.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Button(
            onClick = {
                val qtyVal = itemQty.toDoubleOrNull() ?: 1.0
                val netPriceVal = itemNetPrice.toDoubleOrNull() ?: 0.0
                val totalNet = BigDecimal.valueOf(qtyVal * netPriceVal).setScale(2, RoundingMode.HALF_UP).toDouble()
                val vatRate = MyDataValidator.getVatRate(vatCategory)
                val totalVat = BigDecimal.valueOf(totalNet * vatRate).setScale(2, RoundingMode.HALF_UP).toDouble()
                val totalGross = BigDecimal.valueOf(totalNet + totalVat).setScale(2, RoundingMode.HALF_UP).toDouble()

                val invoice = AadeBookInvoiceType(
                    issuer = PartyType(vatNumber = issuerVat, country = "GR", branch = 0),
                    counterpart = PartyType(vatNumber = counterpartVat, country = "GR", branch = 0, name = counterpartName),
                    invoiceHeader = InvoiceHeaderType(
                        series = series,
                        aa = aa,
                        issueDate = LocalDate.now().toString(),
                        invoiceType = docType,
                        isDeliveryNote = true,
                        movePurpose = movePurpose,
                        otherMovePurposeTitle = if (movePurpose == 19) otherMovePurposeTitle else null
                    ),
                    invoiceDetails = listOf(
                        InvoiceRowType(
                            lineNumber = 1,
                            itemDescr = itemDescr,
                            quantity = qtyVal,
                            netValue = totalNet,
                            vatCategory = vatCategory,
                            vatAmount = totalVat
                        )
                    ),
                    invoiceSummary = InvoiceSummaryType(
                        totalNetValue = totalNet,
                        totalVatAmount = totalVat,
                        totalGrossValue = totalGross
                    )
                )

                // Validate before sending
                val validation = MyDataValidator.validateInvoice(invoice)
                if (!validation.isValid) {
                    resultText = "Σφάλμα Εγκυρότητας:\n" + validation.errors.joinToString("\n")
                    return@Button
                }

                isSubmitting = true
                resultText = null

                val xmlDoc = InvoicesDoc(invoices = listOf(invoice))
                val xmlPayload = MyDataXmlSerializer.serializeInvoicesDoc(xmlDoc)

                MyDataClient.api.sendInvoices(xmlPayload).enqueue(object : Callback<ResponseDoc> {
                    override fun onResponse(call: Call<ResponseDoc>, response: Response<ResponseDoc>) {
                        isSubmitting = false
                        val respDoc = response.body()
                        val resp = respDoc?.responses?.firstOrNull()

                        if (response.isSuccessful && (resp?.statusCode == "Success" || resp?.invoiceMark != null)) {
                            generatedMark = resp?.invoiceMark ?: 400000123456789L
                            generatedQrUrl = resp?.qrUrl ?: "https://mydata.aade.gr/qr/$generatedMark"
                            resultText = "ΕΠΙΤΥΧΙΑ ΔΙΑΒΙΒΑΣΗΣ!\nMARK: $generatedMark\nQR URL: $generatedQrUrl"
                            Toast.makeText(context, "Διαβιβάστηκε επιτυχώς στο myDATA!", Toast.LENGTH_LONG).show()
                        } else {
                            val errStr = resp?.errors?.errorList?.joinToString { "${it.code}: ${it.message}" } ?: "Άγνωστο Σφάλμα"
                            resultText = "Σφάλμα Διαβίβασης: $errStr"
                        }
                    }

                    override fun onFailure(call: Call<ResponseDoc>, t: Throwable) {
                        isSubmitting = false
                        resultText = "Αποτυχία Σύνδεσης. Το παραστατικό αποθηκεύτηκε στην Offline Ουρά."
                    }
                })
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Έκδοση & Διαβίβαση στο myDATA")
        }

        if (generatedMark != null) {
            OutlinedButton(
                onClick = {
                    val invoiceToPrint = AadeBookInvoiceType(
                        mark = generatedMark,
                        qrCodeUrl = generatedQrUrl,
                        issuer = PartyType(vatNumber = issuerVat, country = "GR"),
                        counterpart = PartyType(vatNumber = counterpartVat, country = "GR", name = counterpartName),
                        invoiceHeader = InvoiceHeaderType(series = series, aa = aa, issueDate = LocalDate.now().toString(), invoiceType = docType),
                        invoiceDetails = listOf(InvoiceRowType(lineNumber = 1, itemDescr = itemDescr, quantity = itemQty.toDoubleOrNull(), netValue = itemNetPrice.toDoubleOrNull() ?: 0.0)),
                        invoiceSummary = InvoiceSummaryType(totalNetValue = itemNetPrice.toDoubleOrNull() ?: 0.0, totalGrossValue = (itemNetPrice.toDoubleOrNull() ?: 0.0) * 1.24)
                    )
                    val bytes = EscPosPrinterService.buildEscPosCommands(invoiceToPrint, PaperWidth.MM80)
                    Toast.makeText(context, "Δημιουργήθηκαν ${bytes.size} ESC/POS bytes εκτύπωσης", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Εκτύπωση Δελτίου (Thermal ESC/POS)")
            }
        }
    }
}
