package com.example.comforterproject.model

data class AuthResponse(
    val status: Boolean,
    val message: String,
    val token: String? = null
)