package com.trader.salesmanager.ui.scanner

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.trader.salesmanager.ui.theme.Emerald500
import com.trader.salesmanager.ui.theme.Cyan500
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
@Composable
fun BarcodeScannerScreen(
    onBarcodeDetected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission  by remember { mutableStateOf(false) }
    var torchEnabled   by remember { mutableStateOf(false) }
    var cameraControl  by remember { mutableStateOf<CameraControl?>(null) }
    var lastScanned    by remember { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
    }
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted) hasPermission = true else launcher.launch(Manifest.permission.CAMERA)
    }

    val scanAnim = rememberInfiniteTransition(label = "scan")
    val scanLineY by scanAnim.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse),
        label = "line"
    )

    Box(Modifier.fillMaxSize().background(Color.Black)) {

        if (hasPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val executor = Executors.newSingleThreadExecutor()
                    ProcessCameraProvider.getInstance(ctx).also { future ->
                        future.addListener({
                            val provider = future.get()
                            val preview = Preview.Builder().build()
                                .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                            val analysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build().also { ia ->
                                    ia.setAnalyzer(executor) { proxy ->
                                        val mi = proxy.image
                                        if (mi != null) {
                                            val img = InputImage.fromMediaImage(mi, proxy.imageInfo.rotationDegrees)
                                            BarcodeScanning.getClient().process(img)
                                                .addOnSuccessListener { codes ->
                                                    codes.firstOrNull()?.rawValue?.let { v ->
                                                        if (v != lastScanned) { lastScanned = v; onBarcodeDetected(v) }
                                                    }
                                                }
                                                .addOnCompleteListener { proxy.close() }
                                        } else proxy.close()
                                    }
                                }
                            try {
                                provider.unbindAll()
                                val cam = provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                                cameraControl = cam.cameraControl
                            } catch (e: Exception) { Log.e("Scanner", "bind failed", e) }
                        }, ContextCompat.getMainExecutor(ctx))
                    }
                    previewView
                }
            )

            // إطار المسح
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(280.dp, 170.dp)
                        .drawWithContent {
                            drawContent()
                            // خلفية شبه شفافة خارج الإطار
                            val fw = size.width; val fh = size.height
                            drawRect(Color.Black.copy(0.55f), size = Size(fw, 0f - (fh * 3)))
                            // خط المسح
                            val lineY = scanLineY * fh
                            drawLine(
                                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    listOf(Color.Transparent, Emerald500, Color.Transparent)
                                ),
                                start = Offset(0f, lineY),
                                end = Offset(fw, lineY),
                                strokeWidth = 3.dp.toPx()
                            )
                            // إطار الزوايا
                            val cr = 12.dp.toPx(); val cs = 24.dp.toPx(); val sw = 3.dp.toPx()
                            val stroke = Stroke(sw)
                            // كامل الإطار
                            drawRoundRect(Emerald500.copy(0.5f), cornerRadius = CornerRadius(cr), style = stroke)
                        }
                )
                Text(
                    "وجّه الكاميرا نحو الباركود",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 140.dp)
                )
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("يلزم إذن الكاميرا", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) { Text("منح الإذن") }
                }
            }
        }

        // شريط التحكم
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
            Arrangement.SpaceBetween, Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(0.5f))
            ) { Icon(Icons.Rounded.Close, null, tint = Color.White) }

            Text("مسح الباركود", color = Color.White,
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            IconButton(
                onClick = { torchEnabled = !torchEnabled; cameraControl?.enableTorch(torchEnabled) },
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(0.5f))
            ) {
                Icon(if (torchEnabled) Icons.Rounded.FlashOff else Icons.Rounded.FlashOn,
                    null, tint = if (torchEnabled) Cyan500 else Color.White)
            }
        }
    }
}
