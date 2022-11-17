package com.example.grabstufffromdevice

data class ImageDataClass(
    val imageID: String? = null,
    val filePath: String? = null,
    val labelList: List<String>? = null,
    val contentDateAdded: String,
    val contentDateTaken: String,
    val lastEdit: String? = null
)