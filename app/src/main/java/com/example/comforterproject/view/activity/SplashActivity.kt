package com.example.comforterproject.view.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.comforterproject.R
import com.example.comforterproject.utils.PrefManager

class SplashActivity : AppCompatActivity() {
    private val splashDelayMillis = 2000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            val prefManager = PrefManager(this)
            val nextActivity = if (prefManager.isLoggedIn()) {
                HomeActivity::class.java
            } else {
                MainActivity::class.java
            }

            startActivity(Intent(this, nextActivity))
            finish()
        }, splashDelayMillis)
    }
}
