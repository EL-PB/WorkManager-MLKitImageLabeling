package com.example.grabstufffromdevice

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.grabstufffromdevice.db.ImageDao
import com.example.grabstufffromdevice.db.ImageEntity
import com.example.grabstufffromdevice.db.ImageLabelEntity
import com.google.gson.Gson
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.*
import javax.inject.Inject
import kotlin.random.Random

object WorkerKeys {
    const val ERROR_MSG = "errorMsg"
    const val LABELING_URI = "labelingUri"
}

@HiltWorker
@Suppress("BlockingMethodInNonBlockingContext")
class ImageLabelingWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workerParams: WorkerParameters,

    private val imageDao:ImageDao,
    private val notificationBuilder: NotificationCompat.Builder,
    private val notificationManager: NotificationManagerCompat
): CoroutineWorker(context, workerParams) {
    private lateinit var imageLabeler: ImageLabeler

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
    suspend fun processDeviceImages(context: Context) {
        labelPhoneAlbumPhotos(context)
    }

    @OptIn(DelicateCoroutinesApi::class)
    @RequiresApi(Build.VERSION_CODES.N)
    suspend fun labelPhoneAlbumPhotos(context: Context){
        val startTime = System.currentTimeMillis()
        notificationManager.notify(1, notificationBuilder
            .setContentTitle("Process initiated")
            .build()
        )
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA
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

        GlobalScope.launch(Dispatchers.IO){
            var index = 1
            queryCursor.use { cursor ->
                if (cursor != null && cursor.count > 0) {
                    val contentIdColumn = cursor.getColumnIndex(MediaStore.Images.ImageColumns._ID)
                    val contentDiskPathColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATA)

                    if (cursor.moveToFirst()) {
                        var contentId: Long
                        var contentDiskPath: String

                        var exception: Exception? = null

                        try {
                            do {
                                contentId = cursor.getLong(contentIdColumn)
                                contentDiskPath = cursor.getString(contentDiskPathColumn)

                                println("$index) $contentDiskPath")
                                val imageIdString = contentId.toString()

                                if(imageDao.doesImageExist(imageIdString)) {
                                    println("Skipping")
                                }
                                else {
                                    labelImage(context, imageIdString, contentDiskPath)
                                }

                                index++
                            } while (cursor.moveToNext())
                        }
                        catch (e: Exception) {
                            exception = e
                            println("WHY DID YOU CRASH!!!???: ${e.message} ")
                        }

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
            val endTime = System.currentTimeMillis()
            notificationManager.notify(1, notificationBuilder
                .setContentTitle("Total images: $index; total time: ${endTime - startTime}")
                .build()
            )
        }
    }

    fun labelImage(
        context: Context,
        contentId: String,
        contentDiskPath: String
    ) {
        println("new image being processed")
        val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, ContentUris.withAppendedId(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentId.toLong()))
        val inputImage = InputImage.fromBitmap(bitmap, 0)

        imageDao.insertImage(
            ImageEntity(
                imageId = contentId,
                imagePath = contentDiskPath
            )
        )
        imageLabeler
            .process(inputImage)
            .addOnSuccessListener { imageLabels ->
                for(imageLabel in imageLabels) {
                    val text = imageLabel.text
                    val confidence = imageLabel.confidence
                    val index = imageLabel.index

                    imageDao.insertLabel(
                        ImageLabelEntity(
                            imageId = contentId,
                            label = "Text: $text\tConfidence: $confidence\tIndex: $index"
                        )
                    )
                    println("Text: $text\tConfidence: $confidence\tIndex: $index")
                }

            }
    }
}