package com.example.comforterproject.model

data class SignupRequest(
    val email: String,
    val password: String,
    val name: String,
    val phone: String,
    val password_confirmation: String
)