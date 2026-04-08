package com.example.comforterproject.repository

import com.example.comforterproject.network.RetrofitClient

class PromiseRepository {

    suspend fun getTodaysPromise() = RetrofitClient.api.getTodaysPromise()
}
