package com.example.comforterproject.repository

import com.example.comforterproject.model.LoginRequest
import com.example.comforterproject.model.SignupRequest
import com.example.comforterproject.network.RetrofitClient

class AuthRepository {

    suspend fun signup(request: SignupRequest) =
        RetrofitClient.api.signup(
            request.email,
            request.password,
            request.name,
            request.phone,
            request.password_confirmation
        )

    suspend fun login(request: LoginRequest) =
        RetrofitClient.api.login(
            request.email,
            request.password
        )
}