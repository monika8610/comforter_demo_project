package com.example.comforterproject.view.fragment

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.comforterproject.R
import com.example.comforterproject.model.MusicSongItem
import com.example.comforterproject.repository.MusicRepository
import kotlinx.coroutines.launch

class SongFragment : Fragment() {

    companion object {
        private const val ARG_ALBUM_ID = "arg_album_id"
        private const val ARG_ALBUM_NAME = "arg_album_name"
        private const val ARG_IMAGE_URL = "arg_image_url"
        private const val ARG_INITIAL_SONG_FILE = "arg_initial_song_file"
        private const val TAG = "SongFragment"

        fun newInstance(
            albumId: String,
            albumName: String,
            imageUrl: String?,
            initialSongFile: String?
        ): SongFragment {
            return SongFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ALBUM_ID, albumId)
                    putString(ARG_ALBUM_NAME, albumName)
                    putString(ARG_IMAGE_URL, imageUrl)
                    putString(ARG_INITIAL_SONG_FILE, initialSongFile)
                }
            }
        }
    }

    private val musicRepository = MusicRepository()
    private val progressHandler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private var songs: List<MusicSongItem> = emptyList()
    private var audioBaseUrl: String? = null
    private var currentSongIndex: Int = 0
    private var isPrepared = false

    private var albumImageView: ImageView? = null
    private var albumTitleView: TextView? = null
    private var songTitleView: TextView? = null
    private var currentTimeView: TextView? = null
    private var totalTimeView: TextView? = null
    private var seekBar: SeekBar? = null
    private var playPauseButton: ImageButton? = null
    private var previousButton: ImageButton? = null
    private var nextButton: ImageButton? = null

    private val progressRunnable = object : Runnable {
        override fun run() {
            val player = mediaPlayer ?: return
            if (isPrepared) {
                seekBar?.progress = player.currentPosition
                currentTimeView?.text = formatTime(player.currentPosition)
                totalTimeView?.text = formatTime(player.duration)
                progressHandler.postDelayed(this, 500)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_song, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val albumName = arguments?.getString(ARG_ALBUM_NAME).orEmpty()
        val imageUrl = arguments?.getString(ARG_IMAGE_URL)

        val backButton = view.findViewById<ImageButton>(R.id.songBackButton)
        albumImageView = view.findViewById(R.id.songAlbumImage)
        albumTitleView = view.findViewById(R.id.songAlbumTitle)
        songTitleView = view.findViewById(R.id.songTrackTitle)
        currentTimeView = view.findViewById(R.id.songCurrentTime)
        totalTimeView = view.findViewById(R.id.songTotalTime)
        seekBar = view.findViewById(R.id.songSeekBar)
        playPauseButton = view.findViewById(R.id.songPlayPauseButton)
        previousButton = view.findViewById(R.id.songPreviousButton)
        nextButton = view.findViewById(R.id.songNextButton)

        albumTitleView?.text = albumName
        songTitleView?.text = "Loading..."
        currentTimeView?.text = "0:00"
        totalTimeView?.text = "0:00"

        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.home_banner_placeholder)
            .error(R.drawable.home_banner_placeholder)
            .into(albumImageView!!)

        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        playPauseButton?.setOnClickListener {
            togglePlayback()
        }

        previousButton?.setOnClickListener {
            playPreviousSong()
        }

        nextButton?.setOnClickListener {
            playNextSong()
        }

        seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    currentTimeView?.text = formatTime(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (isPrepared) {
                    mediaPlayer?.seekTo(seekBar?.progress ?: 0)
                }
            }
        })

        loadSongs()
    }

    override fun onStop() {
        super.onStop()
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
            updatePlayPauseIcon()
        }
    }

    override fun onDestroyView() {
        progressHandler.removeCallbacks(progressRunnable)
        releasePlayer()
        albumImageView = null
        albumTitleView = null
        songTitleView = null
        currentTimeView = null
        totalTimeView = null
        seekBar = null
        playPauseButton = null
        previousButton = null
        nextButton = null
        super.onDestroyView()
    }

    private fun loadSongs() {
        val albumId = arguments?.getString(ARG_ALBUM_ID).orEmpty()
        val initialSongFile = arguments?.getString(ARG_INITIAL_SONG_FILE)

        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                musicRepository.getSongs(albumId)
            }.onSuccess { response ->
                val body = response.body()
                songs = body?.data.orEmpty()
                audioBaseUrl = normalizeBaseUrl(body?.file_path)

                if (!response.isSuccessful || songs.isEmpty() || audioBaseUrl == null) {
                    showSongUnavailable()
                    return@onSuccess
                }

                currentSongIndex = songs.indexOfFirst { it.songsFile == initialSongFile }
                    .takeIf { it >= 0 } ?: 0

                startSongAt(currentSongIndex)
            }.onFailure {
                Log.e(TAG, "Unable to load song list", it)
                showSongUnavailable()
            }
        }
    }

    private fun startSongAt(index: Int) {
        val song = songs.getOrNull(index) ?: return
        val baseUrl = audioBaseUrl ?: return
        val fileName = song.songsFile?.trim().orEmpty()
        if (fileName.isBlank()) {
            showSongUnavailable()
            return
        }

        currentSongIndex = index
        songTitleView?.text = song.songsName
        currentTimeView?.text = "0:00"
        totalTimeView?.text = "0:00"
        seekBar?.progress = 0
        updateSkipButtons()
        prepareAndPlay("$baseUrl/${fileName.trimStart('/')}")
    }

    private fun prepareAndPlay(audioUrl: String) {
        progressHandler.removeCallbacks(progressRunnable)
        releasePlayer()
        isPrepared = false
        playPauseButton?.isEnabled = false

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setOnPreparedListener { player ->
                isPrepared = true
                seekBar?.max = player.duration
                totalTimeView?.text = formatTime(player.duration)
                playPauseButton?.isEnabled = true
                player.start()
                updatePlayPauseIcon()
                progressHandler.post(progressRunnable)
            }
            setOnCompletionListener {
                if (currentSongIndex < songs.lastIndex) {
                    startSongAt(currentSongIndex + 1)
                } else {
                    seekBar?.progress = seekBar?.max ?: 0
                    currentTimeView?.text = totalTimeView?.text
                    updatePlayPauseIcon(isPlaying = false)
                }
            }
            setOnErrorListener { _, _, _ ->
                showSongUnavailable()
                true
            }
        }

        runCatching {
            mediaPlayer?.setDataSource(audioUrl)
            mediaPlayer?.prepareAsync()
        }.onFailure {
            Log.e(TAG, "Unable to prepare audio", it)
            showSongUnavailable()
        }
    }

    private fun togglePlayback() {
        val player = mediaPlayer ?: return
        if (!isPrepared) return

        if (player.isPlaying) {
            player.pause()
            progressHandler.removeCallbacks(progressRunnable)
        } else {
            player.start()
            progressHandler.post(progressRunnable)
        }
        updatePlayPauseIcon()
    }

    private fun playPreviousSong() {
        if (currentSongIndex > 0) {
            startSongAt(currentSongIndex - 1)
        }
    }

    private fun playNextSong() {
        if (currentSongIndex < songs.lastIndex) {
            startSongAt(currentSongIndex + 1)
        }
    }

    private fun updatePlayPauseIcon(isPlaying: Boolean = mediaPlayer?.isPlaying == true) {
        playPauseButton?.setImageResource(
            if (isPlaying) android.R.drawable.ic_media_pause
            else android.R.drawable.ic_media_play
        )
    }

    private fun updateSkipButtons() {
        previousButton?.alpha = if (currentSongIndex > 0) 1f else 0.4f
        previousButton?.isEnabled = currentSongIndex > 0
        nextButton?.alpha = if (currentSongIndex < songs.lastIndex) 1f else 0.4f
        nextButton?.isEnabled = currentSongIndex < songs.lastIndex
    }

    private fun normalizeBaseUrl(path: String?): String? {
        if (path.isNullOrBlank()) return null
        return when {
            path.startsWith("//") -> "https:${path.trimEnd('/')}"
            path.startsWith("http://") || path.startsWith("https://") -> path.trimEnd('/')
            else -> "https://${path.trimEnd('/')}"
        }
    }

    private fun formatTime(milliseconds: Int): String {
        val totalSeconds = milliseconds / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    private fun showSongUnavailable() {
        if (!isAdded) return
        Toast.makeText(requireContext(), "Unable to play song", Toast.LENGTH_SHORT).show()
        songTitleView?.text = "Song not available"
        playPauseButton?.isEnabled = false
        updateSkipButtons()
        progressHandler.removeCallbacks(progressRunnable)
        releasePlayer()
    }

    private fun releasePlayer() {
        mediaPlayer?.runCatching {
            stop()
        }
        mediaPlayer?.release()
        mediaPlayer = null
        isPrepared = false
    }
}
