package com.example.grabstufffromdevice

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.work.*
import com.example.grabstufffromdevice.ImageLabelingWorker

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val vm = ViewModelProvider(this).get(ViewModel::class.java)

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, Array(1){Manifest.permission.READ_EXTERNAL_STORAGE}, 121)
        }

        setContent {
            val labelingRequest = OneTimeWorkRequestBuilder<ImageLabelingWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            val workManager = WorkManager.getInstance(applicationContext)
            val workInfos = workManager.getWorkInfosForUniqueWorkLiveData("ImageLabling").observeAsState().value
            val imageLabelingInfo = remember(key1 = workInfos) { workInfos?.find { it.id == labelingRequest.id } }

            Column(Modifier.fillMaxSize()) {
                val labeledImages by vm.labeledImageRows.observeAsState()

                Column(Modifier.fillMaxSize()) {
                    Button(
                        onClick = {
                            workManager
                                .beginUniqueWork("ImageLabling", ExistingWorkPolicy.KEEP, labelingRequest)
                                .enqueue()
                        },
                        enabled = imageLabelingInfo?.state != WorkInfo.State.RUNNING
                    ) {
                        Text(text = "Begin operations")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    when(imageLabelingInfo?.state) {
                        WorkInfo.State.RUNNING -> Text("Labeling...")
                        WorkInfo.State.SUCCEEDED -> DisplayLabelsButton(vm)//Text("Labeling succeeded")
                        WorkInfo.State.FAILED -> Text("Labeling failed")
                        WorkInfo.State.CANCELLED -> Text("Labeling cancelled")
                        WorkInfo.State.ENQUEUED -> Text("Labeling enqueued")
                        WorkInfo.State.BLOCKED -> Text("Labeling blocked")
                    }
                    DisplayLabelsButton(vm)

                    Text(
                        text = "Labels List",
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    ListOfStuff(labeledImages ?: arrayListOf())
                }
            }
        }
    }
}

@Composable
fun ListOfStuff(imageList: List<ImageDataClass>) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(imageList) { stuff ->
            Text(text = stuff.imageID.toString())
            Text(text = stuff.filePath.toString())
            Text(text = stuff.lastEdit.toString())
            Text(text = stuff.contentDateAdded)
            Text(text = stuff.contentDateTaken)
            Spacer(modifier = Modifier.padding(20.dp))
        }
    }
}

@Composable
fun DisplayLabelsButton(vm: ViewModel){
    Button(onClick = { vm.getDataOfImagesAndLabels() }) {
        Text(text = "Display Labels")
    }
}