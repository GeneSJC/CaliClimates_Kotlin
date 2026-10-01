package io.github.genesjc.microclimatefinder.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Renders the web-preview HTML/SVG animation inside an embedded, offline Android WebView.
 * 
 * Benefits:
 * 1. 100% pixel-perfect match with the web preview (smooth SVG beziers, drop shadows, CSS keyframes).
 * 2. Completely offline: assets are bundled in file:///android_asset/splash_animation_web.html.
 * 3. Smooth 60fps hardware-accelerated rendering with no network latency.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebSplashAnimationScreen(
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF08203E))
    ) {
        // Top navigation bar with Back button and technical badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onNavigateBack,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E293B),
                    contentColor = Color(0xFFF1F5F9)
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("← Back", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Surface(
                color = Color(0xFF0369A1).copy(alpha = 0.35f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "WebView Vector • v0.25.0",
                    color = Color(0xFF7DD3FC),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        // Embedded WebView rendering the local HTML/SVG animation
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    webViewClient = WebViewClient()
                    loadUrl("file:///android_asset/splash_animation_web.html")
                }
            }
        )
    }
}
