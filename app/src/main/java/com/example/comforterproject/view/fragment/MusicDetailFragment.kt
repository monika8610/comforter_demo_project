package com.example.comforterproject.view.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.comforterproject.R
import com.example.comforterproject.model.MusicSongItem
import com.example.comforterproject.repository.MusicRepository
import com.example.comforterproject.view.activity.HomeActivity
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class MusicDetailFragment : Fragment() {

    companion object {
        private const val ARG_ALBUM_ID = "arg_album_id"
        private const val ARG_ALBUM_NAME = "arg_album_name"
        private const val ARG_IMAGE_URL = "arg_image_url"
        private const val TAG = "MusicDetailFragment"

        fun newInstance(albumId: String, albumName: String, imageUrl: String?): MusicDetailFragment {
            return MusicDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ALBUM_ID, albumId)
                    putString(ARG_ALBUM_NAME, albumName)
                    putString(ARG_IMAGE_URL, imageUrl)
                }
            }
        }
    }

    private val musicRepository = MusicRepository()
    private var firstSong: MusicSongItem? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_music_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val albumId = arguments?.getString(ARG_ALBUM_ID).orEmpty()
        val albumName = arguments?.getString(ARG_ALBUM_NAME).orEmpty()
        val imageUrl = arguments?.getString(ARG_IMAGE_URL)

        val backButton = view.findViewById<ImageButton>(R.id.backButton)
        val coverImage = view.findViewById<ImageView>(R.id.detailAlbumImage)
        val titleView = view.findViewById<TextView>(R.id.detailAlbumTitle)
        val songTitleView = view.findViewById<TextView>(R.id.detailSongTitle)
        val playRow = view.findViewById<MaterialCardView>(R.id.playRowCard)

        titleView.text = albumName
        songTitleView.text = "Loading..."

        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.home_banner_placeholder)
            .error(R.drawable.home_banner_placeholder)
            .into(coverImage)

        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        playRow.setOnClickListener {
            val songFile = firstSong?.songsFile
            if (songFile.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Song not available", Toast.LENGTH_SHORT).show()
            } else {
                (activity as? HomeActivity)?.openSongPlayer(albumId, albumName, imageUrl, songFile)
            }
        }

        loadSongs(albumId, songTitleView)
    }

    private fun loadSongs(albumId: String, songTitleView: TextView) {
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                musicRepository.getSongs(albumId)
            }.onSuccess { response ->
                val songs = response.body()?.data.orEmpty()
                firstSong = songs.firstOrNull()
                songTitleView.text = firstSong?.songsName ?: "Song not available"
            }.onFailure {
                Log.e(TAG, "Unable to load songs", it)
                songTitleView.text = "Song not available"
            }
        }
    }
}
