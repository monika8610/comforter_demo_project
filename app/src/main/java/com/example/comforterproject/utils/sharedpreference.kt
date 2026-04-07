package com.example.comforterproject.utils

import android.content.Context

class PrefManager(context: Context) {

    private val sharedPref = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        sharedPref.edit().putString("USER_TOKEN", token).apply()
    }

    fun getToken(): String? {
        return sharedPref.getString("USER_TOKEN", null)
    }
}