package com.example.comforterproject.view.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.comforterproject.R
import com.example.comforterproject.view.fragment.HomeFragment
import com.example.comforterproject.view.fragment.MusicDetailFragment
import com.example.comforterproject.view.fragment.MusicFragment
import com.example.comforterproject.view.fragment.SongFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
class HomeActivity : AppCompatActivity() {
    private lateinit var bottomNav: BottomNavigationView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        bottomNav = findViewById(R.id.bottomNavigation)
        bottomNav.setOnItemSelectedListener { item ->

            when (item.itemId) {

                R.id.nav_home -> {
                    showFragment(HomeFragment())
                    true
                }

                R.id.nav_music -> {
                    showFragment(MusicFragment())
                    true
                }

                R.id.nav_message -> {
                    Toast.makeText(this, "Message Clicked", Toast.LENGTH_SHORT).show()
                    true
                }

                R.id.nav_logout -> {
                    Toast.makeText(this, "Logout Clicked", Toast.LENGTH_SHORT).show()
                    val pref = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                    pref.edit().clear().apply()
                    startActivity(Intent(this, SignupActivity::class.java))
                    finish()
                    true
                }

                else -> false
            }
        }

        if (savedInstanceState == null) {
            bottomNav.selectedItemId = R.id.nav_home
        }

        supportFragmentManager.addOnBackStackChangedListener {
            syncBottomNavVisibility()
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.homeFragmentContainer, fragment)
            .commit()
        syncBottomNavVisibility()
    }

    fun openMusicDetail(albumId: String, albumName: String, imageUrl: String?) {
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.homeFragmentContainer,
                MusicDetailFragment.newInstance(albumId, albumName, imageUrl)
            )
            .addToBackStack(MusicDetailFragment::class.java.simpleName)
            .commit()
        syncBottomNavVisibility()
    }

    fun openSongPlayer(
        albumId: String,
        albumName: String,
        imageUrl: String?,
        initialSongFile: String?
    ) {
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.homeFragmentContainer,
                SongFragment.newInstance(albumId, albumName, imageUrl, initialSongFile)
            )
            .addToBackStack(SongFragment::class.java.simpleName)
            .commit()
        syncBottomNavVisibility()
    }

    private fun syncBottomNavVisibility() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.homeFragmentContainer)
        bottomNav.visibility = if (
            currentFragment is MusicDetailFragment || currentFragment is SongFragment
        ) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }
}
