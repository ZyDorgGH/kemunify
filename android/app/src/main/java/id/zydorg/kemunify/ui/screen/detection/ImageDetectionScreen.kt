package id.zydorg.kemunify.ui.screen.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import id.zydorg.kemunify.ObjectDetectorHelper
import id.zydorg.kemunify.ui.theme.KemunifyTheme
import org.tensorflow.lite.task.vision.detector.Detection
import java.io.IOException
import java.util.concurrent.Executors

@Composable
fun ImageDetectionScreen(
    modifier: Modifier = Modifier,
    photoTaken: String?,
    navigateToCamera: () -> Unit,
) {

    val context = LocalContext.current

    val detectedObject = remember {
        mutableStateOf<List<Detection>>(emptyList())
    }

    val frameHeight = remember {
        mutableIntStateOf(0)
    }
    val frameWidth = remember {
        mutableIntStateOf(0)
    }

    val bitmap = remember {
        mutableStateOf<Bitmap?>(null)
    }

    val uri = remember {
        mutableStateOf<Uri?>(null)
    }

    Log.d("CameraX", "Image saved at: $photoTaken")

    if (photoTaken != "null" && photoTaken != "{photo}"){
        LaunchedEffect(photoTaken) {
            photoTaken?.toUri().let {
                val selectedBitmap = if (Build.VERSION.SDK_INT < 28) {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                } else {
                    val source = it.let { uri ->
                        uri?.let { it1 -> ImageDecoder.createSource(context.contentResolver, it1) }
                    }
                    source.let { image ->
                        image?.let { it1 -> ImageDecoder.decodeBitmap(it1) }
                    }
                }
                bitmap.value = selectedBitmap
                uri.value = it
            }
        }
    }

    val launchImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) {
        val selectedBitmap = if (Build.VERSION.SDK_INT < 28 ){
            MediaStore.Images.Media.getBitmap(context.contentResolver, it)
        }
        else {
            val source = it?.let { uri ->
                ImageDecoder.createSource(context.contentResolver, uri)
            }
            source?.let { image ->
                ImageDecoder.decodeBitmap(image)
            }!!
        }
        bitmap.value = selectedBitmap
        uri.value = it
    }
    if (bitmap.value != null){
        detectImage(
            context = context,
            bitmap = bitmap,
            uri = uri,
            detectedObject = detectedObject,
            frameWidth = frameWidth,
            frameHeight = frameHeight
        )
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Back Button
        IconButton(
            onClick = {  },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        // Main Content
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pick Photo Button
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(Color.LightGray, RoundedCornerShape(16.dp))
            ) {

                if (bitmap.value != null){
                    Image(
                        bitmap = bitmap.value!!.asImageBitmap(),
                        contentDescription = "Selected Image",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )

                } else {
                    Image(
                        imageVector = Icons.Default.ImageNotSupported,
                        contentDescription = "Selected Image",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Canvas(
                    modifier = Modifier.fillMaxSize(),
                    onDraw = {
                        val scaleX = constraints.maxWidth / frameWidth.intValue.toFloat()
                        val scaleY = constraints.maxHeight / frameHeight.intValue.toFloat()
                        detectedObject.value.mapIndexed { index, detection ->

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
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tombol
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = {
                        launchImage.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )},
                    border = BorderStroke(1.dp, Color(0xFF2E7D32))
                ) {
                    Text("Pick photo", color = Color(0xFF2E7D32))
                }
                Button(
                    onClick = { navigateToCamera() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32),
                        contentColor = Color.White
                    )
                ) {
                    Text("Analyze")
                }
            }
        }
    }
}

fun getRotationDegreesFromUri(context: Context, uri: Uri): Int {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val exif = inputStream?.let { ExifInterface(it) }

        when (exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    } catch (e: IOException) {
        e.printStackTrace()
        0
    }
}

private fun detectImage(
    context: Context,
    bitmap: MutableState<Bitmap?>,
    uri: MutableState<Uri?>,
    detectedObject: MutableState<List<Detection>>,
    frameHeight: MutableState<Int>,
    frameWidth: MutableState<Int>

){
    bitmap.value?.let { selectedBitmap ->

        val rotation = uri.value?.let { getRotationDegreesFromUri(context, it) }
        val objectDetector = ObjectDetectorHelper(context) { results, imageHeight, imageWidth ->
            detectedObject.value = results ?: emptyList()
            frameHeight.value = imageHeight
            frameWidth.value = imageWidth
        }

        val convertedBitmap = if (selectedBitmap.config != Bitmap.Config.ARGB_8888) {
            selectedBitmap.copy(Bitmap.Config.ARGB_8888, true)
        } else {
            selectedBitmap
        }

        Executors.newSingleThreadExecutor().execute {
            if (rotation != null) {
                val startTime = System.nanoTime()
                objectDetector.detect(convertedBitmap, rotation)
                val endTime = System.nanoTime()
                val inferenceTimeMs = (endTime - startTime) / 1_000_000

                Log.d("InferenceTime", "Waktu inferensi: $inferenceTimeMs ms")
            }

        }
    }
}

// Preview
@Preview(showBackground = true)
@Composable
fun PreviewImageAnalysisScreen() {
    KemunifyTheme {
        ImageDetectionScreen(
            photoTaken = "",
            navigateToCamera = {},
        )
    }
}