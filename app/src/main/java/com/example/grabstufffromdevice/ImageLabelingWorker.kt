package com.example.grabstufffromdevice

import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.grabstufffromdevice.db.ImageDatabase
import com.example.grabstufffromdevice.db.ImageEntity
import com.example.grabstufffromdevice.db.ImageLabelEntity
import com.google.gson.Gson
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

object WorkerKeys {
    const val ERROR_MSG = "errorMsg"
    const val LABELING_URI = "labelingUri"
}

class ImageLabelingWorker(
    private val context: Context,
    private val workerParams: WorkerParameters
): CoroutineWorker(context, workerParams) {

    private lateinit var imageLabeler: ImageLabeler

    private val imageDB : ImageDatabase by lazy {
        Room.databaseBuilder(context,ImageDatabase::class.java,"ImageDatabase")
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override suspend fun doWork(): Result {
        startForegroundService()

        imageLabeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

        return withContext(Dispatchers.IO) {
            try {
                processDeviceImages(context)
                Result.success(workDataOf(WorkerKeys.LABELING_URI to "It worked"))
            } catch(e: Exception) {
                return@withContext Result.failure(workDataOf(WorkerKeys.ERROR_MSG to e.localizedMessage))
            }
        }
    }

    private suspend fun startForegroundService() {
        setForeground(
            ForegroundInfo(
                Random.nextInt(),
                NotificationCompat.Builder(context, "image_labelling_channel")
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentText("Labeling images")
                    .setContentTitle("Image Labeling in progress")
                    .build()
            )
        )
    }

    @RequiresApi(Build.VERSION_CODES.N)
    suspend fun processDeviceImages(
        context: Context
    ) {
        labelPhoneAlbumPhotos(context)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    suspend fun labelPhoneAlbumPhotos(
        context: Context
    ){
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_TAKEN
        )

        val imagesUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val orderBy = MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC"

        val queryCursor = context.applicationContext.contentResolver.query(
            imagesUri,
            projection,
            null,
            null,
            orderBy
        )

        val startingPoint = System.currentTimeMillis()

        queryCursor.use { cursor ->
            if (cursor != null && cursor.count > 0) {
                val contentIdColumn = cursor.getColumnIndex(MediaStore.Images.ImageColumns._ID)
                val contentDiskPathColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATA)

                if (cursor.moveToFirst()) {
                    var contentId: Long
                    var contentDiskPath: String

                    var exception: Exception? = null
                    var index = 1

                    val job: Job = GlobalScope.launch(Dispatchers.IO){
                        try {
                            do {
                                contentId = cursor.getLong(contentIdColumn)
                                contentDiskPath = cursor.getString(contentDiskPathColumn)

                                println("$index) $contentDiskPath")
                                labelImage(context, contentId, contentDiskPath)
                                index++
                            } while (cursor.moveToNext())
                        }
                        catch (e: Exception) {
                            exception = e
                            println("WHY DID YOU CRASH!!!???: ${e.message} ")
                        }
                    }
                    job.join()

                    exception?.let {
                        val localException = Exception("Crash while getting albums attributes", exception)
                        println(localException)
                    }
                }

                cursor.close()
            }
            else {
                cursor?.close()
            }
        }

        val endingPoint = System.currentTimeMillis()

        println("Image Disk Path\n----------------------")
        println("startingPoint: $startingPoint")
        println("endingPoint: $endingPoint")
        println("Total Time : " + (endingPoint - startingPoint) + "milliseconds \n")
    }

    suspend fun labelImage(
        context: Context,
        contentId: Long,
        contentDiskPath: String
    ) {
        println("new image being processed")
        imageDB.imageDao().insertImage(
            ImageEntity(
                contentId.toString(),
                contentDiskPath
            )
        )
        val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, ContentUris.withAppendedId(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentId))
        val inputImage = InputImage.fromBitmap(bitmap, 0)

        imageLabeler
            .process(inputImage)
            .addOnSuccessListener { imageLabels ->
                for(imageLabel in imageLabels) {
                    val text = imageLabel.text
                    val confidence = imageLabel.confidence
                    val index = imageLabel.index

                    imageDB.imageDao().insertLabel(
                        ImageLabelEntity(
                            imageId = contentId.toString(),
                            label =text,
                            confidence = confidence.toString(),
                            index = index.toString()
                        )
                    )
                    println("Text: $text\tConfidence: $confidence\tIndex: $index\n")
                }
            }
    }

}