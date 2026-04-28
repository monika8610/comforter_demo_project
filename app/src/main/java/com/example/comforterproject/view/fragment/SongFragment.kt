package com.example.comforterproject.view.fragment

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.comforterproject.R
import com.example.comforterproject.model.MusicSongItem
import com.example.comforterproject.repository.MusicRepository
import com.example.comforterproject.service.AudioPlaybackService
import kotlinx.coroutines.launch
class SongFragment : Fragment() {
    companion object {
        private const val ARG_ALBUM_ID = "arg_album_id"
        private const val ARG_ALBUM_NAME = "arg_album_name"
        private const val ARG_IMAGE_URL = "arg_image_url"
        private const val ARG_INITIAL_SONG_INDEX = "arg_initial_song_index"
        private const val ARG_INITIAL_SONG_ID = "arg_initial_song_id"
        private const val ARG_INITIAL_SONG_FILE = "arg_initial_song_file"
        private const val ARG_SONGS = "arg_songs"
        private const val ARG_AUDIO_BASE_URL = "arg_audio_base_url"
        private const val TAG = "SongFragment"
        fun newInstance(
            albumId: String,
            albumName: String,
            imageUrl: String?,
            initialSongIndex: Int,
            initialSongId: String?,
            initialSongFile: String?,
            songs: ArrayList<MusicSongItem>,
            audioBaseUrl: String?
        ): SongFragment {
            return SongFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ALBUM_ID, albumId)
                    putString(ARG_ALBUM_NAME, albumName)
                    putString(ARG_IMAGE_URL, imageUrl)
                    putInt(ARG_INITIAL_SONG_INDEX, initialSongIndex)
                    putString(ARG_INITIAL_SONG_ID, initialSongId)
                    putString(ARG_INITIAL_SONG_FILE, initialSongFile)
                    putSerializable(ARG_SONGS, songs)
                    putString(ARG_AUDIO_BASE_URL, audioBaseUrl)
                }
            }
        }
    }
    private val musicRepository = MusicRepository()
    private val progressHandler = Handler(Looper.getMainLooper())
    private var songs: List<MusicSongItem> = emptyList()
    private var audioBaseUrl: String? = null
    private var currentSongIndex: Int = 0
    private var playbackService: AudioPlaybackService? = null
    private var isServiceBound = false
    private var albumImageView: ImageView? = null
    private var albumTitleView: TextView? = null
    private var songTitleView: TextView? = null
    private var currentTimeView: TextView? = null
    private var totalTimeView: TextView? = null
    private var seekBar: SeekBar? = null
    private var playPauseButton: ImageButton? = null
    private var previousButton: ImageButton? = null
    private var nextButton: ImageButton? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as? AudioPlaybackService.LocalBinder ?: return
            playbackService = localBinder.getService()

            playbackService?.setOnCompletionListener {
                activity?.runOnUiThread {
                    val nextPlayableIndex = findPlayableSongIndex(
                        startIndex = currentSongIndex + 1,
                        step = 1
                    )

                    if (nextPlayableIndex != null) {
                        startSongAt(nextPlayableIndex)
                    } else {
                        updatePlayPauseIcon(false)
                    }
                }
            }
            isServiceBound = true
            syncUiFromService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playbackService = null
            isServiceBound = false
        }
    }

    private val progressRunnable = object : Runnable {
        override fun run() {
            val service = playbackService
            if (service != null && service.isPrepared()) {
                val current = service.getCurrentPosition()
                val duration = service.getDuration()
                seekBar?.max = duration
                seekBar?.progress = current
                currentTimeView?.text = formatTime(current)
                totalTimeView?.text = formatTime(duration)
                updatePlayPauseIcon(service.isPlaying())
            }
            progressHandler.postDelayed(this, 300)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_song, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }

        val albumName = arguments?.getString(ARG_ALBUM_NAME).orEmpty()
        val imageUrl = arguments?.getString(ARG_IMAGE_URL)
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
        Glide.with(this)
            .load(imageUrl)
            .into(albumImageView!!)
        loadSongs()

        playPauseButton?.setOnClickListener { togglePlayback() }
        previousButton?.setOnClickListener { playPreviousSong() }
        nextButton?.setOnClickListener { playNextSong() }
        seekBar = view.findViewById(R.id.songSeekBar)

        seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    playbackService?.let {
                        if (it.isPrepared()) {
                            it.seekTo(progress)
                        }
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    override fun onStart() {
        super.onStart()
        bindToPlaybackService()
    }


    override fun onStop() {
        progressHandler.removeCallbacks(progressRunnable)
        unbindFromPlaybackService()
        super.onStop()
    }

    private fun startSongAt(index: Int) {
        val song = songs.getOrNull(index) ?: return
        val baseUrl = audioBaseUrl
        val songFile = song.songsFile
        currentSongIndex = index
        songTitleView?.text = song.songsName

        if (songFile.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Song not available", Toast.LENGTH_SHORT).show()
            return
        }

        if (baseUrl.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Song not available", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Missing audio base URL for songFile=$songFile")
            return
        }

        val audioUrl = buildAudioUrl(baseUrl, songFile)
        if (audioUrl.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Invalid audio source", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Invalid audio URL. baseUrl=$baseUrl, songFile=$songFile")
            return
        }

        playFromService(
            audioUrl,
            song.songsName.orEmpty(),
            arguments?.getString(ARG_IMAGE_URL)
        )
    }

    private fun playFromService(audioUrl: String, title: String, imageUrl: String?) {
        val context = context ?: return
        Log.d(TAG, "Starting playback with URL: $audioUrl")

        val intent = Intent(context, AudioPlaybackService::class.java).apply {
            action = AudioPlaybackService.ACTION_PLAY
            putExtra(AudioPlaybackService.EXTRA_AUDIO_URL, audioUrl)
            putExtra(AudioPlaybackService.EXTRA_TITLE, title)
            putExtra(AudioPlaybackService.EXTRA_IMAGE_URL, imageUrl)
        }

        Handler(Looper.getMainLooper()).post {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        progressHandler.postDelayed(progressRunnable, 300)
    }

    private fun togglePlayback() {
        val context = context ?: return
        val intent = Intent(context, AudioPlaybackService::class.java).apply {
            action = AudioPlaybackService.ACTION_TOGGLE
        }
        context.startService(intent)

        Handler(Looper.getMainLooper()).postDelayed({
            updatePlayPauseIcon(playbackService?.isPlaying() == true)
        }, 200)
    }

    private fun playPreviousSong() {
        val previousPlayableIndex = findPlayableSongIndex(
            startIndex = currentSongIndex - 1,
            step = -1
        )
        println("##############---------------------previousPlayableIndex----${previousPlayableIndex}")
        if (previousPlayableIndex == null) {
            Toast.makeText(requireContext(), "Song not available", Toast.LENGTH_SHORT).show()
            return
        }

        startSongAt(previousPlayableIndex)
    }

    private fun playNextSong() {
        val nextPlayableIndex = findPlayableSongIndex(
            startIndex = currentSongIndex + 1,
            step = 1
        )
        println("##############---------------------nextPlayableIndex----${nextPlayableIndex}")

        if (nextPlayableIndex == null) {
            Toast.makeText(requireContext(), "Song not available", Toast.LENGTH_SHORT).show()
            return
        }

        startSongAt(nextPlayableIndex)
    }

    private fun updatePlayPauseIcon(isPlaying: Boolean) {
        playPauseButton?.setImageResource(
            if (isPlaying) android.R.drawable.ic_media_pause
            else android.R.drawable.ic_media_play
        )
    }

    private fun bindToPlaybackService() {
        val context = context ?: return
        val intent = Intent(context, AudioPlaybackService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun unbindFromPlaybackService() {
        val context = context ?: return
        if (isServiceBound) {
            context.unbindService(serviceConnection)
            isServiceBound = false
        }
    }

    private fun syncUiFromService() {
        val service = playbackService ?: return
        updatePlayPauseIcon(service.isPlaying())
        progressHandler.post(progressRunnable)
    }

    private fun loadSongs() {
        val albumId = arguments?.getString(ARG_ALBUM_ID).orEmpty()
        val initialSongIndex = arguments?.getInt(ARG_INITIAL_SONG_INDEX, -1) ?: -1
        val initialSongFile = arguments?.getString(ARG_INITIAL_SONG_FILE)
        val argumentSongs = readSongsArguments()
        val argumentAudioBaseUrl = arguments?.getString(ARG_AUDIO_BASE_URL)

        if (argumentSongs.isNotEmpty() && !argumentAudioBaseUrl.isNullOrBlank()) {
            songs = argumentSongs
            audioBaseUrl = argumentAudioBaseUrl
            playInitialSong(initialSongIndex, initialSongFile)
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                musicRepository.getSongs(albumId)
            }.onSuccess { response ->
                songs = response.body()?.data.orEmpty()
                audioBaseUrl = response.body()?.file_path
                playInitialSong(initialSongIndex, initialSongFile)
            }.onFailure {
                Toast.makeText(requireContext(), "Error loading songs", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun playInitialSong(initialSongIndex: Int, initialSongFile: String?) {
        val initialIndex = resolveInitialSongIndex(initialSongIndex, initialSongFile)
        if (initialIndex == null) {
            songTitleView?.text = "Song not available"
            Toast.makeText(requireContext(), "Song not available", Toast.LENGTH_SHORT).show()
            return
        }

        startSongAt(initialIndex)
    }

    private fun resolveInitialSongIndex(initialSongIndex: Int, initialSongFile: String?): Int? {
        if (songs.isEmpty()) return null

        if (initialSongIndex in songs.indices && !songs[initialSongIndex].songsFile.isNullOrBlank()) {
            return initialSongIndex
        }

        val normalizedInitialSongFile = initialSongFile
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        val requestedIndexByFile = normalizedInitialSongFile?.let { requestedFile ->
            songs.indexOfFirst { song ->
                song.songsFile?.trim().equals(requestedFile, ignoreCase = true)
            }.takeIf { it >= 0 }
        }

        if (requestedIndexByFile != null) {
            return requestedIndexByFile
        }

        val requestedSongId = arguments?.getString(ARG_INITIAL_SONG_ID)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        val requestedIndexById = requestedSongId?.let { songId ->
            songs.indexOfFirst { song ->
                song.id?.trim().equals(songId, ignoreCase = true) && !song.songsFile.isNullOrBlank()
            }.takeIf { it >= 0 }
        }

        return requestedIndexById ?: findPlayableSongIndex(startIndex = 0, step = 1)
    }

    private fun findPlayableSongIndex(startIndex: Int, step: Int): Int? {
        if (songs.isEmpty() || step == 0) return null

        var index = startIndex
        while (index in songs.indices) {
            val song = songs[index]
            if (!song.songsFile.isNullOrBlank()) {
                return index
            }
            index += step
        }

        return null
    }

    @Suppress("UNCHECKED_CAST")
    private fun readSongsArguments(): List<MusicSongItem> {
        val serializableSongs = arguments?.getSerializable(ARG_SONGS) as? ArrayList<*>
        return serializableSongs
            ?.filterIsInstance<MusicSongItem>()
            .orEmpty()
    }

    private fun formatTime(ms: Int): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format("%d:%02d", min, sec)
    }

    private fun buildAudioUrl(baseUrl: String, songFile: String): String? {
        val normalizedFile = normalizeNetworkPath(songFile.trim())
        if (normalizedFile.isEmpty()) return null

        val directUri = Uri.parse(normalizedFile)
        if (!directUri.scheme.isNullOrBlank()) {
            return normalizedFile
        }

        val normalizedBase = normalizeNetworkPath(baseUrl.trim()).trimEnd('/')
        if (normalizedBase.isEmpty()) return null

        return try {
            val baseUri = Uri.parse(normalizedBase)
            val combinedPath = buildString {
                append(baseUri.encodedPath?.trimEnd('/').orEmpty())
                append('/')
                append(normalizedFile.trimStart('/'))
            }

            baseUri.buildUpon()
                .encodedPath(combinedPath)
                .build()
                .toString()
        } catch (error: Exception) {
            Log.e(TAG, "Failed to build audio URL from baseUrl=$baseUrl and songFile=$songFile", error)
            null
        }
    }

    private fun normalizeNetworkPath(value: String): String {
        return if (value.startsWith("//")) "https:$value" else value
    }
}
