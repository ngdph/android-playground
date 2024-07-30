package com.example.appwithshortcut


import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.Camera
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Icon
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.appwithshortcut.ui.theme.AppWithShortcutTheme
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

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



    private val viewModel: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)





        if(!checkStoragePermissions()){
            requestForStoragePermissions()
        }
        handleIntent(intent)
        enableEdgeToEdge()
        setContent {

            AppWithShortcutTheme {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(
                        16.dp, Alignment.CenterVertically
                    )
                ) {
                    when(viewModel.shortcutType) {
                        ShortcutType.STATIC -> Text("Static shortcut clicked")
                        ShortcutType.DYNAMIC -> Text("Dynamic shortcut clicked")
                        ShortcutType.PINNED -> Text("Pinned shortcut clicked")
                        null -> Text("No shortcut clicked")
                    }
                    Screenshot()

                    Spacer(modifier=Modifier.height(16.dp))

                    Button(
                        onClick = ::DynamicShortcut
                    ) {

                        Text("Add dynamic shortcut")
                    }
                    Button(
                        onClick = ::addPinnedShortcut
                    ) {
                        Text("Pin App to the main screen")
                    }
                }
            }
        }
    }



    inner class MediaProjectionCallBack{

    }




    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }




    private fun DynamicShortcut(){
        val shortcut1= ShortcutInfoCompat.Builder(this, "1")
            .setShortLabel("Function1")
            .setLongLabel("Do function1")
            .setIcon(IconCompat.createWithResource(this, R.drawable.baseline_cell_tower_24))
            .setIntent(
                Intent(this, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra("shortcut_id", "dynamic")

                }
            )
            .build()
        val shortcut2= ShortcutInfoCompat.Builder(this, "2")
            .setShortLabel("Function2")
            .setLongLabel("Do function2")
            .setIcon(IconCompat.createWithResource(this, R.drawable.baseline_carpenter_24))
            .setIntent(
                Intent(this, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra("shortcut_id", "dynamic")
                }
            )
            .build()
        val shortcut3= ShortcutInfoCompat.Builder(this, "3")
            .setShortLabel("Screenshot")
            .setLongLabel("Take a Screenshot")
            .setIcon(IconCompat.createWithResource(this, R.drawable.baseline_camera_alt_24))
            .setIntent(
                Intent(this, MainActivity::class.java).apply {
                    action = "com.example.myapp.ACTION_TAKE_SCREENSHOT"
                    putExtra("shortcut_id", "dynamic")
                }
            )
            .build()



        ShortcutManagerCompat.pushDynamicShortcut(this,shortcut1)
        ShortcutManagerCompat.pushDynamicShortcut(this,shortcut2)
        ShortcutManagerCompat.pushDynamicShortcut(this,shortcut3)
    }

    private fun addPinnedShortcut() {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val shortcutManager = getSystemService<ShortcutManager>()!!
        if(shortcutManager.isRequestPinShortcutSupported) {
            val shortcut = ShortcutInfo.Builder(applicationContext, "pinned")
                .setShortLabel("AppWithShortcut")
                .setLongLabel("This is AppWithShortcut")
                .setIcon(
                    Icon.createWithResource(
                    applicationContext, R.drawable.facebook
                ))
                .setIntent(
                    Intent(applicationContext, MainActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        putExtra("shortcut_id", "pinned")
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




    private fun handleIntent(intent: Intent?) {
        intent?.let {
            when(intent.getStringExtra("shortcut_id")) {
                "static" -> viewModel.onShortcutClicked(ShortcutType.STATIC)
                "dynamic" -> viewModel.onShortcutClicked(ShortcutType.DYNAMIC)
                "pinned" -> viewModel.onShortcutClicked(ShortcutType.PINNED)
            }
        }
    }


    @Composable
    fun Screenshot() {
            val context = LocalContext.current
            val density = LocalDensity.current.density

            val capturedBitmap = remember { mutableStateOf<Bitmap?>(null) }

            Button(
                onClick = {
                    capturedBitmap.value = captureScreen(context)
                    Toast.makeText(context, "Screenshot captured", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.padding(16.dp)
            ) {
                androidx.compose.material3.Text("Capture Screenshot")

            }

            Spacer(modifier = Modifier.height(16.dp))

            capturedBitmap.value?.let {
                androidx.compose.foundation.Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Captured Screenshot",
                    modifier = Modifier
                        .size(400.dp)

                )
            }

            saveBitmapToStorage(context, capturedBitmap.value)

    }


    private fun captureScreen(context: android.content.Context): Bitmap {
        val view = (context as android.app.Activity).window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    private fun saveBitmapToStorage(context: android.content.Context, bitmap: Bitmap?) {
        if (bitmap == null) {
            Toast.makeText(context, "Bitmap is null", Toast.LENGTH_SHORT).show()
            return
        }

        val fileName = "screenshot.jpg"

        val picturesDirectory =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val imageFile = File(picturesDirectory, fileName)

        var outputStream: OutputStream? = null
        try {
            outputStream = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()

            ContextCompat.getMainExecutor(context).execute {
                Toast.makeText(context, "Image saved to $imageFile", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
        } finally {
            try {
                outputStream?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


    }

}




