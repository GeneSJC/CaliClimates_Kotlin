package io.github.genesjc.microclimatefinder.ui.screens

import android.annotation.SuppressLint
import android.text.TextUtils
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.genesjc.microclimatefinder.R

private const val TAG = "WebSplash"
private const val HTML_ASSET = "splash_animation_web.html"

/**
 * Renders the web-preview HTML/SVG animation inside an embedded, offline Android WebView.
 *
 * The HTML is read from assets and the app name in its title heading is replaced with the app_name
 * string before loading, so the name lives in one place even if the HTML asset is swapped for a
 * newer version. The badge at the top shows the load status (loading, loaded or the error) and page
 * console messages go to Logcat under "WebSplash"; both are diagnostics for this demo screen.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebSplashAnimationScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val appName = stringResource(R.string.app_name)
    val html = remember(appName) {
        try {
            val safeName = TextUtils.htmlEncode(appName)
            context.assets.open(HTML_ASSET).bufferedReader().use { it.readText() }
                .replace(Regex("(<h1 class=\"app-title\">).*?(</h1>)")) { m ->
                    m.groupValues[1] + safeName + m.groupValues[2]
                }
        } catch (e: Exception) {
            Log.e(TAG, "Could not read $HTML_ASSET", e)
            null
        }
    }
    var status by remember { mutableStateOf(if (html == null) "error: asset not readable" else "loading") }

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
                    text = "WebView Vector • v0.25.0 • $status",
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
            factory = { viewContext ->
                WebView(viewContext).apply {
                    setBackgroundColor(android.graphics.Color.parseColor("#08203E"))
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                            Log.w(TAG, "console: ${message.message()} (${message.sourceId()}:${message.lineNumber()})")
                            return true
                        }
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String?) {
                            status = "loaded"
                        }

                        override fun onReceivedError(
                            view: WebView,
                            request: WebResourceRequest,
                            error: WebResourceError
                        ) {
                            if (request.isForMainFrame) status = "error: ${error.description}"
                        }
                    }
                    if (html != null) {
                        loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null)
                    }
                }
            }
        )
    }
}
