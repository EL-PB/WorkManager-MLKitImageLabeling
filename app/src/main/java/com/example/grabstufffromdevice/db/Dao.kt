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

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertLabel(imageLabel: ImageLabelEntity)

    @Query("SELECT * FROM ImageLabelTable WHERE imageIdChild = :imageId ORDER BY label ASC")
    fun getImageSpecificLabels(imageId: String) : MutableList<ImageLabelEntity>
}