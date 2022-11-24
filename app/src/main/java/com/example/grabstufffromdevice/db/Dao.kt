package com.example.grabstufffromdevice.db

import androidx.room.*

@Dao
interface ImageDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertImage(imageEntity: ImageEntity)

    @Update
    fun updateImage(imageEntity: ImageEntity)

    @Delete
    fun deleteImage(imageEntity: ImageEntity)

    @Query("SELECT * FROM imagetable ORDER BY imageIdParent ASC")
    fun getAllImages() : MutableList<ImageEntity>

    @Query("SELECT EXISTS(SELECT * FROM imagetable WHERE imageIdParent = :imageId)")
    fun doesImageExist(imageId : String) : Boolean

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertLabel(imageLabel: ImageLabelEntity)

    @Query("SELECT * FROM ImageLabelTable WHERE imageIdChild = :imageId ORDER BY label ASC")
    fun getImageSpecificLabels(imageId: String) : MutableList<ImageLabelEntity>
}