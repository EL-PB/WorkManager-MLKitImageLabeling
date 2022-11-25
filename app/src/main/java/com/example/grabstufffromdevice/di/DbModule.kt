package com.example.grabstufffromdevice.di

import android.content.Context
import androidx.room.Room
import com.example.grabstufffromdevice.db.ImageDatabase
import com.example.grabstufffromdevice.db.ImageEntity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DbModule {
    @Provides
    @Singleton
    fun provide(@ApplicationContext context: Context): ImageDatabase {
        return Room
            .databaseBuilder(
                context,
                ImageDatabase::class.java,
                "ImageDatabase"
            )
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideDao(db: ImageDatabase) = db.imageDao()

    @Provides
    fun provideEntity() = ImageEntity()
}