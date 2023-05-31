package com.wangjingbo.low.fragment

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.wangjingbo.low.Activity.MusicPlayer
import com.wangjingbo.low.R

class PlaylistFragment : Fragment() {
    private lateinit var playlistListView: ListView
    private lateinit var playlistAdapter: PlaylistAdapter
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_playlist, container, false)

        playlistListView = view.findViewById(R.id.playlistListView)

        dbHelper = DatabaseHelper(requireContext())

        val playlist = loadPlaylist()
        playlistAdapter = PlaylistAdapter(requireContext(), R.layout.list_item_song, playlist)
        playlistListView.adapter = playlistAdapter

        playlistListView.setOnItemClickListener { _, _, position, _ ->
            val selectedSong = playlist[position]
            playSong(selectedSong.url)
        }

        playlistListView.setOnItemLongClickListener { _, _, position, _ ->
            val selectedSong = playlist[position]
            deleteSong(selectedSong)
            true
        }

        return view
    }

    private fun loadPlaylist(): List<Song> {
        val dbHelper = DatabaseHelper(requireContext())
        val database = dbHelper.readableDatabase

        val columns = arrayOf("_id", "url", "name", "artist", "album")

        val cursor = database.query(
            "new_songs",
            columns,
            null,
            null,
            null,
            null,
            "_id ASC"
        )

        val playlist = mutableListOf<Song>()
        val urlSet = HashSet<String>() // HashSet to store unique URLs

        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndex("_id"))
            val url = cursor.getString(cursor.getColumnIndex("url"))
            val name = cursor.getString(cursor.getColumnIndex("name"))
            val artist = cursor.getString(cursor.getColumnIndex("artist"))
            val album = cursor.getString(cursor.getColumnIndex("album"))

            // Check if the URL already exists in the set
            if (!urlSet.contains(url)) {
                val song = Song(id, url, name, artist, album)
                playlist.add(song)
                urlSet.add(url) // Add the URL to the set
            }
        }

        cursor.close()
        database.close()

        return playlist
    }

    private fun playSong(url: String) {
        val intent = Intent(requireContext(), MusicPlayer::class.java)
        intent.putExtra("url", url)
        startActivity(intent)
    }

    private fun deleteSong(song: Song) {
        val dbHelper = DatabaseHelper(requireContext())
        val database = dbHelper.writableDatabase

        val selection = "_id = ?"
        val selectionArgs = arrayOf(song.id.toString())

        database.delete("new_songs", selection, selectionArgs)

        database.close()

        // Refresh the playlist after deletion
        val updatedPlaylist = loadPlaylist()
        playlistAdapter.clear()
        playlistAdapter.addAll(updatedPlaylist)
        playlistAdapter.notifyDataSetChanged()
    }

    inner class Song(
        val id: Int,
        val url: String,
        val name: String,
        val artist: String,
        val album: String
    )

    inner class PlaylistAdapter(
        context: Context,
        resource: Int,
        objects: List<Song>
    ) : ArrayAdapter<Song>(context, resource, objects) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView
                ?: LayoutInflater.from(context).inflate(R.layout.list_item_song, parent, false)

            val songNameTextView = view.findViewById<TextView>(R.id.songNameTextView)
            val artistTextView = view.findViewById<TextView>(R.id.artistTextView)
            val albumTextView = view.findViewById<TextView>(R.id.albumTextView)

            val song = getItem(position)
            songNameTextView.text = song?.name
            artistTextView.text = song?.artist
            albumTextView.text = song?.album

            return view
        }
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