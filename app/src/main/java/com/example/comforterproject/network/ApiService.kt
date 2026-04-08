package com.example.comforterproject.network

import com.example.comforterproject.model.AuthResponse
import com.example.comforterproject.model.BannerResponse
import com.example.comforterproject.model.PromiseResponse
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @FormUrlEncoded
    @POST("register")
    suspend fun signup(
        @Field("email") email: String,
        @Field("password") password: String,
        @Field("name") name: String,
        @Field("phone") phone: String,
        @Field("password_confirmation") passwordConfirmation: String
    ): Response<AuthResponse>

    @FormUrlEncoded
    @POST("login")
    suspend fun login(
        @Field("email") email: String,
        @Field("password") password: String
    ): Response<AuthResponse>

    @GET("banners")
    suspend fun getBanners(
        @Query("languageid") languageId: Int
    ): Response<BannerResponse>

    @GET("todays-promise")
    suspend fun getTodaysPromise(): Response<PromiseResponse>
}
