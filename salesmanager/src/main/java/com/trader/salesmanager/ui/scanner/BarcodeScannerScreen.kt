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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.trader.salesmanager.ui.theme.Cyan500
import com.trader.salesmanager.ui.theme.Emerald500
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

    // نتجنب تكرار نفس الباركود بفاصل زمني لا بمقارنة القيمة فقط
    var lastScanned    by remember { mutableStateOf("") }
    var lastScannedAt  by remember { mutableStateOf(0L) }
    val SCAN_COOLDOWN  = 2000L  // 2 ثانية بين كل قراءتين

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
        0f, 1f, infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse),
        label = "line"
    )

    Box(Modifier.fillMaxSize().background(Color.Black)) {

        if (hasPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val executor = Executors.newSingleThreadExecutor()

                    // ✅ تحديد صيغ الباركود المدعومة صراحةً
                    val options = BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(
                            Barcode.FORMAT_EAN_13,
                            Barcode.FORMAT_EAN_8,
                            Barcode.FORMAT_UPC_A,
                            Barcode.FORMAT_UPC_E,
                            Barcode.FORMAT_CODE_128,
                            Barcode.FORMAT_CODE_39,
                            Barcode.FORMAT_CODE_93,
                            Barcode.FORMAT_QR_CODE,
                            Barcode.FORMAT_DATA_MATRIX
                        )
                        .build()
                    val scanner = BarcodeScanning.getClient(options)

                    ProcessCameraProvider.getInstance(ctx).also { future ->
                        future.addListener({
                            val provider = future.get()
                            val preview = Preview.Builder().build()
                                .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                            val analysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                // ✅ دقة أعلى لتحسين قراءة الباركود
                                .setTargetResolution(android.util.Size(1280, 720))
                                .build().also { ia ->
                                    ia.setAnalyzer(executor) { proxy ->
                                        val mi = proxy.image
                                        if (mi != null) {
                                            val img = InputImage.fromMediaImage(mi, proxy.imageInfo.rotationDegrees)
                                            scanner.process(img)
                                                .addOnSuccessListener { codes ->
                                                    codes.firstOrNull()?.let { barcode ->
                                                        val raw = barcode.rawValue ?: return@addOnSuccessListener
                                                        // ✅ التحقق من صحة الباركود:
                                                        // 1. يجب أن لا يكون فارغاً
                                                        // 2. يجب أن يطابق الطول المتوقع لنوعه
                                                        if (isValidBarcode(barcode, raw)) {
                                                            val now = System.currentTimeMillis()
                                                            if (raw != lastScanned || now - lastScannedAt > SCAN_COOLDOWN) {
                                                                lastScanned = raw
                                                                lastScannedAt = now
                                                                onBarcodeDetected(raw)
                                                            }
                                                        }
                                                    }
                                                }
                                                .addOnCompleteListener { proxy.close() }
                                        } else proxy.close()
                                    }
                                }
                            try {
                                provider.unbindAll()
                                val cam = provider.bindToLifecycle(
                                    lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis
                                )
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
                    Modifier.size(300.dp, 180.dp).drawWithContent {
                        drawContent()
                        val lineY = scanLineY * size.height
                        drawLine(
                            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                listOf(Color.Transparent, Emerald500, Color.Transparent)
                            ),
                            start = Offset(0f, lineY), end = Offset(size.width, lineY),
                            strokeWidth = 3.dp.toPx()
                        )
                        drawRoundRect(
                            Emerald500.copy(0.6f),
                            cornerRadius = CornerRadius(12.dp.toPx()),
                            style = Stroke(3.dp.toPx())
                        )
                    }
                )
                Text("وجّه الكاميرا نحو الباركود",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 140.dp))
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

        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
            Arrangement.SpaceBetween, Alignment.CenterVertically) {
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

/**
 * ✅ التحقق من صحة الباركود حسب نوعه وطوله
 * يمنع قراءة باركود ناقص أو خاطئ
 */
private fun isValidBarcode(barcode: Barcode, raw: String): Boolean {
    if (raw.isBlank()) return false
    return when (barcode.format) {
        Barcode.FORMAT_EAN_13 -> raw.length == 13 && raw.all { it.isDigit() }
        Barcode.FORMAT_EAN_8  -> raw.length == 8  && raw.all { it.isDigit() }
        Barcode.FORMAT_UPC_A  -> raw.length == 12 && raw.all { it.isDigit() }
        Barcode.FORMAT_UPC_E  -> raw.length == 8  && raw.all { it.isDigit() }
        Barcode.FORMAT_CODE_128, Barcode.FORMAT_CODE_39, Barcode.FORMAT_CODE_93 ->
            raw.length >= 4   // حروف وأرقام — حد أدنى 4 أحرف
        Barcode.FORMAT_QR_CODE, Barcode.FORMAT_DATA_MATRIX ->
            raw.isNotEmpty()
        else -> raw.length >= 4
    }
}
