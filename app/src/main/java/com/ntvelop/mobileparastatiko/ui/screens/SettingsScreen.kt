package com.ntvelop.mobileparastatiko.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ntvelop.mobileparastatiko.api.MyDataValidator
import com.ntvelop.mobileparastatiko.api.SessionManager
import com.ntvelop.mobileparastatiko.ui.theme.DarkBg
import com.ntvelop.mobileparastatiko.ui.theme.DarkSurface
import com.ntvelop.mobileparastatiko.ui.theme.NeonGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    sessionManager: SessionManager,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    var vat by remember { mutableStateOf(sessionManager.getVat() ?: "") }
    var aadeUserId by remember { mutableStateOf(sessionManager.getUsername() ?: "") }
    var subscriptionKey by remember { mutableStateOf(sessionManager.getSubscriptionKey() ?: "") }
    var isSandbox by remember { mutableStateOf(sessionManager.isSandboxMode()) }

    val isVatValid = remember(vat) {
        if (vat.length == 9) MyDataValidator.isValidGreekVat(vat) else true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ρυθμίσεις myDATA", fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Κάρτα Περιβάλλοντος
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Περιβάλλον myDATA", fontWeight = FontWeight.Bold, color = NeonGreen)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isSandbox) "Δοκιμαστικό Περιβάλλον (Sandbox)" else "Παραγωγικό Περιβάλλον (Live)",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isSandbox) "https://mydata-dev.azure-api.net" else "https://mydatapi.aade.gr/myDATA",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = isSandbox,
                            onCheckedChange = { isSandbox = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen, checkedTrackColor = NeonGreen.copy(alpha = 0.4f))
                        )
                    }
                }
            }

            // Κάρτα Διαπιστευτηρίων
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Στοιχεία Επιχείρησης & API", fontWeight = FontWeight.Bold, color = NeonGreen)

                    OutlinedTextField(
                        value = vat,
                        onValueChange = { if (it.length <= 9) vat = it },
                        label = { Text("ΑΦΜ Επιχείρησης (9 ψηφία)") },
                        isError = vat.length == 9 && !isVatValid,
                        supportingText = {
                            if (vat.length == 9 && !isVatValid) {
                                Text("Μη έγκυρο ΑΦΜ (Modulo 11)", color = Color.Red, fontSize = 10.sp)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = aadeUserId,
                        onValueChange = { aadeUserId = it },
                        label = { Text("aade-user-id (myDATA Username)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = subscriptionKey,
                        onValueChange = { subscriptionKey = it },
                        label = { Text("Ocp-Apim-Subscription-Key") },
                        visualTransformation = PasswordVisualTransformation(),
                        trailingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // Πληροφορίες Έκδοσης
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Mobile-Parastatiko v1.4.0", fontWeight = FontWeight.Bold, color = Color.LightGray, fontSize = 12.sp)
                    Text("Συμβατό με myDATA Technical Specs v2.0.2", color = Color.Gray, fontSize = 11.sp)
                }
            }

            Spacer(Modifier.height(8.dp))

            //Κουμπί Αποθήκευσης
            Button(
                onClick = {
                    if (vat.isNotBlank() && !MyDataValidator.isValidGreekVat(vat)) {
                        Toast.makeText(context, "Παρακαλώ ελέγξτε το ΑΦΜ (άκυρο Modulo 11)", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // ΕΔΩ ΗΤΑΝ ΤΟ ΛΑΘΟΣ - Διορθώθηκε για να ταιριάζει με τον SessionManager
                    sessionManager.saveCredentials(
                        username = aadeUserId.trim(),
                        vat = vat.trim(),
                        subKey = subscriptionKey.trim()
                    )
                    
                    // Το sandbox αποθηκεύεται με δική του μέθοδο
                    sessionManager.setSandboxMode(isSandbox)
                    
                    Toast.makeText(context, "Οι ρυθμίσεις αποθηκεύτηκαν επιτυχώς!", Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("ΑΠΟΘΗΚΕΥΣΗ ΡΥΘΜΙΣΕΩΝ", fontWeight = FontWeight.Bold)
            }
        }
    }
}