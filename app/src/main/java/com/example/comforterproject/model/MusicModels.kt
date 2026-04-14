package com.example.comforterproject.model

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
    val albumId: String,
    val songsName: String,
    val songsFile: String?
)
