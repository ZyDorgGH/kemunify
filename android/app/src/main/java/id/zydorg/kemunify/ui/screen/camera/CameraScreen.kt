package id.zydorg.kemunify.ui.screen.camera

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Paint
import android.util.Log
import android.widget.Toast
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import id.zydorg.kemunify.ObjectDetectorHelper
import id.zydorg.kemunify.utils.createCustomTempFile
import org.tensorflow.lite.task.vision.detector.Detection
import java.util.concurrent.Executors

@SuppressLint("SuspiciousIndentation")
@Composable
fun CameraScreen(
    modifier: Modifier = Modifier,
    navigateToDetail: (String) -> Unit,
){
    var detectedObject by remember {
        mutableStateOf<List<Detection>>(emptyList())
    }

    var frameHeight by remember {
        mutableIntStateOf(0)
    }
    var frameWidth by remember {
        mutableIntStateOf(0)
    }

    var active by remember {
        mutableStateOf(true)
    }

    var bitmapBuffer by remember {
        mutableStateOf<Bitmap?>(null)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    DisposableEffect(Unit) {
        onDispose {
            active = false;
            cameraProviderFuture.get().unbindAll()
        }
    }

    val cameraController = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(
                CameraController.IMAGE_CAPTURE or
                    CameraController.IMAGE_ANALYSIS
            )
        }
    }

    BoxWithConstraints(
        modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ){
            AndroidView(
                modifier =  Modifier.fillMaxSize(),
                factory = {ctx ->
                    val previewView = PreviewView(ctx)
                    val executor = ContextCompat.getMainExecutor(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        imageCapture = ImageCapture.Builder()
                            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()

                        val cameraSelector = CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                            .build()

                        val imageAnalyzer =
                            ImageAnalysis.Builder()
                                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                                .build()

                        val backgroundExecutor = Executors.newSingleThreadExecutor()

                        backgroundExecutor.execute {
                            val objectDetector =
                                ObjectDetectorHelper(ctx){ results, imageHeight, imageWidth ->
                                    detectedObject = results?: emptyList()
                                    frameHeight = imageHeight
                                    frameWidth = imageWidth
                                }

                            imageAnalyzer.setAnalyzer(
                                backgroundExecutor,
                                ImageAnalysis.Analyzer { imageProxy ->
                                    if (bitmapBuffer == null) {
                                        bitmapBuffer = Bitmap.createBitmap(
                                            imageProxy.width,
                                            imageProxy.height,
                                            Bitmap.Config.ARGB_8888
                                        )
                                    }
                                    imageProxy.use {
                                        val startTime = System.nanoTime()
                                        detectObjects(imageProxy, bitmapBuffer, objectDetector)
                                        val endTime = System.nanoTime()
                                        val inferenceTimeMs = (endTime - startTime) / 1_000_000

                                        Log.d("InferenceTime", "Waktu inferensi: $inferenceTimeMs ms")
                                    }
                                }
                            )
                        }
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            imageAnalyzer,
                            preview,
                            imageCapture
                        )
                    }, executor)
                    previewView
                }
            )
        }

        Canvas(
            modifier = Modifier.fillMaxSize(),
            onDraw = {
                val scaleX = constraints.maxWidth / frameWidth.toFloat()
                val scaleY = constraints.maxHeight / frameHeight.toFloat()
                detectedObject.mapIndexed { index, detection ->

                    val left = detection.boundingBox.left * scaleX
                    val top = detection.boundingBox.top * scaleY
                    val width = detection.boundingBox.width() * scaleX
                    val height = detection.boundingBox.height() * scaleY

                    val category = detection.categories.firstOrNull()
                    val label = category?.label ?: "Unknown"
                    val confidence = category?.score?.times(100)?.toInt()?.coerceIn(0, 100) ?: 0
                    Log.d(
                        "Object",
                        detection.categories.first().label + " --- " + detection.categories.first().label
                    )

                    if (label != "???"){
                        drawRect(
                            color = Color.White,
                            topLeft = Offset(left, top),
                            size = Size(width, height),
                            style = Stroke(width = 8f)
                        )
                        drawIntoCanvas {
                            val paint = Paint().apply {
                                color = android.graphics.Color.RED
                                textSize = 50f
                                isFakeBoldText = true
                                isAntiAlias = true
                                style = Paint.Style.FILL_AND_STROKE
                                strokeWidth = 3f
                                setShadowLayer(10f, 4f, 4f, android.graphics.Color.BLACK)
                            }
                            it.nativeCanvas.drawText(
                                "$label $confidence%",
                                left + 10f,
                                top + 50f,
                                paint
                            )
                        }
                    }
                }
            }
        )

        IconButton(
            onClick = {
                val image = imageCapture ?: return@IconButton
                val photoFile = createCustomTempFile(context)
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                image.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            Toast.makeText(context, "Gambar berhasil disimpan", Toast.LENGTH_SHORT).show()
                            Log.d("CameraX", "Image saved at: ${output.savedUri}")
                            navigateToDetail(output.savedUri.toString())

                        }

                        override fun onError(exception: ImageCaptureException) {
                            Toast.makeText(context, "Gagal mengambil gambar", Toast.LENGTH_SHORT).show()
                            Log.e("CameraX", "Image capture failed: ${exception.message}", exception)
                        }
                    }
                )
            }, modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Camera,
                contentDescription = "Capture Image",
                tint = Color.White,
                modifier = Modifier.size(64.dp)
            )
        }
    }
}

private fun detectObjects(
    image: ImageProxy,
    bitmapBuffer: Bitmap?,
    detector: ObjectDetectorHelper
) {
    image.use { proxy ->
        bitmapBuffer?.let {
            // Salin data gambar ke bitmap buffer
            it.copyPixelsFromBuffer(proxy.planes[0].buffer)

            // Dapatkan rotasi gambar
            val rotation = proxy.imageInfo.rotationDegrees

            // Proses deteksi di background thread

            detector.detect(it, rotation)

        }
    }
}