package com.example.imageselector

import android.net.Uri

data class FabricModelUiState(
    val imageUri: Uri? = null,
    val flowerNames: List<String> = listOf("Possibility1", "Possibility2", "Possibility3"),
    val fabricType: String = ""
)
