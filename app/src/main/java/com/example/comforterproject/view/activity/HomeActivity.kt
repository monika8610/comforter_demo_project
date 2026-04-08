package com.example.comforterproject.view.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.comforterproject.R
import com.example.comforterproject.repository.BannerRepository
import com.example.comforterproject.repository.PromiseRepository
import com.example.comforterproject.view.adapter.BannerPagerAdapter
import com.example.comforterproject.view.adapter.PromiseAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "HomeActivity"
    }
    private val bannerRepository = BannerRepository()
    private val promiseRepository = PromiseRepository()
    private val autoScrollHandler = Handler(Looper.getMainLooper())
    private var bannerPager: ViewPager2? = null
    private var promiseRecyclerView: RecyclerView? = null
    private var bannerUrls: List<String> = emptyList()
    private val autoScrollRunnable = object : Runnable {
        override fun run() {
            val pager = bannerPager ?: return
            if (bannerUrls.size <= 1) return

            val nextItem = if (pager.currentItem == bannerUrls.lastIndex) {
                0
            } else {
                pager.currentItem + 1
            }

            pager.setCurrentItem(nextItem, true)
            autoScrollHandler.postDelayed(this, 3000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bannerPager = findViewById(R.id.bannerPager)
        promiseRecyclerView = findViewById(R.id.promiseRecyclerView)
        promiseRecyclerView?.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        loadBanners()
        loadPromises()

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

    override fun onResume() {
        super.onResume()
        startAutoScroll()
    }

    override fun onPause() {
        super.onPause()
        stopAutoScroll()
    }

    private fun loadBanners() {
        lifecycleScope.launch {
            runCatching {
                bannerRepository.getBanners(languageId = 5)
            }.onSuccess { response ->
                val imageUrls = response.body()
                    ?.response
                    ?.mapNotNull { it.imageNameMain?.takeIf(String::isNotBlank) }
                    .orEmpty()

                if (response.isSuccessful && imageUrls.isNotEmpty()) {
                    bannerUrls = imageUrls
                    Log.d(TAG, "Loading ${imageUrls.size} banner images")
                    bannerPager?.adapter = BannerPagerAdapter(imageUrls)
                    startAutoScroll()
                } else {
                    Log.d(TAG, "Banner response received but image URLs were empty")
                    Toast.makeText(
                        this@HomeActivity,
                        "Banner images not available",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }.onFailure {
                Log.e(TAG, "Unable to load banner", it)
                Toast.makeText(
                    this@HomeActivity,
                    "Unable to load banner",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun startAutoScroll() {
        stopAutoScroll()
        if (bannerUrls.size > 1) {
            autoScrollHandler.postDelayed(autoScrollRunnable, 3000)
        }
    }

    private fun stopAutoScroll() {
        autoScrollHandler.removeCallbacks(autoScrollRunnable)
    }

    private fun loadPromises() {
        lifecycleScope.launch {
            runCatching {
                promiseRepository.getTodaysPromise()
            }.onSuccess { response ->
                val items = response.body()
                    ?.response
                    ?.filter {
                        !it.imageNameLarge.isNullOrBlank() || !it.imageNameThumb.isNullOrBlank()
                    }
                    .orEmpty()

                if (response.isSuccessful && items.isNotEmpty()) {
                    promiseRecyclerView?.adapter = PromiseAdapter(items)
                } else {
                    Toast.makeText(
                        this@HomeActivity,
                        "Promise verse not available",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }.onFailure {
                Log.e(TAG, "Unable to load promise verse", it)
            }
        }
    }
}
