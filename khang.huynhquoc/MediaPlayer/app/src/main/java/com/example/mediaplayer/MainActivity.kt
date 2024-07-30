package com.example.mediaplayer

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

import androidx.media3.session.SessionToken
import androidx.media3.ui.PlayerView
import com.example.mediaplayer.ui.theme.MediaPlayerTheme
import com.google.common.util.concurrent.MoreExecutors
import java.io.File

class MainActivity : ComponentActivity() {
    val storagePermissionCode: Int = 23

    val storageActivityResultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {}

    fun requestForStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent()
                intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                val uri = Uri.fromParts("package", this.packageName, null)
                intent.setData(uri)
                storageActivityResultLauncher.launch(intent)
            } catch (e: java.lang.Exception) {
                val intent = Intent()
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                storageActivityResultLauncher.launch(intent)
            }
        } else {
            //Below android 11
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ), storagePermissionCode
            )
        }
    }

    fun checkStoragePermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            //Android is 11 (R) or above
            return Environment.isExternalStorageManager()
        } else {
            //Below android 11
            val write = ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            val read = ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.READ_EXTERNAL_STORAGE
            )

            return read == PackageManager.PERMISSION_GRANTED && write == PackageManager.PERMISSION_GRANTED
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(!checkStoragePermissions()){
            requestForStoragePermissions()
        }
        setContent {
            MediaPlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    //VideoPlayer("/storage/emulated/0/Movies/okok (1).mp4") // Thay thế bằng URL video của bạn

                    VideoPlayer("/storage/emulated/0/Movies")
                }
            }
        }
    }
}
//@Composable
//fun VideoPlayer(videoUrl: String) {
//    val context = LocalContext.current
//    val exoPlayer = remember {
//        ExoPlayer.Builder(context).build().apply {
//            setMediaItem(MediaItem.fromUri(videoUrl))
//            prepare()
//            playWhenReady = true // Tự động phát khi sẵn sàng
//        }
//    }
//
//    // Giải phóng ExoPlayer khi Composable bị hủy
//    DisposableEffect(
//        AndroidView(factory = {
//            PlayerView(context).apply {
//                player = exoPlayer
//            }
//        })
//    ) {
//        onDispose { exoPlayer.release() }
//    }
//}
@Composable
fun VideoPlayer(folderPath: String) {
    val context = LocalContext.current
    val videoUris = remember { mutableStateListOf<Uri>() }

    // Quét thư mục vàtìm các file .mp4
    LaunchedEffect(folderPath) {
        val folder = File(folderPath)
        if (folder.exists() && folder.isDirectory) {
            folder.listFiles { file -> file.isFile && file.name.endsWith(".mp4") }
                ?.map { Uri.fromFile(it) }
                ?.let { videoUris.addAll(it) }
        }
    }

    LazyColumn {
        items(videoUris) { uri ->
            Text(text = uri.lastPathSegment ?: "Unknown", modifier = Modifier.padding(8.dp))
        }}

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }

    // Tạo playlist và phát
    LaunchedEffect(videoUris) {
        val mediaItems = videoUris.map { MediaItem.fromUri(it) }
        exoPlayer.setMediaItems(mediaItems)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    // Giải phóng ExoPlayer khi Composable bị hủy
    DisposableEffect(
        AndroidView(factory = {
            PlayerView(context).apply {
                player = exoPlayer
            }
        })
    ) {
        onDispose { exoPlayer.release() }
    }
}






