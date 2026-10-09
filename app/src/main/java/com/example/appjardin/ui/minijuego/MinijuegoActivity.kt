package com.example.appjardin.ui.minijuego

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MinijuegoActivity : ComponentActivity() {
    private val viewModel: MinijuegoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable full-screen immersive mode & keep screen on
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContent {
            MaterialTheme {
                MinijuegoScreen(
                    viewModel = viewModel,
                    onExit = { finish() }
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MinijuegoScreen(
    viewModel: MinijuegoViewModel,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    var hasError by remember { mutableStateOf(false) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    val initStateJson by viewModel.initStateJson.collectAsStateWithLifecycle()
    val answerResponseJson by viewModel.answerResponseJson.collectAsStateWithLifecycle()

    BackHandler {
        onExit()
    }

    // Send init state to JS when loaded
    LaunchedEffect(initStateJson) {
        val json = initStateJson
        val webView = webViewInstance
        if (json != null && webView != null) {
            val safeJson = json.replace("'", "\\'")
            webView.evaluateJavascript("if (typeof setAndroidState === 'function') { setAndroidState('$safeJson'); }", null)
        }
    }

    // Send answer response to JS when processed
    LaunchedEffect(answerResponseJson) {
        val json = answerResponseJson
        val webView = webViewInstance
        if (json != null && webView != null) {
            val safeJson = json.replace("'", "\\'")
            webView.evaluateJavascript("if (typeof onAnswerProcessed === 'function') { onAnswerProcessed('$safeJson'); }", null)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasError) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No se pudo cargar el minijuego",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = {
                            hasError = false
                            webViewInstance?.reload()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                    ) {
                        Text("Reintentar", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onExit,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("Salir", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewInstance = this
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = false
                        settings.allowFileAccess = true
                        settings.allowContentAccess = false
                        settings.allowFileAccessFromFileURLs = false
                        settings.allowUniversalAccessFromFileURLs = false

                        class AndroidBridge {
                            @JavascriptInterface
                            fun requestInitState() {
                                try {
                                    viewModel.loadInitState()
                                } catch (e: Exception) {
                                    Log.e("AndroidBridge", "Error in requestInitState", e)
                                }
                            }

                            @JavascriptInterface
                            fun submitAnswer(jsonInput: String) {
                                try {
                                    viewModel.submitAnswer(jsonInput)
                                } catch (e: Exception) {
                                    Log.e("AndroidBridge", "Error in submitAnswer", e)
                                }
                            }

                            @JavascriptInterface
                            fun exitGame() {
                                try {
                                    (ctx as? ComponentActivity)?.runOnUiThread { onExit() }
                                } catch (e: Exception) {
                                    Log.e("AndroidBridge", "Error in exitGame", e)
                                }
                            }
                        }

                        addJavascriptInterface(AndroidBridge(), "AndroidBridge")

                        webViewClient = object : WebViewClient() {
                            override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                                hasError = true
                                return true
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                if (request?.isForMainFrame == true) {
                                    hasError = true
                                }
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                return true // Block external navigation
                            }
                        }

                        loadUrl("file:///android_asset/MiniJuego/index.html")
                    }
                },
                update = { webView ->
                    webViewInstance = webView
                }
            )

            DisposableEffect(Unit) {
                onDispose {
                    webViewInstance?.apply {
                        removeJavascriptInterface("AndroidBridge")
                        stopLoading()
                        destroy()
                    }
                    webViewInstance = null
                }
            }
        }

        // Native Overlay "Salir" Button in top-right corner
        Surface(
            color = Color.Black.copy(alpha = 0.45f),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 12.dp, end = 16.dp)
        ) {
            IconButton(
                onClick = onExit,
                modifier = Modifier.size(48.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Salir",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
