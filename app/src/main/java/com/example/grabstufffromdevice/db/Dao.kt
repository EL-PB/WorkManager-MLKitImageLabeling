package com.example.grabstufffromdevice.db

import androidx.room.*

@Dao
interface ImageDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertImage(imageEntity: ImageEntity)

    @Update
    fun updateImage(imageEntity: ImageEntity)

    @Query("DELETE FROM imagetable")
    fun nukeImageTable()

    @Query("DELETE FROM imagelabeltable")
    fun nukeImageLabelTable()

    @Query("SELECT * FROM imagetable ORDER BY imageIdParent ASC")
    fun getAllImages() : MutableList<ImageEntity>

    @Query("SELECT EXISTS(SELECT * FROM imagetable WHERE imageIdParent = :imageId)")
    fun doesImageExist(imageId : String) : Boolean

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertLabel(imageLabel: ImageLabelEntity)

    @Query("SELECT * FROM ImageLabelTable WHERE imageIdChild = :imageId ORDER BY label ASC")
    fun getImageSpecificLabels(imageId: String) : MutableList<ImageLabelEntity>

    @Query("UPDATE CountCompleted SET completed = completed + 1")
    fun updateCompleted()

    @Query("SELECT completed FROM CountCompleted")
    fun countCompleted(): Int
}