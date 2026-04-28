package com.example.comforterproject.model

import java.io.Serializable

data class MusicAlbumResponse(
    val response: String,
    val data: List<MusicAlbumItem>,
    val image_path: String?
)

data class MusicAlbumItem(
    val id: String,
    val albumName: String,
    val imageName: String?
)

data class MusicSongsResponse(
    val response: String,
    val data: List<MusicSongItem>,
    val file_path: String?
)

data class MusicSongItem(
    val id: String? = null,
    val albumId: String,
    val songsName: String,
    val songsFile: String?
) : Serializable
