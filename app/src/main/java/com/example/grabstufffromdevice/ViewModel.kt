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
import com.google.gson.Gson
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.tasks.await

class ViewModel(application: Application): AndroidViewModel(application) {
    private var imageLabeler: ImageLabeler

    private val _labeledImageRows = MutableLiveData<List<ImageDataClass>>()
    val labeledImageRows: MutableLiveData<List<ImageDataClass>> = _labeledImageRows

    init {
        imageLabeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
    }

    fun getDataOfImagesAndLabels() {
        
    }
}