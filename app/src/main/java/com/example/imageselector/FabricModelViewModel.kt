package com.example.imageselector

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import androidx.camera.core.ExperimentalGetImage
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.imageselector.ml.FabricModel
import com.example.imageselector.ml.FlowerModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder


@ExperimentalGetImage class FabricModelViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(FabricModelUiState())
    val uiState = _uiState.asStateFlow()

    fun setImageUri(uri: Uri) {
        _uiState.value = _uiState.value.copy(imageUri = uri)
    }

    companion object {
        const val IMAGE_WIDTH = 300
        const val IMAGE_HEIGHT = 400
    }

    private suspend fun saveImageToInternalStorage(bitmap: Bitmap, context: Context): Uri {
        val filename = "resized_image.png"
        context.openFileOutput(filename, Context.MODE_PRIVATE).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
        return Uri.fromFile(File(context.filesDir, filename))
    }

    private fun convertByteBufferToBitmap(byteBuffer: ByteBuffer): Bitmap {
        byteBuffer.rewind() // Rewind the buffer to read from the beginning
        val bitmap = Bitmap.createBitmap(IMAGE_WIDTH, IMAGE_HEIGHT, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(IMAGE_WIDTH * IMAGE_HEIGHT)

        for (i in 0 until IMAGE_WIDTH * IMAGE_HEIGHT) {
            val r = (byteBuffer.float * 127.5f + 127.5f).toInt()
            val g = (byteBuffer.float * 127.5f + 127.5f).toInt()
            val b = (byteBuffer.float * 127.5f + 127.5f).toInt()
            pixels[i] = 0xFF shl 24 or (r shl 16) or (g shl 8) or b
        }

        bitmap.setPixels(pixels, 0, IMAGE_WIDTH, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT)
        return bitmap
    }


    fun classifyImage(imageUri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext

            // 1. Load the image
            val imageStream = context.contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(imageStream)
            // After creating the bitmap, log its properties
            Log.d("ImageInfo", "Bitmap width: ${bitmap.width}") // Bitmap width: 960
            Log.d("ImageInfo", "Bitmap height: ${bitmap.height}") // Bitmap height: 1280
            Log.d("ImageInfo", "Bitmap config: ${bitmap.config}") // Bitmap config: ARGB_8888
            val mimeType = context.contentResolver.getType(imageUri)
            Log.d("ImageInfo", "MIME type: $mimeType") // MIME type: image/jpeg
            /*
            ARGB_4444: This configuration is deprecated and was removed from the API level 13.
            Each pixel is stored on 2 bytes and both the RGB and alpha channels are encoded,
            each channel with 4 bits of precision (16 possible values).
            ARGB_8888: Each pixel is stored on 4 bytes. Like the ARGB_4444,
            this configuration allows both RGB and alpha channels to be stored,
            but each channel is stored with 8 bits of precision (256 possible values),
            which results in a total of 16,777,216 possible colors with full alpha channel precision.
             */

            // 2. Resize the image
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, IMAGE_WIDTH, IMAGE_HEIGHT, true)
            Log.d("ImageResize", "Resized bitmap width: ${resizedBitmap.width}")
            Log.d("ImageResize", "Resized bitmap height: ${resizedBitmap.height}")
            // Save the resized image to internal storage and get a URI to it
            val resizedImageUri = saveImageToInternalStorage(resizedBitmap, context) // Update the UI state with the new URI



            // 3. Preprocess the image and convert to ByteBuffer
            val byteBuffer = convertBitmapToByteBuffer(resizedBitmap)
            // Log buffer info
            Log.d("ByteBuffer", "Capacity: ${byteBuffer.capacity()}, Position: ${byteBuffer.position()}")
            // Debug: Convert the ByteBuffer back to Bitmap and check it
            val debugBitmap = convertByteBufferToBitmap(byteBuffer)
            val debugBitmapUri = saveImageToInternalStorage(debugBitmap, context) // Update the UI state with the new URI

            // 4. Load TensorFlow Lite model
            val model = FabricModel.newInstance(context)
            val flowerModel = FlowerModel.newInstance(context)

            // 5. Create inputs for reference
            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, IMAGE_WIDTH, IMAGE_HEIGHT, 3), DataType.FLOAT32)
            inputFeature0.loadBuffer(byteBuffer)
            val inputFeatureFlower0 = TensorImage.fromBitmap(resizedBitmap)

            // 6. Run model inference and get result
            val outputs = model.process(inputFeature0)
            val outputFeature0 = outputs.outputFeature0AsTensorBuffer.floatArray
            val outputsFlower = flowerModel.process(inputFeatureFlower0).probabilityAsCategoryList.apply {
                sortByDescending { it.score }
            }.take(1)

            // 7. Interpret the model's output
            var fabricType = if (outputFeature0[0] > 0) {
                "Positive Fabric "
            } else {
                "Negative Fabric "
            }
            // Assuming outputsFlower[0].score is between 0 and 1, multiply by 100 to get a percentage
            fabricType += ": ${String.format("%.3f", outputFeature0[0])}, ${outputsFlower[0].label}: ${String.format("%.0f", outputsFlower[0].score * 100)}%"


            Log.d("classifyImage", "Values returned from the model: ${outputFeature0[0]}")
            Log.d("classifyImage", "Values returned from the Flower model: $outputsFlower")

            // 8. Update UI state
            _uiState.update { currentState ->
                currentState.copy(
                    fabricType = fabricType,
                    //imageUri = resizedImageUri,  // This should be a URI pointing to the resized image
                    //imageUri = debugBitmapUri  // This should be a URI pointing to the resized image

                )

            }

            // 9. Release model resources
            model.close()
            flowerModel.close()
        }
    }

    // bitmap range [-1,1]
    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * IMAGE_WIDTH * IMAGE_HEIGHT * 3)
        byteBuffer.order(ByteOrder.nativeOrder())
        val intValues = IntArray(IMAGE_WIDTH * IMAGE_HEIGHT)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var pixel = 0
        for (i in 0 until IMAGE_WIDTH) {
            for (j in 0 until IMAGE_HEIGHT) {
                val `val` = intValues[pixel++]
                byteBuffer.putFloat(((`val` shr 16 and 0xFF) / 127.5f) - 1f)
                byteBuffer.putFloat(((`val` shr 8 and 0xFF) / 127.5f) - 1f)
                byteBuffer.putFloat(((`val` and 0xFF) / 127.5f) - 1f)
            }
        }
        return byteBuffer
    }


    /*
            // bitmap range [0,1]
            private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
                val byteBuffer = ByteBuffer.allocateDirect(4 * IMAGE_WIDTH * IMAGE_HEIGHT  * 3)
                byteBuffer.order(ByteOrder.nativeOrder())
                val intValues = IntArray(IMAGE_WIDTH * IMAGE_HEIGHT )
                bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                var pixel = 0
                for (i in 0 until IMAGE_WIDTH) {
                    for (j in 0 until IMAGE_HEIGHT) {
                        val `val` = intValues[pixel++]
                        byteBuffer.putFloat((`val` shr 16 and 0xFF) / 255.0f)
                        byteBuffer.putFloat((`val` shr 8 and 0xFF) / 255.0f)
                        byteBuffer.putFloat((`val` and 0xFF) / 255.0f)
                    }
                }
                return byteBuffer
            }

     */

}
