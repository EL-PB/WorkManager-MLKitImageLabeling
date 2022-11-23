package com.example.grabstufffromdevice.views

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.example.grabstufffromdevice.db.ImageEntity
import com.example.grabstufffromdevice.db.ImageLabelEntity

class ViewModel(application: Application): AndroidViewModel(application) {

    private val _labeledImages = MutableLiveData<List<ImageEntity>>()
    val labeledImages: MutableLiveData<List<ImageEntity>> = _labeledImages

    private val _imageLabels = MutableLiveData<List<List<ImageLabelEntity>>>()
    val imageLabels: MutableLiveData<List<List<ImageLabelEntity>>> = _imageLabels

    fun getDataOfImagesAndLabels() {
        var imagesList: MutableList<ImageEntity> = arrayListOf()



//        _labeledImageRows.value = imagesList
    }
}