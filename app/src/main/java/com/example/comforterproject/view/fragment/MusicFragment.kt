package com.example.comforterproject.view.fragment

import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.comforterproject.R
import com.example.comforterproject.repository.MusicRepository
import com.example.comforterproject.view.activity.HomeActivity
import com.example.comforterproject.view.adapter.MusicAlbumAdapter
import kotlinx.coroutines.launch

class MusicFragment : Fragment() {

    companion object {
        private const val TAG = "MusicFragment"
    }

    private val musicRepository = MusicRepository()
    private var albumRecyclerView: RecyclerView? = null
    private var emptyTextView: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_music, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        albumRecyclerView = view.findViewById(R.id.albumRecyclerView)
        emptyTextView = view.findViewById(R.id.emptyTextView)

        albumRecyclerView?.layoutManager = GridLayoutManager(requireContext(), 2)
        if (albumRecyclerView?.itemDecorationCount == 0) {
            albumRecyclerView?.addItemDecoration(GridSpacingItemDecoration(2, 18))
        }

        loadAlbums()
    }

    override fun onDestroyView() {
        albumRecyclerView = null
        emptyTextView = null
        super.onDestroyView()
    }

    private fun loadAlbums() {
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                musicRepository.getAlbums()
            }.onSuccess { response ->
                val body = response.body()
                val albums = body?.data.orEmpty()
                val imageBaseUrl = normalizeImageBaseUrl(body?.image_path)

                if (response.isSuccessful && albums.isNotEmpty() && imageBaseUrl != null) {
                    albumRecyclerView?.adapter = MusicAlbumAdapter(
                        items = albums,
                        imageBaseUrl = imageBaseUrl
                    ) { albumId, albumName, imageUrl ->
                        (activity as? HomeActivity)?.openMusicDetail(albumId, albumName, imageUrl)
                    }
                    albumRecyclerView?.isVisible = true
                    emptyTextView?.isVisible = false
                } else {
                    Log.d(TAG, "Albums unavailable or image path missing")
                    albumRecyclerView?.isVisible = false
                    emptyTextView?.isVisible = true
                    Toast.makeText(
                        requireContext(),
                        "Audio albums not available",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }.onFailure {
                Log.e(TAG, "Unable to load albums", it)
                albumRecyclerView?.isVisible = false
                emptyTextView?.isVisible = true
                Toast.makeText(
                    requireContext(),
                    "Unable to load albums",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun normalizeImageBaseUrl(imagePath: String?): String? {
        if (imagePath.isNullOrBlank()) return null
        return when {
            imagePath.startsWith("//") -> "https:$imagePath"
            imagePath.startsWith("http://") || imagePath.startsWith("https://") -> imagePath
            else -> "https://$imagePath"
        }
    }

    private class GridSpacingItemDecoration(
        private val spanCount: Int,
        private val spacing: Int
    ) : RecyclerView.ItemDecoration() {

        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val position = parent.getChildAdapterPosition(view)
            val column = position % spanCount

            outRect.left = spacing - column * spacing / spanCount
            outRect.right = (column + 1) * spacing / spanCount
            outRect.bottom = spacing
            if (position < spanCount) {
                outRect.top = spacing
            }
        }
    }
}
