package com.example.comforterproject.view.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.comforterproject.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {

                R.id.nav_home -> {
                    Toast.makeText(this, "Home Clicked", Toast.LENGTH_SHORT).show()
                    true
                }

                R.id.nav_music -> {
                    Toast.makeText(this, "Music Clicked", Toast.LENGTH_SHORT).show()
                    true
                }

                R.id.nav_message -> {
                    Toast.makeText(this, "Message Clicked", Toast.LENGTH_SHORT).show()
                    true
                }

                R.id.nav_logout -> {
                    Toast.makeText(this, "Logout Clicked", Toast.LENGTH_SHORT).show()

                    // 🔥 Clear token (logout)
                    val pref = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                    pref.edit().clear().apply()

                    // 🔥 Go back to login/signup
                    startActivity(Intent(this, SignupActivity::class.java))
                    finish()
                    true
                }

                else -> false
            }
        }
    }
}