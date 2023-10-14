package com.example.imageselector
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

        /*
        // Returns the most probable result
        val maxProb = probability.maxByOrNull { it.score }
        return maxProb?.label ?: "Unknown"
         */

        // Returns the most probable three results
        return probability.sortedByDescending { it.score }.take(3).joinToString("\n") { "${it.label}: ${(it.score * 100).toInt()}%" }

    }

}

@Composable
fun ImageSelectorMain(classifyFunction: (Uri) -> String){

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    //var flowerName by remember { mutableStateOf("") }
    var flowerNames by remember { mutableStateOf(listOf("Possibility1", "Possibility2", "Possibility3")) }


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
        Box(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
                .weight(1f)
        ) {
            ImageHolder(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxSize(),
                selectedImageUri = selectedImageUri
            )
            ClassificationResults(
                modifier = Modifier
                    .align(Alignment.BottomStart)  // Align to the bottom
                    .fillMaxWidth(),
                names = flowerNames
            )
        }



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
                flowerNames = listOf(result)
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
        modifier = modifier,
    )
}

@Composable
fun ImageHolder(modifier: Modifier, selectedImageUri: Uri?){
    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    val painter = rememberImagePainter(
        data = selectedImageUri ?: R.drawable.sampleimage,
        imageLoader = LocalImageLoader.current
    )
    Image(
        painter = painter,
        contentDescription = if (selectedImageUri != null) null else "Sample Image",
        contentScale = ContentScale.Crop,  // Change to Crop
        modifier = modifier
    )
}


@Composable
fun ClassificationResults(modifier: Modifier, names: List<String>) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(8.dp)
    ) {
        for (name in names) {
            Text(
                text = name,
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.h6,
                color = Color.White,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    }
}


@Composable
fun ImageSelectorButton(modifier: Modifier, onImageSelected: (Uri) -> Unit){
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri: Uri? = result.data?.data
                if (uri != null) {
                    onImageSelected(uri) // Invoke the callback with the selected URI
                }
            }
        }
    )

    Button(
        modifier = modifier, // Apply the modifier to the Button
        onClick = {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            galleryLauncher.launch(intent)
        }
    ) {
        Text(text = "Select a Photo")
    }
}

@Composable
fun ClassifyImageButton(modifier: Modifier, onImageClassified: (String) -> Unit, selectedImageUri: Uri?, classifyFunction: (Uri) -> String) {
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

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ImageSelectorTheme {
        ImageSelectorMain { _ -> "Mock Flower Name" }
    }
}