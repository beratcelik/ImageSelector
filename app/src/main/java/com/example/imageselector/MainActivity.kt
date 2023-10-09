package com.example.imageselector

import android.content.Intent
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.imageselector.ui.theme.ImageSelectorTheme
import coil.compose.LocalImageLoader
import coil.compose.rememberImagePainter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ImageSelectorTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    ImageSelectorMain()
                }
            }
        }
    }
}

@Composable
fun ImageSelectorMain(){

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center)
    ) {
        Title(
            modifier = Modifier
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
        ImageSelectorButton(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally),
            onImageSelected = { uri ->
                selectedImageUri = uri
            }
        )

    }

}

@Composable
fun Title(modifier: Modifier){
    Text(
        text = "Image Selector",
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
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ImageSelectorTheme {
        ImageSelectorMain()
    }
}