package com.wangjingbo.low.Activity

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.wangjingbo.low.R
import com.wangjingbo.low.Routh.PlayMode
import com.wangjingbo.low.Server.MusicPlayerService
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.media.MediaPlayer

// 音乐播放器类
class MusicPlayer : AppCompatActivity(), View.OnClickListener {
    private lateinit var playButton: Button
    private lateinit var nextButton: Button
    private lateinit var previousButton: Button
    private lateinit var songNameTextView: TextView
    private lateinit var artistTextView: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var hartMusicButton: Button
    private lateinit var playModeButton: Button
    private lateinit var songUrl: String
    private lateinit var songName: String
    private lateinit var artist: String
    private lateinit var album: String
    private var isPlaying: Boolean = false
    private var currentProgress: Int = 0
    private var mod: Int = 0
    private val progressReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val progress = intent?.getIntExtra("progress", 0) ?: 0
            seekBar.progress = progress
        }
    }
    private val seekToReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val progress = intent?.getIntExtra("progress", 0) ?: 0
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter("SEEK_TO")
        registerReceiver(seekToReceiver, filter)
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(seekToReceiver)

    }


    // 创建活动
    override fun onCreate(savedInstanceState: Bundle?) {
        val filter = IntentFilter("PLAY_SONG")
        registerReceiver(playSongReceiver, filter)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_player)
        playButton = findViewById(R.id.playButton)
        nextButton = findViewById(R.id.nextButton)
        previousButton = findViewById(R.id.previousButton)
        songNameTextView = findViewById(R.id.songNameTextView)
        artistTextView = findViewById(R.id.artistTextView)
        seekBar = findViewById(R.id.seekBar)
        hartMusicButton = findViewById(R.id.HartMusic)
        playModeButton = findViewById(R.id.playModeButton)
        playButton.setOnClickListener(this)
        playButton.isEnabled = true // 启用播放按钮
        nextButton.setOnClickListener(this)
        previousButton.setOnClickListener(this)
        hartMusicButton.setOnClickListener(this)
        playModeButton.setOnClickListener(this)
        seekBar = findViewById(R.id.seekBar)
        // 从Intent中获取音乐相关数据
        val extras = intent.extras
        if (extras != null) {
            songUrl = extras.getString("url", "")
            songName = extras.getString("name", "")
            artist = extras.getString("artist", "")
            album = extras.getString("album", "")
        }

        val intent = Intent(this, MusicPlayerService::class.java).apply {
            putExtra("url", songUrl)
            putExtra("name", songName)
            putExtra("artist", artist)
            putExtra("album", album)
        }
        // 启动服务并传递Intent
        startService(intent)
        updateSongDetails(songName,artist)

        // 设置进度条监听器
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val intent = Intent("SEEK_TO").apply {
                        putExtra("progress", progress)
                    }
                    sendBroadcast(intent)
                }
            }



            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // 未使用
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // 未使用
            }
        })

    }
    // 更新歌曲详情
    private fun updateSongDetails(name: String?, artist: String?) {
        songNameTextView.text = name
        artistTextView.text = artist
    }

    private val playSongReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val receivedSongName = intent?.getStringExtra("songName")
            val receivedArtist = intent?.getStringExtra("artist")
            updateSongDetails(receivedSongName, receivedArtist)
        }
    }

    // 播放歌曲
// 播放歌曲
    private fun playSong() {
        val pauseResumeIntent = Intent("PAUSE_RESUME")
        sendBroadcast(pauseResumeIntent)
        isPlaying = !isPlaying

    }


    // 播放下一首歌曲
    private fun playNextSong() {
        val intent = Intent("PLAY_NEXT_SONG")
        sendBroadcast(intent)
    }

    // 播放上一首歌曲
    private fun playPreviousSong() {
        val broadcastIntent = Intent("PLAY_PREVIOUS_SONG")
        sendBroadcast(broadcastIntent)
    }

    // 歌曲数据类
    data class Song(val url: String, val name: String, val artist: String, val album : String) {
    }

    // 切换播放模式
    private fun togglePlayMode() {
        val togglePlayModeIntent = Intent("TOGGLE_PLAY_MODE")
        sendBroadcast(togglePlayModeIntent)
    }

    // 将歌曲插入数据库
    private fun insertSongToDatabase() {
        val dbHelper = DatabaseHelper(this)
        val database = dbHelper.writableDatabase

        val selection = "url = ?"
        val selectionArgs = arrayOf(songUrl)

        val cursor = database.query(
            "song_heart",
            null,
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        val isSongAlreadyAdded = cursor.count > 0
        cursor.close()

        if (!isSongAlreadyAdded) {
            val values = ContentValues().apply {
                put("name", songName)
                put("artist", artist)
                put("album", album)
                put("url", songUrl)
            }

            val rowId = database.insert("song_heart", null, values)
            if (rowId != -1L) {
                Toast.makeText(this, "音乐已加入心动列表", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "音乐加入错误", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "音乐已加入过心动列表", Toast.LENGTH_SHORT).show()
        }
    }

    // 点击事件处理
    override fun onClick(view: View) {
        when (view.id) {
            R.id.playButton -> {
                if (!isPlaying) {
                    playSong()
                    playButton.text = "播放"
                    Toast.makeText(this, "音乐已暂停", Toast.LENGTH_SHORT).show()
                }
                else{
                    playSong()
                    playButton.text = "暂停"
                    Toast.makeText(this, "音乐已播放", Toast.LENGTH_SHORT).show()
                }
            }
            R.id.nextButton -> {
                playNextSong()
            }

            R.id.previousButton -> {
                playPreviousSong()
            }

            R.id.HartMusic -> {
                insertSongToDatabase()
            }

            R.id.playModeButton -> {
                togglePlayMode()
                // 更新播放模式按钮的图标和提示
                when (mod) {
                    0 -> {
                        playModeButton.text = "随机播放"

                        mod =1
                    }

                    1 -> {
                        playModeButton.text = "单曲循环"

                        mod =2
                    }

                    2 -> {
                        playModeButton.text = "顺序播放"

                        mod =0
                    }
                }
            }
        }
    }

    // 数据库帮助类
    inner class DatabaseHelper(context: Context) :
        SQLiteOpenHelper(context, "songs.db", null, 2) {

        // 创建数据库
        override fun onCreate(db: SQLiteDatabase?) {
            db?.execSQL("CREATE TABLE songs (_id TEXT PRIMARY KEY, name TEXT, artist TEXT, album TEXT)")
            db?.execSQL("CREATE TABLE new_songs (_id INTEGER PRIMARY KEY AUTOINCREMENT, url TEXT, name TEXT, artist TEXT, album TEXT)")
            db?.execSQL("CREATE TABLE song_heart (_id INTEGER PRIMARY KEY AUTOINCREMENT, url TEXT, name TEXT, artist TEXT, album TEXT)")
        }

        // 数据库升级
        override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
            if (oldVersion < newVersion) {
                // 在此执行必要的升级操作
            }
        }

        // 数据库降级
        override fun onDowngrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
            if (newVersion < oldVersion) {
                // 在此执行必要的降级操作
                db?.execSQL("DROP TABLE IF EXISTS songs")
                db?.execSQL("DROP TABLE IF EXISTS new_songs")
                db?.execSQL("DROP TABLE IF EXISTS song_heart")
                onCreate(db)
            }
        }
    }
}





