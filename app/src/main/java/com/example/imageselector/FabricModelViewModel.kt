package com.example.imageselector

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.imageselector.ml.FabricModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder


class FabricModelViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(FabricModelUiState())
    val uiState = _uiState.asStateFlow()

    fun setImageUri(uri: Uri) {
        _uiState.value = _uiState.value.copy(imageUri = uri)
    }

    companion object {
        const val IMAGE_WIDTH = 300
        const val IMAGE_HEIGHT = 400
    }

    fun classifyImage(imageUri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext

            // 1. Load the image
            val imageStream = context.contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(imageStream)

            // 2. Resize the image
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, IMAGE_WIDTH, IMAGE_HEIGHT, true)

            // 3. Preprocess the image and convert to ByteBuffer
            val byteBuffer = convertBitmapToByteBuffer(resizedBitmap)

            // 4. Load TensorFlow Lite model
            val model = FabricModel.newInstance(context)

            // 5. Create inputs for reference
            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, IMAGE_WIDTH, IMAGE_HEIGHT, 3), DataType.FLOAT32)
            inputFeature0.loadBuffer(byteBuffer)

            // 6. Run model inference and get result
            val outputs = model.process(inputFeature0)
            val outputFeature0 = outputs.outputFeature0AsTensorBuffer.floatArray

            // 7. Interpret the model's output
            val fabricType = if (outputFeature0[0] > 0) "Positive Fabric" else "Negative Fabric"

            Log.d("classifyImage", "Values returned from the model: ${outputFeature0[0]}")

            // 8. Update UI state
            _uiState.update { currentState ->
                currentState.copy(fabricType = fabricType)
            }

            // 9. Release model resources
            model.close()
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
