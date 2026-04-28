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
import com.bumptech.glide.Glide
import com.example.comforterproject.R
import com.example.comforterproject.model.MusicSongItem
import com.example.comforterproject.view.activity.HomeActivity
import com.google.android.material.card.MaterialCardView
class MusicDetailFragment : Fragment() {
    companion object {
        private const val ARG_ALBUM_ID = "arg_album_id"
        private const val ARG_ALBUM_NAME = "arg_album_name"
        private const val ARG_IMAGE_URL = "arg_image_url"
        private const val ARG_SONGS = "arg_songs"
        private const val ARG_AUDIO_BASE_URL = "arg_audio_base_url"
        private const val TAG = "MusicDetailFragment"
        fun newInstance(
            albumId: String,
            albumName: String,
            imageUrl: String?,
            songs: ArrayList<MusicSongItem>,
            audioBaseUrl: String?
        ): MusicDetailFragment {
            return MusicDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ALBUM_ID, albumId)
                    putString(ARG_ALBUM_NAME, albumName)
                    putString(ARG_IMAGE_URL, imageUrl)
                    putSerializable(ARG_SONGS, songs)
                    putString(ARG_AUDIO_BASE_URL, audioBaseUrl)
                }
            }
        }
    }

    private var firstSong: MusicSongItem? = null
    private var firstSongIndex: Int = -1

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
        val songs = readSongsArguments()
        val audioBaseUrl = arguments?.getString(ARG_AUDIO_BASE_URL)

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
                (activity as? HomeActivity)?.openSongPlayer(
                    albumId = albumId,
                    albumName = albumName,
                    imageUrl = imageUrl,
                    initialSongIndex = firstSongIndex,
                    initialSongId = firstSong?.id,
                    initialSongFile = songFile,
                    songs = songs,
                    audioBaseUrl = audioBaseUrl
                )
            }
        }

        bindSongs(songs, songTitleView)
    }

    private fun bindSongs(songs: List<MusicSongItem>, songTitleView: TextView) {
        firstSongIndex = songs.indexOfFirst { !it.songsFile.isNullOrBlank() }
            .takeIf { it >= 0 }
            ?: songs.indices.firstOrNull()
            ?: -1
        firstSong = songs.getOrNull(firstSongIndex)
        songTitleView.text = firstSong?.songsName ?: "Song not available"

        if (songs.isEmpty()) {
            Log.d(TAG, "No songs available for selected album")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun readSongsArguments(): ArrayList<MusicSongItem> {
        val serializableSongs = arguments?.getSerializable(ARG_SONGS) as? ArrayList<*>
        return ArrayList(
            serializableSongs
                ?.filterIsInstance<MusicSongItem>()
                .orEmpty()
        )
    }
}
