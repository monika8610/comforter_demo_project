package com.example.comforterproject.repository

import com.example.comforterproject.network.RetrofitClient

class BannerRepository {

    suspend fun getBanners(languageId: Int) =
        RetrofitClient.api.getBanners(languageId)
}
