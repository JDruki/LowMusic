package com.wangjingbo.low.fragment

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.squareup.picasso.Picasso
import com.wangjingbo.low.Activity.MusicPlayer
import com.wangjingbo.low.R
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MusicListFragment : Fragment() {

    private lateinit var musicListView: ListView
    private lateinit var musicList: ArrayList<Song>
    private lateinit var musicListAdapter: MusicListAdapter
    private lateinit var dbHelper: DatabaseHelper
    private var selectedSong: Song? = null
    private lateinit var url: String  // 在这里声明url变量
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_music_list, container, false)

        musicListView = view.findViewById(R.id.musicListView)

        dbHelper = DatabaseHelper(requireContext())

        musicList = ArrayList()
        musicListAdapter = MusicListAdapter(requireContext(), R.layout.list_item_music, musicList)
        musicListView.adapter = musicListAdapter

        musicListView.setOnItemClickListener { _, _, position, _ ->
            selectedSong = musicListAdapter.getItem(position)
            selectedSong?.let {
                val songId = it.id
                val songLevel = "exhigh"
                dbHelper.GetSongUrlTask().execute(songId, songLevel)
            }
        }


        musicListView.setOnItemLongClickListener { _, _, position, _ ->
            val selectedSong = musicListAdapter.getItem(position)
            selectedSong?.let {
                // 执行删除或下载操作
                val songId = it.id
                val songLevel = "exhigh"
                dbHelper.GetSongUrlTask().execute(songId, songLevel)
                deleteOrDownloadSong(selectedSong)
            }
            true
        }

        loadSavedData()

        return view
    }

    private fun loadSavedData() {
        val dbHelper = DatabaseHelper(requireContext())
        val database = dbHelper.readableDatabase

        val columns = arrayOf("_id", "name", "artist", "album", "imageurl")

        val cursor = database.query(
            "songs",
            columns,
            null,
            null,
            null,
            null,
            "name ASC"
        )

        musicList.clear()

        while (cursor.moveToNext()) {
            val id = cursor.getString(cursor.getColumnIndex("_id"))
            val name = cursor.getString(cursor.getColumnIndex("name"))
            val artist = cursor.getString(cursor.getColumnIndex("artist"))
            val imageUrl = cursor.getString(cursor.getColumnIndex("imageurl"))
            val album = cursor.getString(cursor.getColumnIndex("album"))

            val song = Song(id, name, artist, album, imageUrl)
            musicList.add(song)
        }

        cursor.close()

        musicListAdapter.notifyDataSetChanged()
    }

    private fun deleteOrDownloadSong(song: Song) {
        val options = arrayOf("删除", "下载")

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("操作")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> deleteSong(song)
                1 -> downloadSong(song)
            }
        }
        builder.show()
    }

    private fun deleteSong(song: Song) {
        val dbHelper = DatabaseHelper(requireContext())
        dbHelper.deleteSong(song)
        loadSavedData()
    }

    private fun downloadSong(song: Song) {
        if (!::url.isInitialized) {
            // 等待一段时间并重新尝试执行下载操作
            Handler(Looper.getMainLooper()).postDelayed({
                downloadSong(song)
            }, 1000) // 这里设置等待的时间，单位为毫秒
            return
        }
        activity?.runOnUiThread {
            Toast.makeText(activity, url, Toast.LENGTH_SHORT).show()
        }
        // 执行下载操作
        val fileName = "${song.name}.mp3" // 设置保存的文件名

        val downloadManager =
            requireActivity().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadUri = Uri.parse(url)

        val request = DownloadManager.Request(downloadUri).apply {
            setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            setTitle(song.name)
            setDescription(song.artist)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        }

        downloadManager.enqueue(request)
    }


    inner class Song(
        val id: String,
        val name: String,
        val artist: String,
        val album: String,
        val imageUrl: String
    )

    inner class MusicListAdapter(
        context: Context,
        resource: Int,
        objects: List<Song>
    ) : ArrayAdapter<Song>(context, resource, objects) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView
                ?: LayoutInflater.from(context).inflate(R.layout.list_item_music, parent, false)

            val songNameTextView = view.findViewById<TextView>(R.id.songNameTextView)
            val artistTextView = view.findViewById<TextView>(R.id.artistTextView)
            val albumTextView = view.findViewById<TextView>(R.id.albumTextView)
            val albumImageView = view.findViewById<ImageView>(R.id.albumImageView) // 添加ImageView
            val song = getItem(position)
            songNameTextView.text = song?.name
            artistTextView.text = song?.artist
            albumTextView.text = song?.album
            Picasso.get().load(song?.imageUrl).into(albumImageView)

            return view
        }
    }

    inner class DatabaseHelper(context: Context) :
        SQLiteOpenHelper(context, "songs.db", null, 2) {

        override fun onCreate(db: SQLiteDatabase?) {
            db?.execSQL("CREATE TABLE songs (_id TEXT PRIMARY KEY, name TEXT, artist TEXT, album TEXT, imageurl TEXT)") // 添加 url 列
            db?.execSQL("CREATE TABLE new_songs (_id INTEGER PRIMARY KEY AUTOINCREMENT, url TEXT, name TEXT, artist TEXT, album TEXT, imageurl TEXT)")
            db?.execSQL("CREATE TABLE song_heart (_id INTEGER PRIMARY KEY AUTOINCREMENT, url TEXT, name TEXT, artist TEXT, album TEXT, imageurl TEXT)")        }

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


        fun isSongExist(song: Song): Boolean {
            val database = readableDatabase
            val selection = "name = ? AND artist = ? AND album = ?"
            val selectionArgs = arrayOf(song.name, song.artist, song.album)

            val cursor = database.query(
                "songs",
                null,
                selection,
                selectionArgs,
                null,
                null,
                null
            )

            val exists = cursor.count > 0
            cursor.close()
            return exists
        }

        fun deleteSong(song: Song) {
            val database = writableDatabase
            val selection = "_id = ?"
            val selectionArgs = arrayOf(song.id)

            database.delete("songs", selection, selectionArgs)
            database.close()
        }

        fun insertSong(song: Song): Boolean {
            val database = writableDatabase
            val values = ContentValues().apply {
                put("_id", song.id)
                put("name", song.name)
                put("artist", song.artist)
                put("album", song.album)
                put("imageurl", song.imageUrl)
            }


            val selection = "name = ? AND artist = ? AND album = ?"
            val selectionArgs = arrayOf(song.name, song.artist, song.album)
            val cursor = database.query(
                "songs",
                null,
                selection,
                selectionArgs,
                null,
                null,
                null
            )

            val exists = cursor.count > 0
            cursor.close()

            if (!exists) {
                database.insert("songs", null, values)
            }

            database.close()

            return exists
        }
        inner class GetSongUrlTask : AsyncTask<String, Void, String>() {

            override fun doInBackground(vararg params: String): String? {
                val songId = params[0]
                val songLevel = params[1]
                val url = "http://38.47.97.104:3000/song/url/v1?id=$songId&level=$songLevel"

                try {
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"

                    val responseCode = connection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val inputStream = connection.inputStream
                        val reader = BufferedReader(InputStreamReader(inputStream))
                        val response = StringBuilder()
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            response.append(line)
                        }
                        reader.close()
                        inputStream.close()
                        return response.toString()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                return null
            }

            override fun onPostExecute(result: String?) {
                super.onPostExecute(result)

                result?.let {
                    try {
                        val jsonObject = JSONObject(it)
                        val dataArray = jsonObject.getJSONArray("data")
                        if (dataArray.length() > 0) {
                            val dataObject = dataArray.getJSONObject(0)
                            val id = dataObject.getInt("id")
                            url = dataObject.getString("url")

                            val dbHelper = DatabaseHelper(requireContext())

                            // Check if the song already exists in the database
                            val song = selectedSong ?: return
                            if (!dbHelper.isSongExist(song)) {
                                dbHelper.insertSong(song)
                            }

                            // Save the song to the new database
                            val newDbHelper = DatabaseHelper(requireContext())
                            val newDatabase = newDbHelper.writableDatabase

                            val values = ContentValues().apply {
                                put("url", url)
                                put("name", song.name)
                                put("artist", song.artist)
                                put("album", song.album)
                                put("imageurl",song.imageUrl)
                            }

                            newDatabase.insert("new_songs", null, values)
                            newDatabase.close()

                            // 跳转到MusicPlayerActivity并传递相关信息
                            val intent = Intent(requireContext(), MusicPlayer::class.java)
                            intent.putExtra("url", url)
                            intent.putExtra("name", song.name)
                            intent.putExtra("artist", song.artist)
                            intent.putExtra("album", song.album)
                            intent.putExtra("imageurl",song.imageUrl)
                            startActivity(intent)
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}