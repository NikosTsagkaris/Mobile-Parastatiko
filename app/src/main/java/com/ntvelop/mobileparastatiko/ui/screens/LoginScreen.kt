package com.ntvelop.mobileparastatiko.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ntvelop.mobileparastatiko.api.MyDataClient
import com.ntvelop.mobileparastatiko.api.RequestedInvoicesDoc
import com.ntvelop.mobileparastatiko.api.SessionManager
import com.ntvelop.mobileparastatiko.ui.theme.DarkBg
import com.ntvelop.mobileparastatiko.ui.theme.DarkSurface
import com.ntvelop.mobileparastatiko.ui.theme.NeonGreen
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    var username by remember { mutableStateOf(sessionManager.getUsername() ?: "") }
    var vat by remember { mutableStateOf(sessionManager.getVat() ?: "") }
    var subKey by remember { mutableStateOf(sessionManager.getSubscriptionKey() ?: "") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var isSandbox by remember { mutableStateOf(sessionManager.isSandboxMode()) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "ΣΥΝΔΕΣΗ",
            color = NeonGreen,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(6.dp))
        Text(text = "myDATA Mobile Parastatiko", color = Color.Gray, fontSize = 14.sp)

        Spacer(modifier = Modifier.height(32.dp))

        // Όνομα Χρήστη AADE
        OutlinedTextField(
            value = username,
            onValueChange = { username = it; showError = false },
            label = { Text("Όνομα Χρήστη (aade-user-id)", color = Color.Gray) },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = NeonGreen,
                cursorColor = NeonGreen
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        // ΑΦΜ Επιχείρησης
        OutlinedTextField(
            value = vat,
            onValueChange = { vat = it; showError = false },
            label = { Text("ΑΦΜ Επιχείρησης", color = Color.Gray) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = NeonGreen,
                cursorColor = NeonGreen
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Subscription Key με Toggle
        OutlinedTextField(
            value = subKey,
            onValueChange = { subKey = it; showError = false },
            label = { Text("Κλειδί Εισόδου (Subscription Key)", color = Color.Gray) },
            singleLine = true,
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Εμφάνιση κλειδιού",
                        tint = Color.Gray
                    )
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = NeonGreen,
                cursorColor = NeonGreen
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Επιλογή Sandbox / Live Mode
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Δοκιμαστικό Περιβάλλον (Sandbox)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(if (isSandbox) "myDATA Dev / Test Server" else "Επίσημο Production myDATA", color = Color.Gray, fontSize = 11.sp)
                }
                Switch(
                    checked = isSandbox,
                    onCheckedChange = { isSandbox = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NeonGreen,
                        checkedTrackColor = NeonGreen.copy(alpha = 0.5f)
                    )
                )
            }
        }

        if (showError) {
            Text(
                text = "Συμπληρώστε όλα τα πεδία!",
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (statusMessage.isNotEmpty()) {
            Text(
                text = statusMessage,
                color = if (statusMessage.contains("Επαλήθευση")) NeonGreen else Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = {
                if (username.isBlank() || subKey.isBlank() || vat.isBlank()) {
                    showError = true
                } else {
                    isLoading = true
                    statusMessage = "Επαλήθευση στοιχείων με ΑΑΔΕ..."

                    sessionManager.setSandboxMode(isSandbox)
                    sessionManager.saveCredentials(username.trim(), vat.trim(), subKey.trim())
                    MyDataClient.resetClient()
                    MyDataClient.sessionManager = sessionManager

                    MyDataClient.api.requestDocs(mark = 0L).enqueue(object : Callback<RequestedInvoicesDoc> {
                        override fun onResponse(call: Call<RequestedInvoicesDoc>, response: Response<RequestedInvoicesDoc>) {
                            isLoading = false
                            val code = response.code()
                            if (response.isSuccessful) {
                                onLoginSuccess()
                            } else if (code == 401 || code == 403) {
                                statusMessage = "Σφάλμα $code: Λανθασμένο Key ή User ID!"
                            } else if (code == 404) {
                                statusMessage = "Σφάλμα 404: Ελέγξτε αν έχετε επιλέξει σωστά το Sandbox Mode."
                            } else {
                                statusMessage = "Σφάλμα $code: Η ΑΑΔΕ επέστρεψε άγνωστη απόκριση."
                            }
                        }

                        override fun onFailure(call: Call<RequestedInvoicesDoc>, t: Throwable) {
                            isLoading = false
                            statusMessage = "Αποτυχία: ${t.localizedMessage}\nΕλέγξτε τη σύνδεση δικτύου."
                        }
                    })
                }
            },
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
            } else {
                Text("ΣΥΝΔΕΣΗ & ΕΠΑΛΗΘΕΥΣΗ", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

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
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}