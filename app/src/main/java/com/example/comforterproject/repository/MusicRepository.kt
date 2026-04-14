package com.example.comforterproject.repository

import com.example.comforterproject.network.RetrofitClient

class MusicRepository {

    suspend fun getAlbums() =
        RetrofitClient.api.getAlbums("https://comforterradio.com/api/get-album.php")

    suspend fun getSongs(albumId: String) =
        RetrofitClient.api.getSongs("https://comforterradio.com/api/get-music.php?albumId=$albumId")
}
