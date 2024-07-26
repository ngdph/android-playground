package com.example.deeplinks

import android.Manifest
import android.app.Activity.RESULT_OK
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.drawable.Icon
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.MediaScannerConnection
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.provider.MediaStore
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.PixelCopy
import android.view.Surface
import android.view.Window
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.getSystemService
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.deeplinks.ui.theme.DeeplinksTheme
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val storagePermissionCode: Int = 23
    private val storageActivityResultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {}

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private lateinit var mediaRecorder: MediaRecorder
    private var virtualDisplay: VirtualDisplay? = null

    private val SCREEN_RECORD_REQUEST_CODE = 1000
    private val SCREEN_RECORD_PERMISSION_CODE = 2000

    private val player by lazy { ExoPlayer.Builder(this).build() }
    private val mediaSession by lazy { MediaSession.Builder(this, player).build() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        if (!checkStoragePermissions()) requestForStoragePermissions()
        handleIntent(intent)
        setContent {
            DeeplinksTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        var appName by remember { mutableStateOf("") }
                        var mediaUrl by remember { mutableStateOf("") }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            when (viewModel.shortcutType) {
                                ShortcutType.STATIC -> {
                                    Text("Static feature")
                                }

                                ShortcutType.DYNAMIC -> {
                                    Text("Opening AnhKhoa's Github")
                                    openApp("github.com/AnhKhoa2174")
                                }

                                ShortcutType.PINNED -> {
                                    Text("Opening pinned shortcut")
                                }

                                null -> Text("This app is developed by Khoa")
                            }
                            Button(
                                onClick = ::addDynamicShortcut
                            ) {
                                Text("Click this to open Github")
                            }
                            Button(
                                onClick = ::addPinnedShortcut
                            ) {
                                Text("Click this to open Youtube")
                            }
                            Button(
                                onClick = ::addPinnedKhoaShortcut
                            ) {
                                Text("Click this to open Khoa's Github")
                            }
                            Button(
                                onClick = ::takeScreenshot
                            ) {
                                Text("Take Screenshot")
                            }
                            OutlinedTextField(
                                value = appName,
                                onValueChange = { appName = it },
                                label = { Text("App Name") }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = {
                                openApp(appName)
                            }) {
                                Text(text = "Open App")
                            }
                            Button(onClick = ::startScreenRecording) {
                                Text(text = "Start Recording")
                            }
                            Button(onClick = ::stopScreenRecording) {
                                Text(text = "Stop Recording")
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = mediaUrl,
                                onValueChange = { mediaUrl = it },
                                label = { Text("Media URL") }
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Button(onClick = { playMedia(mediaUrl) }) {
                                    Text(text = "Play")
                                }
                                Button(onClick = { player.pause() }) {
                                    Text(text = "Pause")
                                }
                                Button(onClick = { player.stop() }) {
                                    Text(text = "Stop")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun requestForStoragePermissions() {
        // Android 11 (R) or above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                val uri = Uri.fromParts("package", this.packageName, null)
                intent.data = uri
                storageActivityResultLauncher.launch(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                storageActivityResultLauncher.launch(intent)
            }
        } else {
            // Below android 11
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ), storagePermissionCode
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun addPinnedShortcut() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val shortcutManager = getSystemService<ShortcutManager>()!!
        if (shortcutManager.isRequestPinShortcutSupported) {
            val shortcut = ShortcutInfo.Builder(applicationContext, "pinned_youtube")
                .setShortLabel("Open Youtube")
                .setLongLabel("This will open Youtube")
                .setIcon(
                    Icon.createWithResource(
                        applicationContext, R.drawable.youtube
                    )
                )
                .setIntent(
                    Intent(applicationContext, MainActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        putExtra("shortcut_id", "pinned_youtube")
                    }
                )
                .build()

            val callbackIntent = shortcutManager.createShortcutResultIntent(shortcut)
            val successPendingIntent = PendingIntent.getBroadcast(
                applicationContext,
                0,
                callbackIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
            shortcutManager.requestPinShortcut(shortcut, successPendingIntent.intentSender)
        }
    }

    private fun checkStoragePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 (R) or above
            Environment.isExternalStorageManager()
        } else {
            // Below android 11
            val write = ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            val read = ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
            write == PackageManager.PERMISSION_GRANTED && read == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun addPinnedKhoaShortcut() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val shortcutManager = getSystemService<ShortcutManager>()!!
        if (shortcutManager.isRequestPinShortcutSupported) {
            val shortcut = ShortcutInfo.Builder(applicationContext, "pinned_github")
                .setShortLabel("Open Khoa's Github")
                .setLongLabel("This will open Anh Khoa's Github")
                .setIcon(
                    Icon.createWithResource(
                        applicationContext, R.drawable.github
                    )
                )
                .setIntent(
                    Intent(applicationContext, MainActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        putExtra("shortcut_id", "pinned_github")
                    }
                )
                .build()

            val callbackIntent = shortcutManager.createShortcutResultIntent(shortcut)
            val successPendingIntent = PendingIntent.getBroadcast(
                applicationContext,
                0,
                callbackIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
            shortcutManager.requestPinShortcut(shortcut, successPendingIntent.intentSender)
        }
    }

    private fun openApp(appName: String) {
        val intent = packageManager.getLaunchIntentForPackage(appName)
        if (intent != null) {
            startActivity(intent)
        } else {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://$appName")
                }
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "App not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addDynamicShortcut() {
        val shortcut = ShortcutInfoCompat.Builder(applicationContext, "dynamic")
            .setShortLabel("Open AnhKhoa's Github")
            .setLongLabel("This will open Khoa's Github")
            .setIcon(IconCompat.createWithResource(applicationContext, R.drawable.github))
            .setIntent(
                Intent(applicationContext, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra("shortcut_id", "dynamic")
                }
            )
            .build()
        ShortcutManagerCompat.pushDynamicShortcut(applicationContext, shortcut)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let {
            when (it.getStringExtra("shortcut_id")) {
                "dynamic" -> {
                    // Handle dynamic shortcut
                }
                "pinned_youtube" -> {
                    // Handle pinned shortcut for YouTube
                }
                "pinned_github" -> {
                    // Handle pinned shortcut for GitHub
                }
            }
        }
    }

    private fun startScreenRecording() {
        if (mediaProjection == null) {
            val screenCaptureIntent = mediaProjectionManager.createScreenCaptureIntent()
            startActivityForResult(screenCaptureIntent, SCREEN_RECORD_PERMISSION_CODE)
        } else {
            setupMediaRecorder()
            startRecording()
        }
    }

    private fun setupMediaRecorder() {
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(getOutputFile().absolutePath)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setVideoSize(1280, 720)
            setVideoFrameRate(30)
            setVideoEncodingBitRate(512 * 1000)
            try {
                prepare()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun startRecording() {
        mediaProjection?.apply {
            virtualDisplay = createVirtualDisplay(
                "MainActivity",
                1280,
                720,
                resources.displayMetrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder.surface,
                null,
                null
            )
        }
        mediaRecorder.start()
        Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
    }

    private fun stopScreenRecording() {
        mediaRecorder.stop()
        mediaRecorder.reset()
        virtualDisplay?.release()
        mediaProjection?.stop()
        mediaProjection = null
        Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show()
    }

    private fun getOutputFile(): File {
        val videoDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            "ScreenRecords"
        )
        if (!videoDir.exists()) {
            videoDir.mkdirs()
        }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(videoDir, "SCREEN_RECORD_$timestamp.mp4")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SCREEN_RECORD_PERMISSION_CODE) {
            if (resultCode == RESULT_OK) {
                data?.let {
                     mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, it)

                    val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            putExtra("mediaProjection", mediaProjection as Parcelable?)
                        } else {
                            @Suppress("DEPRECATION")
                            putExtra("mediaProjection", mediaProjection as Parcelable)
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent)
                    } else {
                        startService(serviceIntent)
                    }

                    setupMediaRecorder()
                    startRecording()
                }
            } else {
                Toast.makeText(this, "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun takeScreenshot() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Request the permission if not granted
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), storagePermissionCode)
            return
        }

        val rootView = window.decorView.rootView
        rootView.isDrawingCacheEnabled = true
        val bitmap = Bitmap.createBitmap(rootView.drawingCache)
        rootView.isDrawingCacheEnabled = false

        val now = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val imagePath = File(downloadsDir, "Screenshot_$now.png")

        try {
            val outputStream = FileOutputStream(imagePath)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()

            // Notify the media scanner of the new file
            MediaScannerConnection.scanFile(this, arrayOf(imagePath.toString()), null, null)

            val imageUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", imagePath)

            // Intent to open the saved screenshot
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(imageUri, "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(viewIntent)

            Toast.makeText(this, "Screenshot saved in: ${imagePath.absolutePath}", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save screenshot", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playMedia(mediaUrl: String) {
        if (mediaUrl.isNotBlank()) {
            val mediaItem = MediaItem.fromUri(mediaUrl)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        } else {
            Toast.makeText(this, "Please provide a valid media URL", Toast.LENGTH_SHORT).show()
        }
    }


}
