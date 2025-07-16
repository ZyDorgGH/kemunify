package id.zydorg.kemunify

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector

class ObjectDetectorHelper(
    private val context: Context,
    private var currentDelegate: Int = 0,
    private val objectDetectorListener: (
        results: MutableList<Detection>?,
        imageHeight: Int,
        imageWidth: Int
    ) -> Unit,
) {

    private val objectDetector: ObjectDetector? by lazy { initializeDetector() }

    private fun initializeDetector(): ObjectDetector? {

        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setMaxResults(5)
            .setScoreThreshold(0.7f)

        val baseOptionsBuilder = BaseOptions.builder()
            .setNumThreads(2)

        when (currentDelegate) {
            DELEGATE_CPU -> {

            }
            DELEGATE_GPU -> {
                if (CompatibilityList().isDelegateSupportedOnThisDevice) {
                    baseOptionsBuilder.useGpu()
                } else {
                    Log.e("ObjectDetector", "Error initializing detector: DELEGATE_GPU")
                }
            }
            DELEGATE_NNAPI -> {
                baseOptionsBuilder.useNnapi()
            }
        }

        options.setBaseOptions(baseOptionsBuilder.build())


        return try {
            ObjectDetector.createFromFileAndOptions(
                context,
                "kemunify_mobilnet_model_metadata.tflite", // Pastikan nama file sesuai
                options.build()
            )
        } catch (e: Exception) {
            Log.e("ObjectDetector", "Error initializing detector: ${e.message}")
            objectDetectorListener.invoke(null, 0, 0)
            null
        }
    }

    fun detect(image: Bitmap, imageRotation: Int) {
        objectDetector?.let {
            // Preprocessing
            val imageProcessor = ImageProcessor.Builder()
                .add(Rot90Op(-imageRotation / 90))
                .build()

            val tensorImage = imageProcessor.process(TensorImage.fromBitmap(image))

            // Inference
            val results = objectDetector?.detect(tensorImage)
            Log.e("ObjectDetector", "Detection results: ${results?.size ?: 0} objects found.")

            objectDetectorListener.invoke(
                results,
                tensorImage.height,
                tensorImage.width
            )

            Log.d("ObjectDetected", "Object detected: $results")
        }
    }
    companion object {
        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
        const val DELEGATE_NNAPI = 2
    }
    interface DetectorListener {
        fun onError(error: String)
        fun onResults(
            results: MutableList<Detection>?,
            inferenceTime: Long,
            imageHeight: Int,
            imageWidth: Int
        )
    }

}