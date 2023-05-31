package com.wangjingbo.low.Server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.wangjingbo.low.R
class MusicPlayerService : Service() {
    private lateinit var mediaPlayer: MediaPlayer
    private var isPlaying: Boolean = false
    private val pauseResumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == "PAUSE_RESUME") {
                if (isPlaying) {
                    pauseMusic()
                } else {
                    resumeMusic()
                }
            }
        }
    }

    // 当服务创建时调用
    override fun onCreate() {
        val seekToFilter = IntentFilter("SEEK_TO")
        registerReceiver(seekToReceiver, seekToFilter)
        super.onCreate()
        mediaPlayer = MediaPlayer()
        val filter = IntentFilter("PAUSE_RESUME")
        registerReceiver(pauseResumeReceiver, filter)
        // 如果设备的 Android 版本大于等于 8.0，则创建通知渠道
        val channel = NotificationChannel(
            "channel_id",
            "Channel Name",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    // 当服务启动时调用
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val url = intent.getStringExtra("url")
            val name = intent.getStringExtra("name")
            val artist = intent.getStringExtra("artist")
            val album = intent.getStringExtra("album")


            // 如果所有的音乐信息都不为空，则开始播放音乐
            if (album != null && url != null && name != null && artist != null) {
                playMusic(url, name, artist, album)
            }
        }

        return START_STICKY
    }

    // 当服务绑定时调用
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    // 当服务销毁时调用
    override fun onDestroy() {
        unregisterReceiver(seekToReceiver)
        super.onDestroy()
        unregisterReceiver(pauseResumeReceiver)
        mediaPlayer.release()
    }

    // 播放音乐的方法
    private fun playMusic(url: String, name: String, artist: String, album: String) {
        mediaPlayer.reset()
        mediaPlayer.setDataSource(url)
        mediaPlayer.prepare()
        mediaPlayer.start()
        isPlaying = true

        // 显示通知
        showNotification(name, artist)

        // 更新通知的进度条
        val handler = Handler()
        handler.postDelayed(object : Runnable {
            override fun run() {
                val maxProgress = mediaPlayer.duration
                val currentProgress = mediaPlayer.currentPosition

                // 更新通知的进度条
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(1, createNotification(name, artist, maxProgress, currentProgress))

                // 每隔一秒更新一次
                handler.postDelayed(this, 1000)
            }
        }, 1000)
    }
    // 暂停音乐的方法
    private fun pauseMusic() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            isPlaying = false

            // 更新通知中的按钮图标
            val pauseResumeIcon = R.drawable.ic_play
        }
    }

    // 恢复音乐的方法
    private fun resumeMusic() {
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.start()
            isPlaying = true

            // 更新通知中的按钮图标
            val pauseResumeIcon = R.drawable.ic_pause
        }
    }

    private fun createNotification(name: String, artist: String, maxProgress: Int, currentProgress: Int): Notification {
        // 创建暂停/恢复操作的 PendingIntent
        val pauseResumeIntent = Intent(this, MusicPlayerService::class.java)
        pauseResumeIntent.action = "PAUSE_RESUME"
        val pauseResumePendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            pauseResumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val seekToIntent = Intent(this, MusicPlayerService::class.java)
        seekToIntent.action = "SEEK_TO"
        // 创建通知构建器
        val notificationBuilder = NotificationCompat.Builder(this, "channel_id")
            .setSmallIcon(R.drawable.music_play_list)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        val seekToPendingIntent = PendingIntent.getService(
            this,
            0,
            seekToIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        // 将通知设置为进行中
        notificationBuilder.setOngoing(true)

        // 设置通知颜色
        val color = ContextCompat.getColor(this, R.color.notificationColor)
        notificationBuilder.color = color

        // 创建大图标的位图（可选）
        val largeIconBitmap = BitmapFactory.decodeResource(resources, R.drawable.notification)
        notificationBuilder.setLargeIcon(largeIconBitmap)

        // 设置通知操作（例如暂停/恢复和进度条拖动）
        val pauseResumeIcon = if (mediaPlayer.isPlaying)
            R.drawable.ic_pause else R.drawable.ic_play
        notificationBuilder.addAction(
            NotificationCompat.Action(
                pauseResumeIcon,
                "Pause/Resume",
                pauseResumePendingIntent
            )
        )
        notificationBuilder.addAction(
            NotificationCompat.Action(
                R.drawable.ic_pause,
                "Seek",
                seekToPendingIntent
            )
        )

        // 设置通知的进度（例如音乐播放进度）
        notificationBuilder.setProgress(maxProgress, currentProgress, false)

        // 构建通知
        return notificationBuilder.build()
    }

    //处理进度条拖动的事件
    private val seekToReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == "SEEK_TO") {
                val seekTo = intent.getIntExtra("seekTo", 0)
                seekMusic(seekTo)
            }
        }
    }

    //将音乐播放器的进度设置为指定的位置
    private fun seekMusic(position: Int) {
        mediaPlayer.seekTo(position)
    }

    // 显示通知的方法
    private fun showNotification(name: String, artist: String) {

        // 创建暂停/恢复操作的 PendingIntent
        val pauseResumeIntent = Intent(this, MusicPlayerService::class.java)
        pauseResumeIntent.action = "PAUSE_RESUME"
        val pauseResumePendingIntent = PendingIntent.getService(
            this,
            0,
            pauseResumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 创建通知渠道（适用于 Android 8.0 及以上版本）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "channel_id",
                "Channel Name",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        // 创建通知构建器
        val notificationBuilder = NotificationCompat.Builder(this, "channel_id")
            .setSmallIcon(R.drawable.music_play_list)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        // 将通知设置为进行中
        notificationBuilder.setOngoing(true)

        // 设置通知颜色
        val color = ContextCompat.getColor(this, R.color.notificationColor)
        notificationBuilder.color = color

        // 创建大图标的位图（可选）
        val largeIconBitmap = BitmapFactory.decodeResource(resources, R.drawable.notification)
        notificationBuilder.setLargeIcon(largeIconBitmap)

        // 设置通知操作（例如暂停/恢复）
        val pauseResumeIcon = if (mediaPlayer.isPlaying)
            R.drawable.ic_pause else R.drawable.ic_play
        notificationBuilder.addAction(
            NotificationCompat.Action(
                pauseResumeIcon,
                "Pause/Resume",
                pauseResumePendingIntent
            )
        )

        // 设置通知的进度（例如音乐播放进度）
        val maxProgress = mediaPlayer.duration
        val currentProgress = mediaPlayer.currentPosition
        notificationBuilder.setProgress(maxProgress, currentProgress, false)

        // 构建通知
        val notification = notificationBuilder.build()

        // 显示通知
        startForeground(1, notification)
    }
}




