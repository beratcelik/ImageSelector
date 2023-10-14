package com.example.imageselector
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.LocalImageLoader
import coil.compose.rememberImagePainter
import com.example.imageselector.ml.FlowerModel
import com.example.imageselector.ui.theme.ImageSelectorTheme
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.TensorImage


class MainActivity : ComponentActivity() {

    private lateinit var tflite: Interpreter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ImageSelectorTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    ImageSelectorMain(::classifyImage)
                }
            }
        }
    }

    fun classifyImage(imageUri: Uri): String {
        val source = ImageDecoder.createSource(contentResolver, imageUri)
        val originalBitmap = ImageDecoder.decodeBitmap(source)
        val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, 224, 224, true)

        val bitmapInARGB8888 = resizedBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val tensorImage = TensorImage.fromBitmap(bitmapInARGB8888)


        val model = FlowerModel.newInstance(this)
        val outputs = model.process(tensorImage)
        val probability = outputs.probabilityAsCategoryList
        model.close()

        val maxProb = probability.maxByOrNull { it.score }
        return maxProb?.label ?: "Unknown"
    }



}

@Composable
fun ImageSelectorMain(classifyFunction: (Uri) -> String){

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var flowerName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center)
    ) {
        Title(
            modifier = Modifier
                .fillMaxWidth()
                .align(alignment = Alignment.CenterHorizontally)
                .padding(8.dp)
        )
        ImageHolder(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
                .weight(1f),
            selectedImageUri = selectedImageUri
        )
        Text(text = flowerName) // Display the flower name

        ImageSelectorButton(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally),
            onImageSelected = { uri ->
                selectedImageUri = uri
            }
        )

        ClassifyImageButton(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally),
            onImageClassified = { result ->
                flowerName = result
            },
            selectedImageUri = selectedImageUri,
            classifyFunction = classifyFunction
        )



    }

}

@Composable
fun Title(modifier: Modifier){
    Text(
        text = "Image Selector",
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.h6,
        modifier = Modifier,
    )
}


@Composable
fun ImageHolder(
    modifier: Modifier,
    selectedImageUri: Uri?
){
    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    val painter = rememberImagePainter(
        data = selectedImageUri ?: R.drawable.sampleimage,
        imageLoader = LocalImageLoader.current
    )
    Image(
        painter = painter,
        contentDescription = if (selectedImageUri != null) null else "Sample Image",
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}

@Composable
fun ImageSelectorButton(
    modifier: Modifier,
    onImageSelected: (Uri) -> Unit
){
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                onImageSelected(uri) // Invoke the callback with the selected URI
            }
        }
    )

    Button(
        modifier = modifier, // Apply the modifier to the Button
        onClick = {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            val mimeTypes = arrayOf("image/jpeg", "image/png")
            intent.setTypeAndNormalize("image/*")
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            galleryLauncher.launch(intent.toString())
        }
    ) {
        Text(text = "Select a Photo")
    }
}

@Composable
fun ClassifyImageButton(
    modifier: Modifier,
    onImageClassified: (String) -> Unit,
    selectedImageUri: Uri?,
    classifyFunction: (Uri) -> String
) {
    Button(
        modifier = modifier,
        onClick = {
            if (selectedImageUri != null) {
                val classificationResult = classifyFunction(selectedImageUri)
                onImageClassified(classificationResult)
            } else {
                onImageClassified("Select an image first")
            }
        }
    ) {
        Text(text = "Classify Image")
    }
}



@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ImageSelectorTheme {
        ImageSelectorMain { _ -> "Mock Flower Name" }
    }
}