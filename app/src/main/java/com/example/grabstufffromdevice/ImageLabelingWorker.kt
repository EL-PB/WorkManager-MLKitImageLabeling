package com.example.grabstufffromdevice

import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.viewModelScope
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
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

@Suppress("BlockingMethodInNonBlockingContext")
class ImageLabelingWorker(
    private val context: Context,
    private val workerParams: WorkerParameters
): CoroutineWorker(context, workerParams) {
    val imagesSharedpreferences: SharedPreferences = context.getSharedPreferences("images_preference", Context.MODE_PRIVATE)
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
    suspend fun processDeviceImages(
        context: Context
    ) {
        labelPhoneAlbumPhotos(context)
    }

    var labeledImagesList: MutableList<ImageDataClass> = mutableListOf()

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

        val job: Job = GlobalScope.launch(Dispatchers.IO){
            queryCursor.use { cursor ->
                if (cursor != null && cursor.count > 0) {
                    val contentIdColumn = cursor.getColumnIndex(MediaStore.Images.ImageColumns._ID)
                    val contentDiskPathColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                    val contentDateModifiedColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)
                    val contentDateAddedColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
                    val contentDateTakenColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)

                    if (cursor.moveToFirst()) {
                        var contentId: Long
                        var contentDiskPath: String
                        var contentDateModified: String
                        var contentDateAdded: String
                        var contentDateTaken: String

                        var exception: Exception? = null
                        var index = 1

                        try {
                            do {
                                contentId = cursor.getLong(contentIdColumn)
                                contentDiskPath = cursor.getString(contentDiskPathColumn)
                                contentDateModified = cursor.getString(contentDateModifiedColumn)
                                contentDateAdded = cursor.getString(contentDateAddedColumn)
                                contentDateTaken = cursor.getString(contentDateTakenColumn)

                                println("$index) $contentDiskPath")
                                labelImage(context, contentId, contentDiskPath, contentDateModified, contentDateAdded, contentDateTaken)
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
        }
        job.join()
        val endingPoint = System.currentTimeMillis()

        //region Log and save image data row
        val imageEditor = imagesSharedpreferences.edit()
        println("Image Disk Path\n----------------------")
        println("startingPoint: $startingPoint")
        println("endingPoint: $endingPoint")
        println("Total Time : " + (endingPoint - startingPoint) + "milliseconds \n")

        val gson = Gson()
        labeledImagesList.forEach {
            val jsonImageDataClass = gson.toJson(it)
            imageEditor.putString(it.imageID, jsonImageDataClass)
        }
        imageEditor.apply()
        //endregion
    }

    suspend fun labelImage(
        context: Context,
        contentId: Long,
        contentDiskPath: String,
        contentDateModified: String,
        contentDateAdded: String,
        contentDateTaken: String
    ) {
        val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, ContentUris.withAppendedId(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentId))
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        var labels: MutableList<String> = arrayListOf()

        val data = imageLabeler.process(inputImage).await()

        data.let { imageLabels ->
            for(imageLabel in imageLabels) {
                val text = imageLabel.text
                labels.add(text)
            }

            labeledImagesList.add(
                ImageDataClass(
                    imageID = contentId.toString(),
                    filePath = contentDiskPath,
                    lastEdit = contentDateModified,
                    contentDateAdded = contentDateAdded,
                    contentDateTaken = contentDateTaken,
                    labelList = labels
                )
            )

            println("new item in labeledImagesList")
        }
    }
}