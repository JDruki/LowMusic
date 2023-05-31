package com.wangjingbo.low.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "songs.db", null, 2) {

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("CREATE TABLE songs (_id TEXT PRIMARY KEY, name TEXT, artist TEXT, album TEXT)")
        db?.execSQL("CREATE TABLE new_songs (_id INTEGER PRIMARY KEY AUTOINCREMENT, url TEXT, name TEXT, artist TEXT, album TEXT)")
        db?.execSQL("CREATE TABLE song_heart (_id INTEGER PRIMARY KEY AUTOINCREMENT, songName TEXT, artist TEXT, album TEXT)")
    }

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
