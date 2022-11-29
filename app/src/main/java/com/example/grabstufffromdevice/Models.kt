package com.example.grabstufffromdevice

import com.example.grabstufffromdevice.db.ImageLabelEntity

data class LabelFrequencyPair(
    val label:String = "",
    val frequency: Int = 0
)

data class ImageAndLabels(
    val imageId:Long = 0,
    val imagePath:String="",
    val labelList: MutableList<ImageLabelEntity>
)