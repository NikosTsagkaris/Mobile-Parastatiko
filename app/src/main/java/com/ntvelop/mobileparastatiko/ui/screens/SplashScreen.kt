package com.ntvelop.mobileparastatiko.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ntvelop.mobileparastatiko.api.SessionManager
import com.ntvelop.mobileparastatiko.ui.theme.DarkBg
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashFinished: (isLoggedIn: Boolean) -> Unit) {
    val context = LocalContext.current
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(key1 = true) {
        alphaAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1500)
        )
        delay(1000) // Minimum display time
        val sessionManager = SessionManager(context)
        onSplashFinished(sessionManager.isLoggedIn())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentAlignment = Alignment.Center
    ) {
        // Will use Android's default launcher icon as a placeholder.
        // The user MUST place their logo.png in res/drawable as R.drawable.logo_ntvelop
        // Using ic_launcher_foreground as fallback conceptually, but we will assume logo_ntvelop exists.
        val imageId = context.resources.getIdentifier("logo_ntvelop", "drawable", context.packageName)
        if (imageId != 0) {
            Image(
                painter = painterResource(id = imageId),
                contentDescription = "NTvelop Logo",
                modifier = Modifier.size(200.dp).alpha(alphaAnim.value)
            )
        } else {
            // Placeholder if logo isn't dropped yet.
            androidx.compose.material3.Text(
                "NTVELOP LOGO PLACEHOLDER\nΤοποθετήστε το logo_ntvelop.png", 
                color = com.ntvelop.mobileparastatiko.ui.theme.NeonGreen,
                modifier = Modifier.alpha(alphaAnim.value)
            )
        }
    }
}
