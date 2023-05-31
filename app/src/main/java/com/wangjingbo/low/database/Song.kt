package com.wangjingbo.low.database

//import androidx.room.ColumnInfo
//import androidx.room.Entity
//import androidx.room.PrimaryKey
//
//@Entity(tableName = "song")
//data class Song(
//    @PrimaryKey val id: Int,
//    val name: String,
//    val duration: Int,
//    val mvid: Int,
//    val fee: Int,
//    val status: Int,
//    @ColumnInfo(name = "album_id") val albumId: Int,
//    @ColumnInfo(name = "artist_id") val artistId: Int,
//    @ColumnInfo(name = "pic_url") val picUrl: String?,
//    @ColumnInfo(name = "img1v1_url") val img1v1Url: String?
//)
data class Song(val url: String, val name: String, val artist: String, val album: String)