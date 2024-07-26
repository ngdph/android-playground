import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.ui.PlayerNotificationManager
import com.example.deeplinks.MainActivity
import com.example.deeplinks.R

@UnstableApi
class YourMediaSessionService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private lateinit var playerNotificationManager: PlayerNotificationManager

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player).build()

        createNotificationChannel()
        setupPlayerNotificationManager()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "playback_channel",
                "Playback Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun setupPlayerNotificationManager() {
        playerNotificationManager = PlayerNotificationManager.Builder(
            this,
            1,
            "playback_channel"
        ).build()

        playerNotificationManager.setPlayer(player)
        //mediaSession?.sessionCompatToken?.let { playerNotificationManager.setMediaSessionToken(it) }
        playerNotificationManager.setSmallIcon(R.drawable.baseline_notifications_24)
        playerNotificationManager.setUseNextAction(true)
        playerNotificationManager.setUsePreviousAction(true)

        startForeground(1, buildNotification())
    }

    private fun buildNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, "playback_channel")
            .setContentTitle("Playing Media")
            .setContentText("Media is playing")
            .setSmallIcon(R.drawable.baseline_notifications_24)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        playerNotificationManager.setPlayer(null)
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        super.onBind(intent)
        return null
    }
}
