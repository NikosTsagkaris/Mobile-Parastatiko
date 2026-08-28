package com.ntvelop.mobileparastatiko.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ntvelop.mobileparastatiko.api.*
import com.ntvelop.mobileparastatiko.offline.OfflineQueueManager
import com.ntvelop.mobileparastatiko.printer.EscPosPrinterService
import com.ntvelop.mobileparastatiko.printer.PaperWidth
import com.ntvelop.mobileparastatiko.ui.theme.DarkBg
import com.ntvelop.mobileparastatiko.ui.theme.DarkSurface
import com.ntvelop.mobileparastatiko.ui.theme.NeonGreen
import com.ntvelop.mobileparastatiko.xml.MyDataXmlSerializer
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

data class DeliveryNoteItemRow(
    val description: String = "Εμπορεύματα / Αγαθά",
    val quantity: Double = 1.0,
    val netPrice: Double = 10.0,
    val measurementUnit: Int = 1, // 1: Τεμάχια, 2: Κιλά, 3: Λίτρα, 4: Μέτρα
    val vatCategory: Int = 1 // 1: 24%, 2: 13%, 3: 6%, 7: 0%
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryNoteScreen(
    sessionManager: SessionManager,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val offlineQueue = remember { OfflineQueueManager(context) }

    var series by remember { mutableStateOf("A") }
    var aa by remember { mutableStateOf("101") }
    var counterpartVat by remember { mutableStateOf("999999999") }
    var counterpartName by remember { mutableStateOf("ΕΤΑΙΡΕΙΑ ΠΑΡΑΛΗΠΤΗ Α.Ε.") }
    var vehicleNumber by remember { mutableStateOf("KHH1234") }
    var docType by remember { mutableStateOf("9.3") }

    val isVatValid = remember(counterpartVat) {
        if (counterpartVat.length == 9) MyDataValidator.isValidGreekVat(counterpartVat) else false
    }

    // Σκοπός Διακίνησης ΑΑΔΕ
    var movePurpose by remember { mutableIntStateOf(1) }
    var otherMovePurposeTitle by remember { mutableStateOf("") }
    var expandedMovePurpose by remember { mutableStateOf(false) }

    val movePurposeOptions = listOf(
        1 to "1: Πώληση",
        2 to "2: Πώληση για λογ/σμό τρίτου",
        3 to "3: Δειγματισμός",
        4 to "4: Έκθεση",
        5 to "5: Επιστροφή",
        6 to "6: Επεξεργασία / Συναρμολόγηση",
        7 to "7: Ενδοδιακίνηση",
        8 to "8: Αποθήκευση σε τρίτους",
        9 to "9: Χρησιδανεισμός",
        19 to "19: Λοιπές Διακινήσεις"
    )

    // Λίστα Γραμμών Ειδών
    val items = remember {
        mutableStateListOf(
            DeliveryNoteItemRow(description = "Εμπορεύματα", quantity = 10.0, netPrice = 15.00, measurementUnit = 1, vatCategory = 1)
        )
    }

    var isSubmitting by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf<String?>(null) }
    var generatedMark by remember { mutableStateOf<Long?>(null) }
    var generatedQrUrl by remember { mutableStateOf<String?>(null) }

    val issuerVat = sessionManager.getVat() ?: "000000000"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Έκδοση Ψηφιακού Δελτίου", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Πίσω", tint = NeonGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = DarkBg
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Στοιχεία Παραστατικού & Οχήματος
            item {
                Card(colors = CardDefaults.cardColors(containerColor = DarkSurface)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Γενικά Στοιχεία & Όχημα", fontWeight = FontWeight.Bold, color = NeonGreen)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = docType,
                                onValueChange = { docType = it },
                                label = { Text("Τύπος") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = series,
                                onValueChange = { series = it },
                                label = { Text("Σειρά") },
                                modifier = Modifier.weight(0.6f)
                            )
                            OutlinedTextField(
                                value = aa,
                                onValueChange = { aa = it },
                                label = { Text("Α/Α") },
                                modifier = Modifier.weight(0.6f)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = counterpartVat,
                                onValueChange = { if (it.length <= 9) counterpartVat = it },
                                label = { Text("ΑΦΜ Λήπτη") },
                                isError = counterpartVat.length == 9 && !isVatValid,
                                supportingText = {
                                    if (counterpartVat.length == 9 && !isVatValid) {
                                        Text("Μη έγκυρο ΑΦΜ (Modulo 11)", color = Color.Red, fontSize = 10.sp)
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = vehicleNumber,
                                onValueChange = { vehicleNumber = it.uppercase() },
                                label = { Text("Πινακίδα Οχήματος") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        OutlinedTextField(
                            value = counterpartName,
                            onValueChange = { counterpartName = it },
                            label = { Text("Επωνυμία Λήπτη") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }

            // Σκοπός Διακίνησης (Dropdown)
            item {
                Card(colors = CardDefaults.cardColors(containerColor = DarkSurface)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Σκοπός Διακίνησης (myDATA)", fontWeight = FontWeight.Bold, color = NeonGreen)
                        
                        ExposedDropdownMenuBox(
                            expanded = expandedMovePurpose,
                            onExpandedChange = { expandedMovePurpose = !expandedMovePurpose }
                        ) {
                            OutlinedTextField(
                                value = movePurposeOptions.firstOrNull { it.first == movePurpose }?.second ?: "Επιλογή",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Σκοπός Μεταφοράς") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMovePurpose) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedMovePurpose,
                                onDismissRequest = { expandedMovePurpose = false }
                            ) {
                                movePurposeOptions.forEach { (code, title) ->
                                    DropdownMenuItem(
                                        text = { Text(title) },
                                        onClick = {
                                            movePurpose = code
                                            expandedMovePurpose = false
                                        }
                                    )
                                }
                            }
                        }

                        if (movePurpose == 19) {
                            OutlinedTextField(
                                value = otherMovePurposeTitle,
                                onValueChange = { otherMovePurposeTitle = it },
                                label = { Text("Περιγραφή Άλλου Σκοπού") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Επικεφαλίδα Ειδών
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Είδη Παραστατικού (${items.size})", fontWeight = FontWeight.Bold, color = NeonGreen)
                    TextButton(onClick = { items.add(DeliveryNoteItemRow()) }) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = NeonGreen)
                        Spacer(Modifier.width(4.dp))
                        Text("Προσθήκη Είδους", color = NeonGreen)
                    }
                }
            }

            // Λίστα Γραμμών
            itemsIndexed(items) { index, item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = item.description,
                                onValueChange = { items[index] = item.copy(description = it) },
                                label = { Text("Περιγραφή Είδους #${index + 1}") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            if (items.size > 1) {
                                IconButton(onClick = { items.removeAt(index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Διαγραφή", tint = Color.Red.copy(alpha = 0.7f))
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = if (item.quantity == 0.0) "" else item.quantity.toString(),
                                onValueChange = { items[index] = item.copy(quantity = it.toDoubleOrNull() ?: 0.0) },
                                label = { Text("Ποσότητα") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = if (item.netPrice == 0.0) "" else item.netPrice.toString(),
                                onValueChange = { items[index] = item.copy(netPrice = it.toDoubleOrNull() ?: 0.0) },
                                label = { Text("Τιμή Μον. (€)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            // Feedback / Result Text
            item {
                if (isSubmitting) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NeonGreen)
                    }
                }

                resultText?.let { res ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = res,
                            modifier = Modifier.padding(12.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (res.startsWith("ΕΠΙΤΥΧΙΑ")) NeonGreen else Color.Red
                        )
                    }
                }
            }

            // Κουμπιά Ενεργειών
            item {
                Button(
                    onClick = {
                        var totalNet = 0.0
                        var totalVat = 0.0

                        val invoiceRows = items.mapIndexed { idx, itm ->
                            val lineNet = BigDecimal.valueOf(itm.quantity * itm.netPrice).setScale(2, RoundingMode.HALF_UP).toDouble()
                            val vatRate = MyDataValidator.getVatRate(itm.vatCategory)
                            val lineVat = BigDecimal.valueOf(lineNet * vatRate).setScale(2, RoundingMode.HALF_UP).toDouble()

                            totalNet += lineNet
                            totalVat += lineVat

                            InvoiceRowType(
                                lineNumber = idx + 1,
                                itemDescr = itm.description,
                                quantity = itm.quantity,
                                measurementUnit = itm.measurementUnit,
                                netValue = lineNet,
                                vatCategory = itm.vatCategory,
                                vatAmount = lineVat
                            )
                        }

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
                            invoiceDetails = invoiceRows,
                            invoiceSummary = InvoiceSummaryType(
                                totalNetValue = BigDecimal.valueOf(totalNet).setScale(2, RoundingMode.HALF_UP).toDouble(),
                                totalVatAmount = BigDecimal.valueOf(totalVat).setScale(2, RoundingMode.HALF_UP).toDouble(),
                                totalGrossValue = totalGross
                            )
                        )

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
                                val resp = response.body()?.responses?.firstOrNull()

                                if (response.isSuccessful && (resp?.statusCode == "Success" || resp?.invoiceMark != null)) {
                                    generatedMark = resp?.invoiceMark ?: 400000123456789L
                                    generatedQrUrl = resp?.qrUrl ?: "https://mydata.aade.gr/qr/$generatedMark"
                                    resultText = "ΕΠΙΤΥΧΙΑ ΔΙΑΒΙΒΑΣΗΣ!\nMARK: $generatedMark\nQR: $generatedQrUrl"
                                    Toast.makeText(context, "Διαβιβάστηκε επιτυχώς στο myDATA!", Toast.LENGTH_LONG).show()
                                } else {
                                    val errStr = resp?.errors?.errorList?.joinToString { "${it.code}: ${it.message}" } ?: "Άγνωστο Σφάλμα"
                                    resultText = "Σφάλμα Διαβίβασης: $errStr"
                                }
                            }

                            override fun onFailure(call: Call<ResponseDoc>, t: Throwable) {
                                isSubmitting = false
                                offlineQueue.enqueueInvoice(invoice)
                                resultText = "Αποτυχία Σύνδεσης. Το παραστατικό αποθηκεύτηκε στην Offline Ουρά (transmissionFailure = 3)."
                                Toast.makeText(context, "Αποθηκεύτηκε στην Offline Ουρά!", Toast.LENGTH_LONG).show()
                            }
                        })
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black)
                ) {
                    Text("Έκδοση & Διαβίβαση στο myDATA", fontWeight = FontWeight.Bold)
                }

                if (generatedMark != null) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            val invoiceToPrint = AadeBookInvoiceType(
                                mark = generatedMark,
                                qrCodeUrl = generatedQrUrl,
                                issuer = PartyType(vatNumber = issuerVat, country = "GR"),
                                counterpart = PartyType(vatNumber = counterpartVat, country = "GR", name = counterpartName),
                                invoiceHeader = InvoiceHeaderType(series = series, aa = aa, issueDate = LocalDate.now().toString(), invoiceType = docType),
                                invoiceDetails = items.mapIndexed { idx, itm ->
                                    InvoiceRowType(lineNumber = idx + 1, itemDescr = itm.description, quantity = itm.quantity, netValue = itm.netPrice * itm.quantity)
                                },
                                invoiceSummary = InvoiceSummaryType(totalNetValue = 0.0, totalGrossValue = 0.0)
                            )
                            val bytes = EscPosPrinterService.buildEscPosCommands(invoiceToPrint, PaperWidth.MM80)
                            Toast.makeText(context, "Δημιουργήθηκαν ${bytes.size} ESC/POS bytes εκτύπωσης", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text("Εκτύπωση Δελτίου (Thermal ESC/POS)", color = NeonGreen)
                    }
                }
            }
        }
    }
}