package com.example.deeplinks

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import android.os.Parcelable

class ScreenCaptureService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        mediaProjection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("mediaProjection", MediaProjection::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("mediaProjection")
        }
        startForegroundService()
        startCapture()
        return START_NOT_STICKY
    }

    private fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "ScreenCaptureServiceChannel"
            val channelName = "Screen Capture Service"
            val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(chan)

            val notification: Notification = NotificationCompat.Builder(this, channelId)
                .setContentTitle("Screen Capture")
                .setContentText("Screen capturing is running")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build()

            startForeground(1, notification)
        }
    }

    private fun startCapture() {
        val metrics = resources.displayMetrics
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            metrics.widthPixels,
            metrics.heightPixels,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            null,
            null,
            null
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        virtualDisplay?.release()
        mediaProjection?.stop()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    companion object {
        const val CHANNEL_ID = "ScreenCaptureServiceChannel"
    }
}
