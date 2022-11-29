package com.example.grabstufffromdevice.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "ImageTable")
data class ImageEntity(
    @PrimaryKey
    @ColumnInfo(name = "imageIdParent")
    val imageId :Long = 0,
    val imagePath:String=""
)

@Entity(
    tableName = "ImageLabelTable",
    primaryKeys = ["imageIdChild","label"],
    foreignKeys = arrayOf(
        ForeignKey(
            entity = ImageEntity::class,
            parentColumns = arrayOf("imageIdParent"),
            childColumns = arrayOf("imageIdChild"),
            onDelete = ForeignKey.CASCADE
        )
    )
)
data class ImageLabelEntity(
    @ColumnInfo(name = "imageIdChild")
    val imageId :Long = 0,
    @ColumnInfo(name = "label")
    val label:String="",
    @ColumnInfo(name = "confidence")
    val confidence:Float=0f,
    @ColumnInfo(name = "labelIndex")
    val index:Int=0,
)