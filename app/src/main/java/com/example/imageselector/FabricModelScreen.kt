package com.example.imageselector


import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.text.style.TextAlign
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.imageselector.ui.theme.ImageSelectorTheme


@Composable
fun ImageSelectorMain(viewModel: FabricModelViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    ImageSelectorScreen(
        imageUri = uiState.imageUri,
        fabricType = uiState.fabricType,
        onImageSelected = { uri ->
            viewModel.setImageUri(uri)
        },
        onClassifyImage = { uri ->
            viewModel.classifyImage(uri)
        }
    )
}

@Composable
fun ImageSelectorScreen(
    imageUri: Uri?,
    fabricType: String,
    onImageSelected: (Uri) -> Unit,
    onClassifyImage: (Uri) -> Unit
) {
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
                selectedImageUri = imageUri
            )
            ClassificationResults(
                modifier = Modifier
                    .align(Alignment.BottomStart)  // Align to the bottom
                    .fillMaxWidth(),
                fabricType = fabricType
            )
        }

        ImageSelectorButton(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally),
            onImageSelected = onImageSelected
        )

        ClassifyImageButton(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally),
            onImageClassified = {
                // This callback could be used to update the UI if needed
            },
            selectedImageUri = imageUri,
            classifyFunction = onClassifyImage
        )
    }
}

@Composable
fun Title(modifier: Modifier){
    Text(
        text = "# FabricAI",
        textAlign = TextAlign.Center,
        fontSize = 24.sp,
        modifier = modifier,
    )
    /*
    Text(
        text = "Felpa Moda ve Tekstil",
        textAlign = TextAlign.Center,
        fontSize = 16.sp,
        modifier = Modifier.fillMaxWidth() // To center the text
    )
    Text(
        text = "Artificial Intelligence Project",
        textAlign = TextAlign.Center,
        fontSize = 16.sp,
        modifier = Modifier.fillMaxWidth() // To center the text
    )

     */
}

@Composable
fun ImageHolder(modifier: Modifier, selectedImageUri: Uri?){
    val painter = rememberAsyncImagePainter(ImageRequest.Builder(LocalContext.current).data(
        data = selectedImageUri ?: R.drawable.ic_launcher_foreground // replace with your default image
    ).apply(block = fun ImageRequest.Builder.() {
        crossfade(true)
    }).build()
    )
    Image(
        painter = painter,
        contentDescription = if (selectedImageUri != null) null else "Sample Image",
        contentScale = ContentScale.Crop,  // Change to Crop
        modifier = modifier
    )
}

@Composable
fun ClassificationResults(modifier: Modifier, fabricType: String) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(8.dp)
    ) {
            Text(
                text = fabricType,
                textAlign = TextAlign.Start,
                fontSize = 20.sp,
                color = Color.White,
                modifier = Modifier.padding(vertical = 2.dp)
            )
    }
}

@Composable
fun ImageSelectorButton(modifier: Modifier, onImageSelected: (Uri) -> Unit){
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
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
fun ClassifyImageButton(modifier: Modifier, onImageClassified: (String) -> Unit, selectedImageUri: Uri?, classifyFunction: (Uri) -> Unit) {
    Button(
        modifier = modifier,
        onClick = {
            if (selectedImageUri != null) {
                classifyFunction(selectedImageUri)
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
        ImageSelectorMain()
    }
}
