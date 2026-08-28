package com.ntvelop.mobileparastatiko

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.ntvelop.mobileparastatiko.api.DeliveryStatus
import com.ntvelop.mobileparastatiko.api.GetDeliveryStatusResponse
import com.ntvelop.mobileparastatiko.api.RequestedInvoicesDoc
import com.ntvelop.mobileparastatiko.api.MyDataClient
import com.ntvelop.mobileparastatiko.api.RegisterTransferRequest
import com.ntvelop.mobileparastatiko.api.ConfirmDeliveryOutcomeRequest
import com.ntvelop.mobileparastatiko.api.RejectDeliveryNoteRequest
import com.ntvelop.mobileparastatiko.api.TransportDetailRequest
import com.ntvelop.mobileparastatiko.api.ResponseDoc
import com.ntvelop.mobileparastatiko.api.GroupQRCodeRequest
import com.ntvelop.mobileparastatiko.api.QrUrlsWrapper
import com.ntvelop.mobileparastatiko.api.CancelDeliveryNoteRequest
import com.ntvelop.mobileparastatiko.api.SessionManager
import com.ntvelop.mobileparastatiko.xml.MyDataXmlSerializer
import com.ntvelop.mobileparastatiko.ui.DeliveryDetailsCard
import com.ntvelop.mobileparastatiko.ui.PartialDeliveryDialog
import com.ntvelop.mobileparastatiko.ui.TriangularTransferDialog
import com.ntvelop.mobileparastatiko.ui.BatchScanListDialog
import com.ntvelop.mobileparastatiko.ui.scanner.QRScannerScreen
import com.ntvelop.mobileparastatiko.ui.screens.LoginScreen
import com.ntvelop.mobileparastatiko.ui.screens.SplashScreen
import com.ntvelop.mobileparastatiko.ui.screens.SettingsScreen
import com.ntvelop.mobileparastatiko.ui.theme.DarkBg
import com.ntvelop.mobileparastatiko.ui.theme.DarkSurface
import com.ntvelop.mobileparastatiko.ui.theme.MobileParastatikoTheme
import com.ntvelop.mobileparastatiko.ui.theme.NeonGreen
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import okhttp3.OkHttpClient
import okhttp3.Request

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sessionManager = SessionManager(this)
        MyDataClient.sessionManager = sessionManager

        enableEdgeToEdge()
        setContent {
            MobileParastatikoTheme {
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize(), containerColor = DarkBg) { innerPadding ->
                    NavHost(navController = navController, startDestination = "splash", modifier = Modifier.padding(innerPadding)) {
                        composable("splash") { SplashScreen { isLoggedIn -> navController.navigate(if (isLoggedIn) "main" else "login") { popUpTo("splash") { inclusive = true } } } }
                        composable("login") { LoginScreen { navController.navigate("main") { popUpTo("login") { inclusive = true } } } }
                        composable("main") { MainDashboardScreen(navController, sessionManager) }
                        composable("delivery") { com.ntvelop.mobileparastatiko.ui.screens.DeliveryNoteScreen(sessionManager) { navController.popBackStack() } }
                        composable("settings") { SettingsScreen(sessionManager) { navController.popBackStack() } }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainDashboardScreen(navController: NavController, sessionManager: SessionManager) {
    var statusMessage by remember { mutableStateOf("") }
    var currentStatus by remember { mutableStateOf(DeliveryStatus.Unknown) }
    var currentQrUrl by remember { mutableStateOf("") }
    var currentMark by remember { mutableLongStateOf(0L) }
    var vehiclePlateNumber by remember { mutableStateOf("Ανευ Οχήματος") }
    var rawDebug by remember { mutableStateOf("") }
    val debugLog = remember { StringBuilder() }

    var showScanner by remember { mutableStateOf(false) }
    var isIssuer by remember { mutableStateOf(false) }

    var showRegisterDialog by remember { mutableStateOf(false) }
    var showTriangularDialog by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showPartialDeliveryDialog by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var showManualMarkDialog by remember { mutableStateOf(false) }
    var showDebugDialog by remember { mutableStateOf(false) }
    var showBatchScanListDialog by remember { mutableStateOf(false) }

    val groupedQrUrls = remember { mutableStateListOf<String>() }
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    if (showScanner) {
        if (cameraPermissionState.status.isGranted) {
            QRScannerScreen(onQrDetected = { qrUrl ->
                showScanner = false
                val sanitizedUrl = sanitizeQrUrl(qrUrl)
                currentQrUrl = sanitizedUrl

                val docId = extractHash(sanitizedUrl) ?: "global"
                currentMark = sessionManager.getDocumentMark(docId)

                currentStatus = DeliveryStatus.Unknown
                debugLog.setLength(0)
                debugLog.append("Scanned. Start Recovery...\n")

                val loggedInVat = sessionManager.getVat()?.trim() ?: ""
                isIssuer = (sanitizedUrl.contains(loggedInVat) || android.net.Uri.decode(sanitizedUrl).contains(loggedInVat))

                statusMessage = "Ανάκτηση..."
                fetchStatusAdaptiveWithFishing(sanitizedUrl, if (currentMark != 0L) currentMark else null, 1, debugLog, { statusMessage = it }, { isIssuer = it }) { statusStr, mark ->
                    currentStatus = DeliveryStatus.fromApiString(statusStr)
                    if (mark != null && mark != 0L) {
                        currentMark = mark
                        sessionManager.saveDocumentMark(docId, mark)
                    }
                    rawDebug = debugLog.toString()
                    statusMessage = "✅ ${currentStatus.text}"
                }
            })
        }
    }

    Column(modifier = Modifier.padding(24.dp).fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Mobile-Parastatiko",
                style = MaterialTheme.typography.headlineSmall,
                color = NeonGreen,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { navController.navigate("settings") }) {
                Icon(Icons.Default.Settings, contentDescription = "Ρυθμίσεις", tint = NeonGreen)
            }
        }

        Button(
            onClick = { showScanner = true },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black)
        ) {
            Text("ΣΑΡΩΣΗ QR", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { navController.navigate("delivery") },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("ΕΚΔΟΣΗ ΨΗΦΙΑΚΟΥ ΔΕΛΤΙΟΥ", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (statusMessage.isNotEmpty() || currentQrUrl.isNotEmpty() || currentMark != 0L) {
            DeliveryDetailsCard(
                mark = currentMark,
                status = currentStatus,
                vehiclePlate = vehiclePlateNumber,
                issuerVat = if (isIssuer) (sessionManager.getVat() ?: "-") else "Εκδότης (ΑΑΔΕ)",
                recipientVat = if (!isIssuer) (sessionManager.getVat() ?: "-") else "Παραλήπτης"
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(text = statusMessage, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(12.dp))

            Row {
                Button(
                    onClick = {
                        debugLog.setLength(0)
                        fetchStatusAdaptiveWithFishing(currentQrUrl, if (currentMark != 0L) currentMark else null, 1, debugLog, { statusMessage = it }, { isIssuer = it }) { s, m ->
                            currentStatus = DeliveryStatus.fromApiString(s)
                            if (m != null && m != 0L) {
                                currentMark = m
                                sessionManager.saveDocumentMark(extractHash(currentQrUrl) ?: "global", m)
                            }
                            rawDebug = debugLog.toString()
                            statusMessage = "✅ ${currentStatus.text}"
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("REFRESH") }

                Spacer(Modifier.width(8.dp))

                Button(
                    onClick = { showManualMarkDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) { Text("LINK MARK") }
            }

            TextButton(onClick = { showDebugDialog = true }) {
                Text("VIEW DEBUG LOG", color = NeonGreen)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (currentQrUrl.isNotEmpty() || currentMark != 0L) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = DarkSurface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Ενέργειες", style = MaterialTheme.typography.titleSmall, color = NeonGreen)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (isIssuer) {
                        if (currentStatus == DeliveryStatus.Registered) {
                            Text("ℹ️ Το παραστατικό έχει ήδη καταχωρηθεί στην ΑΑΔΕ.", color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(16.dp))
                            ActionBtn("Ακύρωση Παραστατικού (Issuer)", containerColor = Color.Red.copy(alpha = 0.6f)) {
                                executeCancelTransfer(currentQrUrl) { success, msg ->
                                    statusMessage = msg ?: "Result: $success"
                                    if (success) {
                                        currentStatus = DeliveryStatus.Cancelled
                                        statusMessage = "✅ Ακυρώθηκε επιτυχώς"
                                    }
                                }
                            }
                        } else if (currentStatus == DeliveryStatus.Unknown) {
                            ActionBtn("Έναρξη Διακίνησης (Register)") { showRegisterDialog = true }
                            Spacer(Modifier.height(8.dp))
                            ActionBtn("Τριγωνική Διακίνηση / Τρίτος", containerColor = Color(0xFF00897B)) { showTriangularDialog = true }
                        } else if (currentStatus == DeliveryStatus.Cancelled) {
                            Text("❌ Το παραστατικό έχει ακυρωθεί.", color = Color.Red, fontSize = 14.sp)
                        } else {
                            Text("Κατάσταση: ${currentStatus.text}", color = NeonGreen)
                        }
                    } else {
                        if (currentStatus == DeliveryStatus.InTransit || currentStatus == DeliveryStatus.DeliveredByCarrier) {
                            ActionBtn("Επιβεβαίωση Παράδοσης (Full)", containerColor = Color(0xFF2E7D32)) { showConfirmDialog = true }
                            Spacer(Modifier.height(8.dp))
                            ActionBtn("Καταγραφή Μερικής Παραλαβής (Partial)", containerColor = Color(0xFFE65100)) { showPartialDeliveryDialog = true }
                            Spacer(Modifier.height(8.dp))
                            ActionBtn("Απόρριψη Παραστατικού (Recipient)", containerColor = Color(0xFFD32F2F)) { showRejectDialog = true }
                        } else {
                            Text("Κατάσταση: ${currentStatus.text}", color = NeonGreen)
                            if (currentStatus != DeliveryStatus.Cancelled) {
                                Text("Αναμονή για έναρξη διακίνησης από τον εκδότη...", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }

                    if (isIssuer) {
                        Spacer(modifier = Modifier.height(16.dp))
                        ActionBtn("Προσθήκη σε Ομαδική Σάρωση") {
                            if (currentQrUrl.isNotEmpty() && !groupedQrUrls.contains(currentQrUrl)) groupedQrUrls.add(currentQrUrl)
                            statusMessage = "Προστέθηκε στην ομάδα (${groupedQrUrls.size} παραστατικά)"
                        }

                        if (groupedQrUrls.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            ActionBtn("Διαχείριση Ομαδικής Σάρωσης (${groupedQrUrls.size})", containerColor = Color(0xFF673AB7)) {
                                showBatchScanListDialog = true
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
        Button(
            onClick = {
                sessionManager.logout()
                navController.navigate("login") { popUpTo("main") { inclusive = true } }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.1f), contentColor = Color.Red),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Αποσύνδεση") }
    }

    if (showDebugDialog) {
        Dialog(onDismissRequest = { showDebugDialog = false }) {
            Card(modifier = Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.8f)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Debug Log", fontWeight = FontWeight.Bold)
                    Column(modifier = Modifier.verticalScroll(rememberScrollState()).weight(1f)) {
                        Text(rawDebug, fontSize = 10.sp)
                    }
                    Button(onClick = { showDebugDialog = false }) { Text("CLOSE") }
                }
            }
        }
    }

    if (showManualMarkDialog) {
        ManualMarkDialog(onDismiss = { showManualMarkDialog = false }) { mark ->
            showManualMarkDialog = false
            currentMark = mark
            sessionManager.saveDocumentMark(extractHash(currentQrUrl) ?: "global", mark)
            fetchStatusAdaptiveWithFishing(currentQrUrl, mark, 1, debugLog, { statusMessage = it }, { isIssuer = it }) { s, m ->
                currentStatus = DeliveryStatus.fromApiString(s)
                rawDebug = debugLog.toString()
                statusMessage = "Mark Linked: $mark"
            }
        }
    }

    if (showRegisterDialog) {
        RegisterTransferDialog(onDismiss = { showRegisterDialog = false }) { v, t ->
            showRegisterDialog = false
            vehiclePlateNumber = v.ifBlank { "Ανευ Οχήματος" }
            statusMessage = "Registering..."
            executeRegisterTransfer(currentQrUrl, v, t, { rawDebug = it }) { success, msg, m ->
                if (success) {
                    val finalMark = m ?: currentMark
                    if (finalMark != 0L) {
                        currentMark = finalMark
                        sessionManager.saveDocumentMark(extractHash(currentQrUrl) ?: "global", finalMark)
                    }
                    fetchStatusAdaptiveWithFishing(currentQrUrl, finalMark, 1, debugLog, { statusMessage = it }, { isIssuer = it }) { s, _ ->
                        currentStatus = DeliveryStatus.fromApiString(s)
                        statusMessage = "Registered Successfully!"
                    }
                } else { statusMessage = msg ?: "Error" }
            }
        }
    }

    if (showTriangularDialog) {
        TriangularTransferDialog(
            onDismiss = { showTriangularDialog = false },
            onConfirm = { data ->
                showTriangularDialog = false
                statusMessage = "Καταχώρηση Τριγωνικής (Παραλήπτης: ${data.thirdPartyVat})..."
                executeRegisterTransfer(currentQrUrl, "TRIANGULAR", data.transportType, { rawDebug = it }) { success, msg, m ->
                    if (success) {
                        val finalMark = m ?: currentMark
                        if (finalMark != 0L) {
                            currentMark = finalMark
                            sessionManager.saveDocumentMark(extractHash(currentQrUrl) ?: "global", finalMark)
                        }
                        statusMessage = "✅ Καταχωρήθηκε Τριγωνική Μεταφορά!"
                    } else { statusMessage = msg ?: "Error" }
                }
            }
        )
    }

    if (showConfirmDialog) {
        ConfirmOutcomeDialog(onDismiss = { showConfirmDialog = false }) { outcome ->
            showConfirmDialog = false
            if (outcome == "PARTIAL") {
                showPartialDeliveryDialog = true
            } else {
                statusMessage = "Confirming Outcome..."
                executeConfirmDeliveryOutcome(currentQrUrl, outcome) { success, msg ->
                    statusMessage = if (success) "Confirmed: $outcome" else "Error: $msg"
                    if (success) fetchStatusAdaptiveWithFishing(currentQrUrl, currentMark, 1, debugLog, { statusMessage = it }, { isIssuer = it }) { s, _ -> currentStatus = DeliveryStatus.fromApiString(s) }
                }
            }
        }
    }

    if (showPartialDeliveryDialog) {
        PartialDeliveryDialog(
            onDismiss = { showPartialDeliveryDialog = false },
            onConfirm = { items, withoutRecipient ->
                showPartialDeliveryDialog = false
                statusMessage = "Καταγράφηκαν ${items.size} διαφορές ειδών (Μερική Παραλαβή)"
                executeConfirmDeliveryOutcome(currentQrUrl, "PARTIAL") { success, msg ->
                    if (success) {
                        fetchStatusAdaptiveWithFishing(currentQrUrl, currentMark, 1, debugLog, { statusMessage = it }, { isIssuer = it }) { s, _ -> currentStatus = DeliveryStatus.fromApiString(s) }
                    } else {
                        statusMessage = "Error: $msg"
                    }
                }
            }
        )
    }

    if (showBatchScanListDialog) {
        BatchScanListDialog(
            qrList = groupedQrUrls.toList(),
            onDismiss = { showBatchScanListDialog = false },
            onRemoveItem = { index -> groupedQrUrls.removeAt(index) },
            onGenerateGroupQR = {
                showBatchScanListDialog = false
                executeGenerateGroupQRCode(groupedQrUrls.toList()) { success, msg, qrUrl ->
                    if (success) {
                        statusMessage = "Group QR Generated!"
                        rawDebug = "New Group QR: $qrUrl"
                        groupedQrUrls.clear()
                    } else { statusMessage = msg ?: "Error" }
                }
            }
        )
    }

    if (showRejectDialog) {
        RejectNoteDialog(onDismiss = { showRejectDialog = false }) { reason ->
            showRejectDialog = false
            statusMessage = "Rejecting Note..."
            executeRejectDeliveryNote(currentQrUrl, reason) { success, msg ->
                statusMessage = if (success) "Rejected Reason: $reason" else "Error: $msg"
                if (success) fetchStatusAdaptiveWithFishing(currentQrUrl, currentMark, 1, debugLog, { statusMessage = it }, { isIssuer = it }) { s, _ -> currentStatus = DeliveryStatus.fromApiString(s) }
            }
        }
    }
}

private fun fetchStatusAdaptiveWithFishing(
    qrUrl: String,
    mark: Long?,
    attempt: Int,
    log: StringBuilder,
    onUI: (String) -> Unit,
    onRoleUpdate: (Boolean) -> Unit,
    onResult: (String?, Long?) -> Unit
) {
    val isSandbox = MyDataClient.sessionManager?.isSandboxMode() ?: true
    val mainHandler = Handler(Looper.getMainLooper())

    when (attempt) {
        1 -> {
            onUI("🔍 Ταυτοποίηση (Fishing)...")
            Thread {
                try {
                    val client = OkHttpClient.Builder()
                        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                        .build()

                    val request = Request.Builder()
                        .url(qrUrl)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .build()

                    val response = client.newCall(request).execute()
                    val html = response.body?.string() ?: ""

                    val loggedInVat = MyDataClient.sessionManager?.getVat()?.trim() ?: ""
                    val htmlContainsVat = html.contains(loggedInVat)

                    val markMatch = Regex("id=[\"']tMark[\"'][^>]*>\\s*(\\d+)").find(html)?.groupValues?.get(1) ?:
                                   Regex("value=[\"'](\\d{12,16})[\"']").find(html)?.groupValues?.get(1) ?:
                                   Regex("\\b(4\\d{14})\\b").find(html)?.groupValues?.get(1)

                    val issuerVatMatch = Regex("id=[\"']pIssuerVat[\"'][^>]*>\\s*(\\d+)").find(html)?.groupValues?.get(1) ?:
                                        Regex("ΑΦΜ Εκδότη:\\s*(\\d+)").find(html)?.groupValues?.get(1)

                    val isActuallyIssuer = (issuerVatMatch == loggedInVat) || 
                                          (qrUrl.contains(loggedInVat)) || 
                                          (htmlContainsVat && html.lowercase().contains("εκδότης"))

                    var bannerStatus = Regex("id=[\"']doc_type2[\"'][^>]*>\\s*([^<]+)").find(html)?.groupValues?.get(1) ?:
                                      Regex("class=[\"']alert alert-info[\"'][^>]*>\\s*([^<]+)").find(html)?.groupValues?.get(1) ?:
                                      Regex("class=[\"']alert alert-success[\"'][^>]*>\\s*([^<]+)").find(html)?.groupValues?.get(1) ?:
                                      Regex("class=[\"']label label-primary[\"'][^>]*>\\s*([^<]+)").find(html)?.groupValues?.get(1) ?:
                                      Regex("class=[\"']label label-success[\"'][^>]*>\\s*([^<]+)").find(html)?.groupValues?.get(1)

                    if (bannerStatus == null) {
                        val h = html.lowercase()
                        bannerStatus = when {
                            h.contains("επιτυχία") || h.contains("διαβιβαστεί") || h.contains("καταχωρημένο") -> "Registered"
                            h.contains("διακίνηση") || h.contains("in transit") || h.contains("μεταφορά") -> "InTransit"
                            h.contains("παραδόθηκε") || h.contains("παραλαβή") || h.contains("delivered") -> "DeliveredByCarrier"
                            h.contains("ολοκληρώθηκε") || h.contains("completed") -> "Completed"
                            h.contains("απορρίφθηκε") || h.contains("rejected") || h.contains("απόρριψη") -> "Rejected"
                            h.contains("ακυρώθηκε") || h.contains("cancelled") || h.contains("άκυρο") -> "Cancelled"
                            h.contains("αποτυχία") || h.contains("failed") -> "FailedDelivery"
                            else -> null
                        }
                    }

                    mainHandler.post {
                        val foundMark = markMatch?.toLongOrNull() ?: mark
                        if (isActuallyIssuer) onRoleUpdate(true)

                        if (bannerStatus != null) {
                            val res = DeliveryStatus.fromApiString(bannerStatus)
                            if (res != DeliveryStatus.Unknown) {
                                log.append("Fished: $bannerStatus -> Mark: $foundMark\n")
                                onUI("✅ Scraped: ${res.text}")
                                onResult(bannerStatus, foundMark)
                            } else fetchStatusAdaptiveWithFishing(qrUrl, foundMark, 2, log, onUI, onRoleUpdate, onResult)
                        } else fetchStatusAdaptiveWithFishing(qrUrl, foundMark, 2, log, onUI, onRoleUpdate, onResult)
                    }
                } catch (e: Exception) {
                    mainHandler.post { fetchStatusAdaptiveWithFishing(qrUrl, mark, 2, log, onUI, onRoleUpdate, onResult) }
                }
            }.start()
        }
        2 -> {
            if (mark == null) { fetchStatusAdaptiveWithFishing(qrUrl, null, 3, log, onUI, onRoleUpdate, onResult); return }
            onUI("Ανάκτηση (by Mark)...")
            MyDataClient.api.getDeliveryNoteStatus(mark = mark.toString()).enqueue(object : Callback<GetDeliveryStatusResponse> {
                override fun onResponse(call: Call<GetDeliveryStatusResponse>, response: Response<GetDeliveryStatusResponse>) {
                    val b = response.body(); val r = b?.responses?.firstOrNull(); val e = b?.errors ?: r?.errors
                    val errMsg = e?.errorList?.joinToString { it.message } ?: ""
                    val h = errMsg.lowercase()
                    val errorState = if (h.contains("registered") || h.contains("καταχωρημενο") || h.contains("καταχωρημένο")) "Registered"
                        else if (h.contains("intransit") || h.contains("in transit") || h.contains("διακίνηση")) "InTransit"
                        else if (h.contains("deliveredbycarrier") || h.contains("delivered") || h.contains("παραδόθηκε")) "DeliveredByCarrier"
                        else if (h.contains("completed") || h.contains("ολοκληρώθηκε")) "Completed"
                        else if (h.contains("rejected") || h.contains("απόρριψη")) "Rejected"
                        else if (h.contains("cancelled") || h.contains("ακυρώθηκε")) "Cancelled"
                        else if (h.contains("faileddelivery") || h.contains("failed")) "FailedDelivery"
                        else null

                    val s = b?.status ?: b?.invoiceDeliveryStatusAlt ?: r?.status ?: r?.invoiceDeliveryStatus ?: errorState
                    if (s != null) onResult(s, b?.invoiceMark?.toLongOrNull() ?: r?.invoiceMark ?: mark)
                    else fetchStatusAdaptiveWithFishing(qrUrl, mark, 3, log, onUI, onRoleUpdate, onResult)
                }
                override fun onFailure(call: Call<GetDeliveryStatusResponse>, t: Throwable) { fetchStatusAdaptiveWithFishing(qrUrl, mark, 3, log, onUI, onRoleUpdate, onResult) }
            })
        }
        3 -> {
            onUI("Ανάκτηση (by QR URL)...")
            MyDataClient.api.getDeliveryNoteStatus(qrUrl = qrUrl).enqueue(object : Callback<GetDeliveryStatusResponse> {
                override fun onResponse(call: Call<GetDeliveryStatusResponse>, response: Response<GetDeliveryStatusResponse>) {
                    val b = response.body(); val r = b?.responses?.firstOrNull(); val e = b?.errors ?: r?.errors
                    val errMsg = e?.errorList?.joinToString { it.message } ?: ""
                    val h = errMsg.lowercase()
                    val errorState = if (h.contains("registered") || h.contains("καταχωρημενο") || h.contains("καταχωρημένο")) "Registered"
                        else if (h.contains("intransit") || h.contains("in transit") || h.contains("διακίνηση")) "InTransit"
                        else if (h.contains("deliveredbycarrier") || h.contains("delivered") || h.contains("παραδόθηκε")) "DeliveredByCarrier"
                        else if (h.contains("completed") || h.contains("ολοκληρώθηκε")) "Completed"
                        else if (h.contains("rejected") || h.contains("απόρριψη")) "Rejected"
                        else if (h.contains("cancelled") || h.contains("ακυρώθηκε")) "Cancelled"
                        else if (h.contains("faileddelivery") || h.contains("failed")) "FailedDelivery"
                        else null

                    val s = b?.status ?: b?.invoiceDeliveryStatusAlt ?: r?.status ?: r?.invoiceDeliveryStatus ?: errorState
                    if (s != null) onResult(s, b?.invoiceMark?.toLongOrNull() ?: r?.invoiceMark ?: mark)
                    else fetchStatusAdaptiveWithFishing(qrUrl, mark, 4, log, onUI, onRoleUpdate, onResult)
                }
                override fun onFailure(call: Call<GetDeliveryStatusResponse>, t: Throwable) { fetchStatusAdaptiveWithFishing(qrUrl, mark, 4, log, onUI, onRoleUpdate, onResult) }
            })
        }
        4 -> {
            if (mark == null) { onUI("⚠️ Δεν βρέθηκε στην AADE"); onResult("UNKNOWN", null); return }
            onUI("Αναζήτηση Παραστατικού (AADE)...")
            MyDataClient.api.requestDocs(mark = mark).enqueue(object : Callback<RequestedInvoicesDoc> {
                override fun onResponse(call: Call<RequestedInvoicesDoc>, response: Response<RequestedInvoicesDoc>) {
                    val inv = response.body()?.invoicesDoc?.invoices?.firstOrNull()
                    val s = inv?.invoiceDeliveryStatus
                    if (s != null) onResult(s, inv?.mark ?: mark)
                    else onResult(null, mark)
                }
                override fun onFailure(call: Call<RequestedInvoicesDoc>, t: Throwable) { onResult(null, mark) }
            })
        }
    }
}

private fun extractHash(url: String): String? {
    if (url.contains("q=")) return url.substringAfter("q=").substringBefore("&")
    return null
}

private fun executeRegisterTransfer(qrUrl: String, vehicleNo: String, type: Int, onLog: (String) -> Unit, onResult: (Boolean, String?, Long?) -> Unit) {
    val req = RegisterTransferRequest(qrUrl, TransportDetailRequest(vehicleNo, type, MyDataClient.sessionManager?.getVat()))
    val xml = MyDataXmlSerializer.serializeRegisterTransfer(req)
    MyDataClient.api.registerTransfer(xml).enqueue(object : Callback<ResponseDoc> {
        override fun onResponse(call: Call<ResponseDoc>, response: Response<ResponseDoc>) {
            if (response.isSuccessful) {
                val r = response.body()?.responses?.firstOrNull()
                if (r?.statusCode == "Success") onResult(true, "Success", r.transferMark ?: r.invoiceMark)
                else {
                    val msg = r?.errors?.errorList?.firstOrNull()?.message ?: "Error code: ${r?.statusCode}"
                    onResult(false, msg, null)
                }
            } else { onResult(false, "HTTP ${response.code()}", null) }
        }
        override fun onFailure(call: Call<ResponseDoc>, t: Throwable) { onResult(false, "Failure", null) }
    })
}

private fun executeConfirmDeliveryOutcome(qrUrl: String, outcome: String, onResult: (Boolean, String?) -> Unit) {
    val req = ConfirmDeliveryOutcomeRequest(qrUrl, outcome)
    val xml = MyDataXmlSerializer.serializeConfirmDeliveryOutcome(req)
    MyDataClient.api.confirmDeliveryOutcome(xml).enqueue(object : Callback<ResponseDoc> {
        override fun onResponse(call: Call<ResponseDoc>, response: Response<ResponseDoc>) {
             if (response.isSuccessful) {
                 val r = response.body()?.responses?.firstOrNull()
                 if (r?.statusCode == "Success") onResult(true, "Success")
                 else onResult(false, r?.errors?.errorList?.firstOrNull()?.message ?: "Error code: ${r?.statusCode}")
             } else { onResult(false, "HTTP ${response.code()}") }
        }
        override fun onFailure(call: Call<ResponseDoc>, t: Throwable) { onResult(false, "Failure") }
    })
}

private fun executeRejectDeliveryNote(qrUrl: String, reason: String?, onResult: (Boolean, String?) -> Unit) {
    val req = RejectDeliveryNoteRequest(qrUrl, reason?.ifBlank { null })
    val xml = MyDataXmlSerializer.serializeRejectDeliveryNote(req)
    MyDataClient.api.rejectDeliveryNote(xml).enqueue(object : Callback<ResponseDoc> {
        override fun onResponse(call: Call<ResponseDoc>, response: Response<ResponseDoc>) {
             if (response.isSuccessful) {
                 val r = response.body()?.responses?.firstOrNull()
                 if (r?.statusCode == "Success") onResult(true, "Success")
                 else onResult(false, r?.errors?.errorList?.firstOrNull()?.message ?: "Error code: ${r?.statusCode}")
             } else { onResult(false, "HTTP ${response.code()}") }
        }
        override fun onFailure(call: Call<ResponseDoc>, t: Throwable) { onResult(false, "Failure") }
    })
}

private fun executeGenerateGroupQRCode(qrUrls: List<String>, onResult: (Boolean, String?, String?) -> Unit) {
    val req = GroupQRCodeRequest(QrUrlsWrapper(qrUrls))
    val xml = MyDataXmlSerializer.serializeGroupQRCode(req)
    MyDataClient.api.generateGroupQRCode(xml).enqueue(object : Callback<ResponseDoc> {
        override fun onResponse(call: Call<ResponseDoc>, response: Response<ResponseDoc>) {
             if (response.isSuccessful) {
                 val r = response.body()?.responses?.firstOrNull()
                 if (r?.statusCode == "Success") onResult(true, "Success", r.qrUrl)
                 else onResult(false, r?.errors?.errorList?.firstOrNull()?.message ?: "Error code: ${r?.statusCode}", null)
             } else { onResult(false, "HTTP ${response.code()}", null) }
        }
        override fun onFailure(call: Call<ResponseDoc>, t: Throwable) { onResult(false, "Failure", null) }
    })
}

private fun executeCancelTransfer(qrUrl: String, onResult: (Boolean, String?) -> Unit) {
    val req = CancelDeliveryNoteRequest(qrUrl, "User cancellation via mobile")
    val xml = MyDataXmlSerializer.serializeCancelDeliveryNote(req)
    MyDataClient.api.cancelDeliveryNote(xml).enqueue(object : Callback<ResponseDoc> {
        override fun onResponse(call: Call<ResponseDoc>, response: Response<ResponseDoc>) {
             val r = response.body()?.responses?.firstOrNull()
             if (r?.statusCode == "Success") onResult(true, "Success")
             else onResult(false, r?.errors?.errorList?.firstOrNull()?.message ?: "Error code: ${r?.statusCode}")
        }
        override fun onFailure(call: Call<ResponseDoc>, t: Throwable) { onResult(false, "Failure") }
    })
}

private fun sanitizeQrUrl(url: String): String {
    if (url.contains("qrUrl=")) return android.net.Uri.decode(url.substringAfter("qrUrl=").substringBefore("&"))
    return url
}

@Composable
fun ManualMarkDialog(onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    var m by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manual Mark Linking") },
        text = { OutlinedTextField(value = m, onValueChange = { m = it }, label = { Text("Enter invoiceMark") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) },
        confirmButton = { Button(onClick = { m.toLongOrNull()?.let { onConfirm(it) } }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}

@Composable
fun RegisterTransferDialog(onDismiss: () -> Unit, onConfirm: (String, Int) -> Unit) {
    var v by remember { mutableStateOf("") }
    var t by remember { mutableIntStateOf(1) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register Transfer") },
        text = {
            Column {
                OutlinedTextField(value = v, onValueChange = { v = it }, label = { Text("Vehicle Plate No") })
                Spacer(Modifier.height(8.dp))
                Text("Transport Type: $t")
                Row {
                    Button(onClick = { t = 1 }) { Text("1") }
                    Spacer(Modifier.width(4.dp))
                    Button(onClick = { t = 5 }) { Text("5") }
                    Spacer(Modifier.width(4.dp))
                    Button(onClick = { t = 7 }) { Text("7") }
                }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(v, t) }) { Text("START") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}

@Composable
fun ConfirmOutcomeDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var outcome by remember { mutableStateOf("FULL") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Delivery") },
        text = {
            Column {
                Row {
                    RadioButton(selected = outcome == "FULL", onClick = { outcome = "FULL" })
                    Text("FULL (Πλήρης Παραλαβή)")
                }
                Row {
                    RadioButton(selected = outcome == "PARTIAL", onClick = { outcome = "PARTIAL" })
                    Text("PARTIAL (Μερική Παραλαβή)")
                }
                Row {
                    RadioButton(selected = outcome == "NONE", onClick = { outcome = "NONE" })
                    Text("NONE (Άρνηση Παραλαβής)")
                }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(outcome) }) { Text("CONFIRM") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}

@Composable
fun RejectNoteDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reject Delivery Note") },
        text = { OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Rejection Reason") }) },
        confirmButton = { Button(onClick = { onConfirm(reason) }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("REJECT") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}

@Composable
fun ActionBtn(text: String, containerColor: Color = NeonGreen, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = if (containerColor == NeonGreen) Color.Black else Color.White)
    ) { Text(text) }
}
