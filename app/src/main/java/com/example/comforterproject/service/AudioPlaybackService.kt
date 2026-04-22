package com.example.comforterproject.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bumptech.glide.Glide
import com.example.comforterproject.R
import com.example.comforterproject.view.activity.HomeActivity
class AudioPlaybackService : Service() {
    companion object {
        const val ACTION_PLAY = "ACTION_PLAY"
        const val ACTION_TOGGLE = "ACTION_TOGGLE"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_AUDIO_URL = "extra_audio_url"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_IMAGE_URL = "extra_image_url"
        private const val CHANNEL_ID = "music_channel"
        private const val NOTIFICATION_ID = 101
        private const val TAG = "AudioPlaybackService"
    }

    inner class LocalBinder : Binder() {
        fun getService(): AudioPlaybackService = this@AudioPlaybackService
    }

    private val binder = LocalBinder()
    private var mediaPlayer: MediaPlayer? = null
    private var isPrepared = false
    private var currentTitle: String = "Playing Music"
    private var currentArtworkBitmap: Bitmap? = null
    private var onCompletionListener: (() -> Unit)? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val url = intent.getStringExtra(EXTRA_AUDIO_URL)
                val title = intent.getStringExtra(EXTRA_TITLE)
                val image = intent.getStringExtra(EXTRA_IMAGE_URL)
                if (!url.isNullOrEmpty()) {
                    playMusic(url, title ?: "Music", image)
                }
            }

            ACTION_TOGGLE -> togglePlayback()
            ACTION_STOP -> stopPlaybackService()
            else -> startForegroundServiceProperly()
        }

        return START_STICKY
    }

    private fun playMusic(url: String, title: String, imageUrl: String?) {
        currentTitle = title
        currentArtworkBitmap = null

        startForegroundServiceProperly()
        loadArtwork(imageUrl)
        releasePlayer()

        runCatching {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                applyDataSource(url)
                setOnPreparedListener {
                    isPrepared = true
                    start()
                    updateNotification()
                }
                setOnCompletionListener {
                    onCompletionListener?.invoke()
                    updateNotification()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error. what=$what extra=$extra url=$url")
                    stopPlaybackService()
                    true
                }
                prepareAsync()
            }
        }.onFailure { error ->
            Log.e(TAG, "Unable to play source: $url", error)
            Toast.makeText(this, "Unable to play this song", Toast.LENGTH_SHORT).show()
            stopPlaybackService()
        }
    }

    private fun MediaPlayer.applyDataSource(url: String) {
        val normalizedUrl = normalizeNetworkPath(url)
        val uri = Uri.parse(normalizedUrl)
        when (uri.scheme?.lowercase()) {
            "content", "android.resource" -> setDataSource(this@AudioPlaybackService, uri)
            "file" -> setDataSource(uri.path ?: normalizedUrl)
            else -> setDataSource(normalizedUrl)
        }
    }

    private fun normalizeNetworkPath(value: String): String {
        return if (value.startsWith("//")) "https:$value" else value
    }

    private fun togglePlayback() {
        val player = mediaPlayer ?: return
        if (!isPrepared) return

        if (player.isPlaying) {
            player.pause()
        } else {
            player.start()
        }
        updateNotification()
    }

    private fun stopPlaybackService() {
        releasePlayer()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startForegroundServiceProperly() {
        val notification = buildNotification(mediaPlayer?.isPlaying == true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(isPlaying: Boolean): Notification {
        val openIntent = Intent(this, HomeActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = Intent(this, AudioPlaybackService::class.java).apply {
            action = ACTION_TOGGLE
        }
        val togglePendingIntent = PendingIntent.getService(
            this,
            1,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AudioPlaybackService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_notification)
            .setContentTitle(currentTitle)
            .setContentText(if (isPlaying) "Playing music" else "Music paused")
            .setContentIntent(openPendingIntent)
            .setOngoing(mediaPlayer != null)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setShowWhen(false)
            .setSilent(true)
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Pause" else "Play",
                togglePendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                stopPendingIntent
            )

        currentArtworkBitmap?.let { builder.setLargeIcon(it) }

        return builder.build()
    }
    private fun updateNotification() {
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) return
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(mediaPlayer?.isPlaying == true))
    }
    private fun loadArtwork(url: String?) {
        if (url.isNullOrEmpty()) return
        Thread {
            try {
                val bitmap = Glide.with(applicationContext)
                    .asBitmap()
                    .load(url)
                    .submit()
                    .get()
                currentArtworkBitmap = bitmap
                updateNotification()
            } catch (_: Exception) {
            }
        }.start()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Player",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun releasePlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
        isPrepared = false
    }

    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
    }
    fun setOnCompletionListener(listener: (() -> Unit)?) {
        onCompletionListener = listener
    }

    fun isPrepared(): Boolean = isPrepared
    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true
    fun getCurrentPosition(): Int = mediaPlayer?.currentPosition ?: 0
    fun getDuration(): Int = if (isPrepared) mediaPlayer?.duration ?: 0 else 0

    override fun onDestroy() {
        releasePlayer()
        super.onDestroy()
    }
}
