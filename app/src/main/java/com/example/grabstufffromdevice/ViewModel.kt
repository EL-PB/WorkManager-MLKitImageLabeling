package com.example.grabstufffromdevice

import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.grabstufffromdevice.db.ImageDatabase
import com.example.grabstufffromdevice.db.ImageLabelEntity
import com.google.gson.Gson
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.tasks.await

class ViewModel constructor(application: Application): AndroidViewModel(application) {

    private val imageDB : ImageDatabase by lazy {
        Room.databaseBuilder(application, ImageDatabase::class.java,"ImageDatabase")
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()
    }

    private val _labeledImages = MutableLiveData<List<ImageAndLabels>>()
    val labeledImages: MutableLiveData<List<ImageAndLabels>> = _labeledImages

    fun getDataOfImagesAndLabels() {
        var imageAndLabelsList: MutableList<ImageAndLabels> = arrayListOf()

        val imagesFromDb = imageDB.imageDao().getAllImages()

        imagesFromDb.forEach {
            var imageLabelArray = imageDB.imageDao().getImageSpecificLabels(it.imageId)

            imageAndLabelsList.add(
                ImageAndLabels(
                    imageId = it.imageId,
                    imagePath = it.imagePath,
                    labelList = imageLabelArray
                )
            )
        }

        _labeledImages.value = imageAndLabelsList
    }

    fun clearDatabase() {
        imageDB.imageDao().nukeImageTable()
        imageDB.imageDao().nukeImageLabelTable()
    }
}

data class ImageAndLabels(
    val imageId:String = "",
    val imagePath:String="",
    val labelList: MutableList<ImageLabelEntity>
)