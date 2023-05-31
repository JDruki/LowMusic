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
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.wangjingbo.low.Activity.MusicPlayer
import com.wangjingbo.low.R
import com.wangjingbo.low.Routh.PlayMode

class MusicPlayerService : Service() {
    private val togglePlayModeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            togglePlayMode()
            updatePlayModeButton()
        }
    }
    private var playMode: PlayMode = PlayMode.ORDER
    private lateinit var mediaPlayer: MediaPlayer
    private var currentSongIndex: Int = 0
    private var isPlaying: Boolean = false
    private lateinit var songsList: ArrayList<MusicPlayer.Song>
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

    private val playPreviousSongReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            playPreviousSong()
        }
    }
    private val playNextSongReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            playNextSong()
        }
    }

    // 当服务创建时调用
    override fun onCreate() {
        val seekToFilter = IntentFilter("SEEK_TO")
        registerReceiver(seekToReceiver, seekToFilter)
        super.onCreate()
        val togglePlayModeFilter = IntentFilter("TOGGLE_PLAY_MODE")
        registerReceiver(togglePlayModeReceiver, togglePlayModeFilter)
        val playPreviousSongFilter = IntentFilter("PLAY_PREVIOUS_SONG")
        registerReceiver(playPreviousSongReceiver, playPreviousSongFilter)
        mediaPlayer = MediaPlayer()
        val filter = IntentFilter("PAUSE_RESUME")
        registerReceiver(pauseResumeReceiver, filter)
        val playNextSongFilter = IntentFilter("PLAY_NEXT_SONG")
        registerReceiver(playNextSongReceiver, playNextSongFilter)
        // 启动服务并传递Intent
        fetchSongsFromDatabase()
        // 如果设备的 Android 版本大于等于 8.0，则创建通知渠道
        val channel = NotificationChannel(
            "channel_id",
            "Channel Name",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel) }




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
        unregisterReceiver(togglePlayModeReceiver)
        unregisterReceiver(playNextSongReceiver)
        unregisterReceiver(playPreviousSongReceiver)
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
        // 监听音乐播放完成事件
        mediaPlayer.setOnCompletionListener {
            playNextSong()
        }
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

    // 播放上一首歌曲
    private fun playPreviousSong() {
        when (playMode) {
            PlayMode.ORDER -> {
                if (currentSongIndex > 0) {
                    currentSongIndex--
                } else {
                    currentSongIndex = songsList.size - 1
                }
            }

            PlayMode.SHUFFLE -> {
                currentSongIndex = (0 until songsList.size).random()
            }

            PlayMode.REPEAT -> {
                // 单曲循环，不改变 currentSongIndex 的值
            }
        }
        val previousSong = songsList[currentSongIndex]
        playMusic(previousSong.url, previousSong.name, previousSong.artist, previousSong.album)
        val playSongIntent = Intent("PLAY_SONG")
        playSongIntent.putExtra("songName", previousSong.name)
        playSongIntent.putExtra("artist", previousSong.artist)
        sendBroadcast(playSongIntent)
    }

    // 播放下一首歌曲
    private fun playNextSong() {
        when (playMode) {
            PlayMode.ORDER -> {
                if (currentSongIndex < songsList.size - 1) {
                    currentSongIndex++
                } else {
                    currentSongIndex = 0
                }
            }

            PlayMode.SHUFFLE -> {
                currentSongIndex = (0 until songsList.size).random()
            }

            PlayMode.REPEAT -> {
                // 单曲循环，不改变 currentSongIndex 的值
            }
        }

        val nextSong = songsList[currentSongIndex]
        playMusic(nextSong.url, nextSong.name, nextSong.artist, nextSong.album)

        val playSongIntent = Intent("PLAY_SONG")
        playSongIntent.putExtra("songName", nextSong.name)
        playSongIntent.putExtra("artist", nextSong.artist)
        sendBroadcast(playSongIntent)
    }

    // 从数据库获取歌曲列表
    private fun fetchSongsFromDatabase() {
        val dbHelper = DatabaseHelper(this)
        val database = dbHelper.readableDatabase

        val projection = arrayOf("url", "name", "artist", "album")
        val cursor = database.query("new_songs", projection, null, null, null, null, null)

        songsList = ArrayList()
        if (cursor != null && cursor.moveToFirst()) {
            do {
                val album = cursor.getString(cursor.getColumnIndexOrThrow("album"))
                val url = cursor.getString(cursor.getColumnIndexOrThrow("url"))
                val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                val artist = cursor.getString(cursor.getColumnIndexOrThrow("artist"))
                val song = MusicPlayer.Song(url, name, artist, album)

                songsList.add(song)
            } while (cursor.moveToNext())
            cursor.close()
        }
    }

    // 歌曲数据类
    data class Song(val url: String, val name: String, val artist: String)
    // 切换播放模式
    private fun togglePlayMode() {
        playMode = when (playMode) {
            PlayMode.ORDER -> PlayMode.SHUFFLE
            PlayMode.SHUFFLE -> PlayMode.REPEAT
            PlayMode.REPEAT -> PlayMode.ORDER
        }
    }

    private fun updatePlayModeButton() {
        when (playMode) {
            PlayMode.ORDER -> {
                Toast.makeText(this, "顺序播放", Toast.LENGTH_SHORT).show()
            }

            PlayMode.SHUFFLE -> {
                Toast.makeText(this, "随机播放", Toast.LENGTH_SHORT).show()
            }

            PlayMode.REPEAT -> {
                Toast.makeText(this, "单曲循环", Toast.LENGTH_SHORT).show()
            }
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

        // 设置通知内容（歌曲名和艺术家）
        notificationBuilder.setContentTitle(name)
        notificationBuilder.setContentText(artist)

        // 构建通知
        val notification = notificationBuilder.build()

        // 显示通知
        startForeground(1, notification)
    }

    inner class DatabaseHelper(context: Context) :
        SQLiteOpenHelper(context, "songs.db", null, 2) {

        override fun onCreate(db: SQLiteDatabase?) {
            db?.execSQL("CREATE TABLE songs (_id TEXT PRIMARY KEY, name TEXT, artist TEXT, album TEXT)")
            db?.execSQL("CREATE TABLE new_songs (_id INTEGER PRIMARY KEY AUTOINCREMENT, url TEXT, name TEXT, artist TEXT, album TEXT)")
            db?.execSQL("CREATE TABLE song_heart (_id INTEGER PRIMARY KEY AUTOINCREMENT, url TEXT, name TEXT, artist TEXT, album TEXT)")        }

        override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
            if (oldVersion < newVersion) {
                // Perform necessary upgrade operations here
            }
        }

        override fun onDowngrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
            if (newVersion < oldVersion) {
                // Perform necessary downgrade operations here
                db?.execSQL("DROP TABLE IF EXISTS songs")
                db?.execSQL("DROP TABLE IF EXISTS new_songs")
                db?.execSQL("DROP TABLE IF EXISTS song_heart")
                onCreate(db)
            }
        }
    }
}




