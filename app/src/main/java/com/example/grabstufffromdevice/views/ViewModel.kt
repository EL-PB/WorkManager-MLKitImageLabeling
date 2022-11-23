package com.example.grabstufffromdevice.views

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.example.grabstufffromdevice.db.ImageDao
import com.example.grabstufffromdevice.db.ImageLabelEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ViewModel @Inject constructor(
    application: Application,
    private val imageDao: ImageDao
): AndroidViewModel(application) {

    private val _labeledImages = MutableLiveData<List<ImageAndLabels>>()
    val labeledImages: MutableLiveData<List<ImageAndLabels>> = _labeledImages

    fun getDataOfImagesAndLabels() {
        var imageAndLabelsList: MutableList<ImageAndLabels> = arrayListOf()

        val imagesFromDb = imageDao.getAllImages()

        imagesFromDb.forEach {
            var imageLabelArray = imageDao.getImageSpecificLabels(it.imageId)

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
}

data class ImageAndLabels(
    val imageId:String = "",
    val imagePath:String="",
    val labelList: MutableList<ImageLabelEntity>
)