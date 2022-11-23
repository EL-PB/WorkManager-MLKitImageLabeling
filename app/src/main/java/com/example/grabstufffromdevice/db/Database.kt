package com.example.grabstufffromdevice.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ImageEntity::class, ImageLabelEntity::class], version = 1)
abstract class ImageDatabase : RoomDatabase(){
    abstract  fun imageDao(): ImageDao
}