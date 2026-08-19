package com.ntvelop.mobileparastatiko.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ntvelop.mobileparastatiko.api.SessionManager
import com.ntvelop.mobileparastatiko.ui.theme.DarkBg
import com.ntvelop.mobileparastatiko.ui.theme.DarkSurface
import com.ntvelop.mobileparastatiko.ui.theme.NeonGreen

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    var username by remember { mutableStateOf(sessionManager.getUsername() ?: "") }
    var vat by remember { mutableStateOf(sessionManager.getVat() ?: "") }
    var subKey by remember { mutableStateOf(sessionManager.getSubscriptionKey() ?: "") }

    var isSandbox by remember { mutableStateOf(sessionManager.isSandboxMode()) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        
        Text(
            text = "ΣΥΝΔΕΣΗ",
            color = NeonGreen,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "myDATA Mobile Parastatiko", color = Color.Gray, fontSize = 16.sp)
        
        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it; showError = false },
            label = { Text("Όνομα Χρήστη", color = Color.Gray) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = NeonGreen,
                cursorColor = NeonGreen
            ),
            modifier = Modifier.fillMaxWidth().height(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = vat,
            onValueChange = { vat = it; showError = false },
            label = { Text("ΑΦΜ", color = Color.Gray) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = NeonGreen,
                cursorColor = NeonGreen
            ),
            modifier = Modifier.fillMaxWidth().height(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = subKey,
            onValueChange = { subKey = it; showError = false },
            label = { Text("Κλειδί εισόδου (Subscription Key)", color = Color.Gray) },
            visualTransformation = PasswordVisualTransformation(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = NeonGreen,
                cursorColor = NeonGreen
            ),
            modifier = Modifier.fillMaxWidth().height(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Sandbox Mode", color = Color.Gray, modifier = Modifier.weight(1f))
            Switch(
                checked = isSandbox,
                onCheckedChange = { isSandbox = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = NeonGreen,
                    checkedTrackColor = NeonGreen.copy(alpha = 0.5f)
                )
            )
        }

        if (showError) {
            Text(
                "Συμπληρώστε όλα τα πεδία!",
                color = Color.Red,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        
        if (statusMessage.isNotEmpty()) {
            Text(
                statusMessage,
                color = if (statusMessage.contains("Επαλήθευση")) NeonGreen else Color.Red,
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = {
                if (username.isBlank() || subKey.isBlank() || vat.isBlank()) {
                    showError = true
                } else {
                    isLoading = true
                    statusMessage = "Επαλήθευση στοιχείων..."
                    
                    // Save first so client uses them
                    sessionManager.setSandboxMode(isSandbox)
                    sessionManager.saveCredentials(username.trim(), vat.trim(), subKey.trim())
                    com.ntvelop.mobileparastatiko.api.MyDataClient.resetClient()
                    com.ntvelop.mobileparastatiko.api.MyDataClient.sessionManager = sessionManager
                    
                    // Test connection
                    com.ntvelop.mobileparastatiko.api.MyDataClient.api.requestDocs(mark = 0L).enqueue(object : retrofit2.Callback<com.ntvelop.mobileparastatiko.api.RequestedInvoicesDoc> {
                        override fun onResponse(call: retrofit2.Call<com.ntvelop.mobileparastatiko.api.RequestedInvoicesDoc>, response: retrofit2.Response<com.ntvelop.mobileparastatiko.api.RequestedInvoicesDoc>) {
                            isLoading = false
                            val code = response.code()
                            if (response.isSuccessful) {
                                // 200 OK means keys are 100% correct
                                onLoginSuccess()
                            } else if (code == 401 || code == 403) {
                                statusMessage = "Σφάλμα $code: Λανθασμένο Key ή UserID!"
                            } else if (code == 404) {
                                // Some sandbox environments return 404 if the path is slightly off but auth is ok? 
                                // To be safe, let's treat 404 as "reached the server" but maybe not verified.
                                // Actually, let's be strict.
                                statusMessage = "Σφάλμα 404: Δεν βρέθηκε η υπηρεσία. Ελέγξτε το Mode (Sandbox)."
                            } else {
                                statusMessage = "Σφάλμα $code: Η AADE επέστρεψε σφάλμα."
                            }
                        }
                        override fun onFailure(call: retrofit2.Call<com.ntvelop.mobileparastatiko.api.RequestedInvoicesDoc>, t: Throwable) {
                            isLoading = false
                            statusMessage = "Αποτυχία: ${t.localizedMessage}\nΕλέγξτε τη σύνδεση σας."
                        }
                    })
                }
            },
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
            } else {
                Text("ΕΓΓΡΑΦΗ / ΣΥΝΔΕΣΗ", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Watermark
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "provided by ", color = Color.Gray, fontSize = 12.sp)
            Text(text = "NTvelop", color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            val imageId = context.resources.getIdentifier("logo_ntvelop", "drawable", context.packageName)
            if (imageId != 0) {
                Spacer(modifier = Modifier.width(4.dp))
                Image(
                    painter = painterResource(id = imageId),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
