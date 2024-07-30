package com.example.screenrecord

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.screenrecord.ui.theme.ScreenRecordTheme
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.util.DisplayMetrics
import android.view.Surface
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast

import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val REQUEST_CODE = 1000
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private lateinit var imageReader: ImageReader
    private lateinit var handler: Handler
    private lateinit var virtualDisplay: VirtualDisplay
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private lateinit var imageView: ImageView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private var screenDensity = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen_record)

        imageView = findViewById(R.id.imageView)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)

        startButton.setOnClickListener {
            if (!isRecording) {
                startScreenRecording()
            } else {
                stopScreenRecording()
            }
        }

        stopButton.setOnClickListener {
            stopScreenRecording()
        }

        // Initialize MediaProjectionManager
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        // Get screen density
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        screenDensity = metrics.densityDpi

        // Create ImageReader
        imageReader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, android.graphics.ImageFormat.RGB_565, 2)
    }

    private fun startScreenRecording() {
        isRecording = true

        // Request permission to capture screen
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE)
    }

    private fun stopScreenRecording() {
        isRecording = false

        // Stop recording
        mediaRecorder?.apply {
            stop()
            reset()
            release()
        }
        mediaRecorder = null

        // Stop VirtualDisplay
        virtualDisplay.release()

        // Release MediaProjection
        mediaProjection?.stop()
        mediaProjection = null
    }

    @SuppressLint("MissingSuperCall")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data!!)

                // Create VirtualDisplay
                createVirtualDisplay()

                // Start capturing
                captureScreen()
            } else {
                Toast.makeText(this, "Permission denied.", Toast.LENGTH_SHORT).show()
                isRecording = false
            }
        }
    }

    private fun createVirtualDisplay() {
        virtualDisplay = mediaProjection!!.createVirtualDisplay(
            "ScreenRecord",
            imageReader.width,
            imageReader.height,
            screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface,
            null,
            handler
        )
    }

    private fun captureScreen() {
        handler = Handler()

        imageReader.setOnImageAvailableListener({ reader ->
            val image: Image? = reader?.acquireLatestImage()
            if (image != null) {
                val bitmap: Bitmap = image.toBitmap()
                imageView.setImageBitmap(bitmap)

                // Save bitmap to file (optional)
                saveBitmap(bitmap)

                image.close()
            }
        }, handler)
    }

    private fun Image.toBitmap(): Bitmap {
        val buffer: ByteBuffer = planes[0].buffer
        val pixelStride: Int = planes[0].pixelStride
        val rowStride: Int = planes[0].rowStride
        val rowPadding: Int = rowStride - pixelStride * width

        val bitmap = Bitmap.createBitmap(
            width + rowPadding / pixelStride,
            height,
            Bitmap.Config.RGB_565
        )
        bitmap.copyPixelsFromBuffer(buffer)

        return bitmap
    }

    private fun saveBitmap(bitmap: Bitmap) {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val date = Date()
        val filename = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)?.absolutePath +
                "/Recording_" + dateFormat.format(date) + ".png"
        val file = File(filename)
        try {
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
            out.close()
            Toast.makeText(this, "Bitmap saved: $filename", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}