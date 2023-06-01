package com.wangjingbo.low.fragment

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.squareup.picasso.Picasso
import com.wangjingbo.low.R
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class SearchFragment : Fragment() {
    private lateinit var searchEditText: EditText
    private lateinit var searchButton: Button
    private lateinit var musicListView: ListView
    private lateinit var musicList: ArrayList<Song>
    private lateinit var musicListAdapter: MusicListAdapter
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        searchEditText = view.findViewById(R.id.searchEditText)
        searchButton = view.findViewById(R.id.searchButton)
        musicListView = view.findViewById(R.id.musicListView)

        dbHelper = DatabaseHelper(requireContext())

        musicList = ArrayList()
        musicListAdapter = MusicListAdapter(requireContext(), R.layout.list_item_music, musicList)
        musicListView.adapter = musicListAdapter

        searchButton.setOnClickListener {
            val keywords = searchEditText.text.toString()
            searchMusic(keywords)
        }

        musicListView.onItemClickListener =
            AdapterView.OnItemClickListener { _, view, position, _ ->
                val selectedSong = musicListAdapter.getItem(position)
                insertSongToDatabase(selectedSong)
            }

        return view
    }

    private fun searchMusic(keywords: String) {
        Thread {
            try {
                val url = URL("http://38.47.97.104:3000/search?keywords=$keywords")
                val connection = url.openConnection() as HttpURLConnection
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

                    val jsonResponse = JSONObject(response.toString())
                    val songs = jsonResponse.getJSONObject("result").getJSONArray("songs")

                    requireActivity().runOnUiThread {
                        musicList.clear()

                        for (i in 0 until songs.length()) {
                            val song = songs.getJSONObject(i)
                            val songId = song.getString("id")
                            val songName = song.getString("name")
                            val artists = song.getJSONArray("artists")
                            val artist = artists.getJSONObject(0).getString("name")
                            val album = song.getJSONObject("album")
                            val albumName = album.getString("name")
                            val imageUrl = album.getJSONObject("artist").getString("img1v1Url") // 获取图片URL

                            val songData = Song(songId, songName, artist, albumName, imageUrl) // 传递图片URL
                            musicList.add(songData)
                        }

                        musicListAdapter.notifyDataSetChanged()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun insertSongToDatabase(song: Song?) {
        song?.let {
            val database = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put("_id", song.id)
                put("name", song.name)
                put("artist", song.artist)
                put("album", song.album)
                put("imageurl", song.imageUrl) // 保存图片URL到数据库
            }

            val rowId = database.insert("songs", null, values)
            if (rowId != -1L) {
                Toast.makeText(requireContext(), "音乐已加入音乐列表", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    "音乐已加入过音乐列表",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    inner class Song(
        val id: String,
        val name: String,
        val artist: String,
        val album: String,
        val imageUrl: String // 新添加的属性
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

            // 使用第三方库（如Picasso、Glide等）加载并显示图片
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        musicListView.adapter = null
    }
}