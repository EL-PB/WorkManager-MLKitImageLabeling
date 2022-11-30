package com.example.grabstufffromdevice.db

import androidx.room.*
import com.example.grabstufffromdevice.LabelFrequencyPair

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
    fun doesImageExist(imageId : Long) : Boolean

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertLabel(imageLabel: ImageLabelEntity)

    @Query("SELECT * FROM ImageLabelTable WHERE imageIdChild = :imageId ORDER BY label ASC")
    fun getImageSpecificLabels(imageId: Long) : MutableList<ImageLabelEntity>

    @Query("SELECT label, COUNT(label) as frequency FROM ImageLabelTable GROUP BY label ORDER BY frequency DESC LIMIT 7")
    fun getMostFrequentLabels(): MutableList<LabelFrequencyPair>

    @Query(
        "SELECT label, COUNT(label) as frequency " +
        "FROM ImageLabelTable " +
        "WHERE label = :labelName AND confidence >= 0.7"
    )
    fun getMFLOccurences(labelName: String): LabelFrequencyPair

    @Query(
        "SELECT * FROM imagetable " +
        "INNER JOIN imagelabeltable ON imagetable.imageIdParent = imagelabeltable.imageIdChild " +
        "WHERE label = :label AND confidence >= :confidence " +
        "ORDER BY confidence DESC"
    )
    fun getImagesFilteredByLabels(label: String, confidence: Float): MutableList<ImageEntity>
}