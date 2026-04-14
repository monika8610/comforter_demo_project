package com.example.comforterproject.view.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.comforterproject.R
import com.example.comforterproject.repository.BannerRepository
import com.example.comforterproject.repository.PromiseRepository
import com.example.comforterproject.view.adapter.BannerPagerAdapter
import com.example.comforterproject.view.adapter.PromiseAdapter
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    companion object {
        private const val TAG = "HomeFragment"
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bannerPager = view.findViewById(R.id.bannerPager)
        promiseRecyclerView = view.findViewById(R.id.promiseRecyclerView)
        promiseRecyclerView?.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        loadBanners()
        loadPromises()
    }

    override fun onResume() {
        super.onResume()
        startAutoScroll()
    }

    override fun onPause() {
        super.onPause()
        stopAutoScroll()
    }

    override fun onDestroyView() {
        stopAutoScroll()
        bannerPager = null
        promiseRecyclerView = null
        super.onDestroyView()
    }

    private fun loadBanners() {
        viewLifecycleOwner.lifecycleScope.launch {
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
                        requireContext(),
                        "Banner images not available",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }.onFailure {
                Log.e(TAG, "Unable to load banner", it)
                Toast.makeText(
                    requireContext(),
                    "Unable to load banner",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun loadPromises() {
        viewLifecycleOwner.lifecycleScope.launch {
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
                        requireContext(),
                        "Promise verse not available",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }.onFailure {
                Log.e(TAG, "Unable to load promise verse", it)
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
}
