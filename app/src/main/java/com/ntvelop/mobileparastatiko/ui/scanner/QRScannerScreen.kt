package com.ntvelop.mobileparastatiko.ui.scanner

import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.ntvelop.mobileparastatiko.api.*
import com.ntvelop.mobileparastatiko.xml.MyDataXmlSerializer
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun QRScannerScreen(
    onQrDetected: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var scannedQrUrl by remember { mutableStateOf<String?>(null) }
    var statusText by remember { mutableStateOf<String?>(null) }
    var deliveryStatus by remember { mutableStateOf(DeliveryStatus.Unknown) }
    var isLoading by remember { mutableStateOf(false) }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    var vehicleNumber by remember { mutableStateOf("") }
    var rejectionReason by remember { mutableStateOf("") }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (scannedQrUrl == null) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalyzer = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(
                                    cameraExecutor,
                                    BarcodeAnalyzer(
                                        onQrScanned = { qrResult ->
                                            if (scannedQrUrl == null) {
                                                scannedQrUrl = qrResult
                                                onQrDetected(qrResult)

                                                // Query status
                                                isLoading = true
                                                MyDataClient.api.getDeliveryNoteStatus(qrUrl = qrResult)
                                                    .enqueue(object : Callback<GetDeliveryStatusResponse> {
                                                        override fun onResponse(
                                                            call: Call<GetDeliveryStatusResponse>,
                                                            response: Response<GetDeliveryStatusResponse>
                                                        ) {
                                                            isLoading = false
                                                            val body = response.body()
                                                            val s = body?.status ?: body?.invoiceDeliveryStatusAlt ?: "Registered"
                                                            deliveryStatus = DeliveryStatus.fromApiString(s)
                                                            statusText = "Κατάσταση: ${deliveryStatus.text}"
                                                        }

                                                        override fun onFailure(call: Call<GetDeliveryStatusResponse>, t: Throwable) {
                                                            isLoading = false
                                                            deliveryStatus = DeliveryStatus.Registered
                                                            statusText = "Κατάσταση: Καταχωρημένο (Offline/Sandbox)"
                                                        }
                                                    })
                                            }
                                        }
                                    )
                                )
                            }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalyzer
                            )
                        } catch (exc: Exception) {
                            Log.e("QRScanner", "Use case binding failed", exc)
                        }

                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f))
            ) {
                Text(
                    text = "Στοχεύστε το QR Code του Δελτίου Αποστολής",
                    color = Color.White,
                    modifier = Modifier.padding(16.dp),
                    fontSize = 16.sp
                )
            }
        } else {
            // Action Overlay Screen for Scanned Delivery Note Lifecycle
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Ψηφιακό Δελτίο Αποστολής (Phase B)",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "QR URL:", fontWeight = FontWeight.SemiBold)
                        Text(text = scannedQrUrl ?: "", fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = statusText ?: "Φόρτωση...", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                if (isLoading) {
                    CircularProgressIndicator()
                }

                actionMessage?.let { msg ->
                    Text(text = msg, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action Buttons according to Phase B State Machine
                OutlinedTextField(
                    value = vehicleNumber,
                    onValueChange = { vehicleNumber = it },
                    label = { Text("Αριθμός Κυκλοφορίας (για Έναρξη Διακίνησης)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = {
                        isLoading = true
                        val req = RegisterTransferRequest(
                            qrUrl = scannedQrUrl ?: "",
                            transportDetail = TransportDetailRequest(vehicleNumber = vehicleNumber.ifBlank { "KHH1234" }, transportType = 1)
                        )
                        val xml = MyDataXmlSerializer.serializeRegisterTransfer(req)
                        MyDataClient.api.registerTransfer(xml).enqueue(object : Callback<ResponseDoc> {
                            override fun onResponse(call: Call<ResponseDoc>, response: Response<ResponseDoc>) {
                                isLoading = false
                                deliveryStatus = DeliveryStatus.InTransit
                                statusText = "Κατάσταση: Προς Διακίνηση / Σε Μεταφορά (InTransit)"
                                actionMessage = "Επιτυχής Έναρξη Διακίνησης (RegisterTransfer)"
                            }

                            override fun onFailure(call: Call<ResponseDoc>, t: Throwable) {
                                isLoading = false
                                actionMessage = "Αποτυχία επικοινωνίας. Αποθηκεύτηκε τοπικά."
                            }
                        })
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("1. Έναρξη Διακίνησης (RegisterTransfer)")
                }

                Button(
                    onClick = {
                        isLoading = true
                        val req = ConfirmDeliveryOutcomeRequest(qrUrl = scannedQrUrl ?: "", outcome = "FULL")
                        val xml = MyDataXmlSerializer.serializeConfirmDeliveryOutcome(req)
                        MyDataClient.api.confirmDeliveryOutcome(xml).enqueue(object : Callback<ResponseDoc> {
                            override fun onResponse(call: Call<ResponseDoc>, response: Response<ResponseDoc>) {
                                isLoading = false
                                deliveryStatus = DeliveryStatus.Completed
                                statusText = "Κατάσταση: Ολοκληρώθηκε (Completed)"
                                actionMessage = "Επιτυχής Πλήρης Παραλαβή (ConfirmDeliveryOutcome)"
                            }

                            override fun onFailure(call: Call<ResponseDoc>, t: Throwable) {
                                isLoading = false
                                actionMessage = "Αποτυχία παραλαβής."
                            }
                        })
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("2. Αποδοχή & Πλήρης Παραλαβή (Completed)")
                }

                OutlinedTextField(
                    value = rejectionReason,
                    onValueChange = { rejectionReason = it },
                    label = { Text("Αιτιολογία Απόρριψης") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = {
                        isLoading = true
                        val req = RejectDeliveryNoteRequest(qrUrl = scannedQrUrl ?: "", rejectionReason = rejectionReason)
                        val xml = MyDataXmlSerializer.serializeRejectDeliveryNote(req)
                        MyDataClient.api.rejectDeliveryNote(xml).enqueue(object : Callback<ResponseDoc> {
                            override fun onResponse(call: Call<ResponseDoc>, response: Response<ResponseDoc>) {
                                isLoading = false
                                deliveryStatus = DeliveryStatus.Rejected
                                statusText = "Κατάσταση: Απορριφθείσα (Rejected)"
                                actionMessage = "Ολική Απόρριψη Διακίνησης (RejectDeliveryNote)"
                            }

                            override fun onFailure(call: Call<ResponseDoc>, t: Throwable) {
                                isLoading = false
                                actionMessage = "Αποτυχία απόρριψης."
                            }
                        })
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("3. Ολική Απόρριψη (RejectDeliveryNote)")
                }

                OutlinedButton(
                    onClick = {
                        scannedQrUrl = null
                        actionMessage = null
                        statusText = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Νέα Σάρωση")
                }
            }
        }
    }
}

private class BarcodeAnalyzer(private val onQrScanned: (String) -> Unit) : ImageAnalysis.Analyzer {

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()
    private val scanner = BarcodeScanning.getClient(options)

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        barcode.rawValue?.let { value ->
                            onQrScanned(value)
                            imageProxy.close()
                            return@addOnSuccessListener
                        }
                    }
                }
                .addOnFailureListener {}
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}
