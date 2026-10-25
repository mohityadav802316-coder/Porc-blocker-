package com.example.music

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.db.LifeTrackerDatabase
import com.example.data.model.MusicMood
import com.example.data.model.PlaylistEntity
import com.example.data.model.SongEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class RepeatMode {
  OFF, ALL, ONE
}

data class MusicPlayerState(
  val currentSong: SongEntity? = null,
  val isPlaying: Boolean = false,
  val currentPositionMs: Long = 0L,
  val durationMs: Long = 0L,
  val queue: List<SongEntity> = emptyList(),
  val currentIndex: Int = 0,
  val isShuffle: Boolean = false,
  val repeatMode: RepeatMode = RepeatMode.ALL,
  val playbackSpeed: Float = 1.0f,
  val currentPlaylist: PlaylistEntity? = null,
  val currentSituation: String? = null,
  val volume: Float = 1.0f
) {
  val currentMood: MusicMood
    get() = currentSong?.let { MusicMood.fromString(it.mood) } ?: MusicMood.CALM

  val progressFraction: Float
    get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}

class MusicPlayerManager private constructor(private val appContext: Context) {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
  private val _playerState = MutableStateFlow(MusicPlayerState())
  val playerState: StateFlow<MusicPlayerState> = _playerState.asStateFlow()

  private val prefs: SharedPreferences =
    appContext.getSharedPreferences("music_playback_saved_state", Context.MODE_PRIVATE)

  val exoPlayer: ExoPlayer by lazy {
    val audioAttributes = AudioAttributes.Builder()
      .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
      .setUsage(C.USAGE_MEDIA)
      .build()

    ExoPlayer.Builder(appContext)
      .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
      .setHandleAudioBecomingNoisy(true)
      .setWakeMode(C.WAKE_MODE_LOCAL)
      .build().apply {
        repeatMode = Player.REPEAT_MODE_ALL
        shuffleModeEnabled = false
        addListener(createPlayerListener())
      }
  }

  private var progressTrackerJob: Job? = null
  private val db = LifeTrackerDatabase.getDatabase(appContext)
  private val equalizerManager = EqualizerManager.getInstance(appContext)
  private val sleepTimerManager = SleepTimerManager.getInstance(appContext)

  init {
    setupSleepTimerCallbacks()
    restoreSavedState()
  }

  private fun createPlayerListener(): Player.Listener {
    return object : Player.Listener {
      override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
          Player.STATE_READY -> {
            val dur = exoPlayer.duration.coerceAtLeast(0L)
            _playerState.update { it.copy(durationMs = dur) }
            savePlaybackState()
          }
          Player.STATE_ENDED -> {
            sleepTimerManager.onTrackCompleted()
            _playerState.update { it.copy(isPlaying = false) }
          }
          else -> {}
        }
      }

      override fun onIsPlayingChanged(isPlaying: Boolean) {
        _playerState.update { it.copy(isPlaying = isPlaying) }
        if (isPlaying) {
          startProgressTracker()
          startPlaybackService()
        } else {
          progressTrackerJob?.cancel()
        }
        savePlaybackState()
      }

      override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        sleepTimerManager.onTrackCompleted()
        val index = exoPlayer.currentMediaItemIndex
        val currentQueue = _playerState.value.queue

        if (index in currentQueue.indices) {
          val song = currentQueue[index]
          _playerState.update {
            it.copy(
              currentSong = song,
              currentIndex = index,
              durationMs = song.durationMs
            )
          }

          // Record in play history
          scope.launch(Dispatchers.IO) {
            db.musicDao().recordSongPlayed(song.id, System.currentTimeMillis())
          }
        }
        savePlaybackState()
      }

      @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
      override fun onAudioSessionIdChanged(audioSessionId: Int) {
        if (audioSessionId > 0) {
          equalizerManager.attachAudioSession(audioSessionId)
        }
      }
    }
  }

  private fun setupSleepTimerCallbacks() {
    sleepTimerManager.onFadeVolume = { vol ->
      exoPlayer.volume = vol
      _playerState.update { it.copy(volume = vol) }
    }
    sleepTimerManager.onTimerExpired = {
      pause()
    }
  }

  fun playQueue(
    songs: List<SongEntity>,
    startIndex: Int = 0,
    playlist: PlaylistEntity? = null,
    situation: String? = null
  ) {
    if (songs.isEmpty()) return
    val safeIndex = startIndex.coerceIn(0, songs.size - 1)

    _playerState.update {
      it.copy(
        queue = songs,
        currentIndex = safeIndex,
        currentSong = songs[safeIndex],
        currentPlaylist = playlist,
        currentSituation = situation,
        durationMs = songs[safeIndex].durationMs,
        currentPositionMs = 0L
      )
    }

    val mediaItems = songs.map { toMediaItem(it) }
    exoPlayer.setMediaItems(mediaItems, safeIndex, 0L)
    exoPlayer.prepare()
    exoPlayer.play()

    startPlaybackService()
    startProgressTracker()
  }

  fun playSong(song: SongEntity) {
    val currentQueue = _playerState.value.queue.toMutableList()
    val existingIndex = currentQueue.indexOfFirst { it.id == song.id }

    if (existingIndex >= 0) {
      _playerState.update { it.copy(currentIndex = existingIndex, currentSong = song) }
      exoPlayer.seekTo(existingIndex, 0L)
      exoPlayer.play()
    } else {
      // Create new single-song queue or append
      val newQueue = listOf(song)
      playQueue(newQueue, 0)
    }
    startPlaybackService()
  }

  fun playNext(song: SongEntity) {
    val state = _playerState.value
    if (state.queue.isEmpty()) {
      playQueue(listOf(song), 0)
      return
    }

    val currentQueue = state.queue.toMutableList()
    val insertIndex = (state.currentIndex + 1).coerceAtMost(currentQueue.size)
    currentQueue.add(insertIndex, song)

    _playerState.update { it.copy(queue = currentQueue) }
    exoPlayer.addMediaItem(insertIndex, toMediaItem(song))
    savePlaybackState()
  }

  fun addToQueue(song: SongEntity) {
    val state = _playerState.value
    if (state.queue.isEmpty()) {
      playQueue(listOf(song), 0)
      return
    }

    val currentQueue = state.queue.toMutableList()
    currentQueue.add(song)

    _playerState.update { it.copy(queue = currentQueue) }
    exoPlayer.addMediaItem(toMediaItem(song))
    savePlaybackState()
  }

  fun moveQueueItem(fromIndex: Int, toIndex: Int) {
    val currentQueue = _playerState.value.queue.toMutableList()
    if (fromIndex !in currentQueue.indices || toIndex !in currentQueue.indices || fromIndex == toIndex) return

    val item = currentQueue.removeAt(fromIndex)
    currentQueue.add(toIndex, item)

    val newIndex = when {
      _playerState.value.currentIndex == fromIndex -> toIndex
      fromIndex < _playerState.value.currentIndex && toIndex >= _playerState.value.currentIndex -> _playerState.value.currentIndex - 1
      fromIndex > _playerState.value.currentIndex && toIndex <= _playerState.value.currentIndex -> _playerState.value.currentIndex + 1
      else -> _playerState.value.currentIndex
    }

    _playerState.update { it.copy(queue = currentQueue, currentIndex = newIndex) }
    exoPlayer.moveMediaItem(fromIndex, toIndex)
    savePlaybackState()
  }

  fun removeFromQueue(index: Int) {
    val currentQueue = _playerState.value.queue.toMutableList()
    if (index !in currentQueue.indices) return

    if (currentQueue.size <= 1) {
      exoPlayer.stop()
      exoPlayer.clearMediaItems()
      _playerState.update {
        MusicPlayerState()
      }
      savePlaybackState()
      return
    }

    currentQueue.removeAt(index)
    exoPlayer.removeMediaItem(index)

    val newCurrentIndex = if (index < _playerState.value.currentIndex) {
      _playerState.value.currentIndex - 1
    } else {
      _playerState.value.currentIndex.coerceAtMost(currentQueue.size - 1)
    }

    val newCurrentSong = currentQueue.getOrNull(newCurrentIndex)
    _playerState.update {
      it.copy(
        queue = currentQueue,
        currentIndex = newCurrentIndex,
        currentSong = newCurrentSong
      )
    }
    savePlaybackState()
  }

  fun clearQueue() {
    exoPlayer.stop()
    exoPlayer.clearMediaItems()
    _playerState.update { MusicPlayerState() }
    savePlaybackState()
  }

  fun togglePlayPause() {
    if (exoPlayer.isPlaying) {
      pause()
    } else {
      if (exoPlayer.playbackState == Player.STATE_IDLE || exoPlayer.mediaItemCount == 0) {
        val q = _playerState.value.queue
        if (q.isNotEmpty()) {
          val items = q.map { toMediaItem(it) }
          val idx = _playerState.value.currentIndex.coerceIn(0, q.size - 1)
          val pos = _playerState.value.currentPositionMs
          exoPlayer.setMediaItems(items, idx, pos)
          exoPlayer.prepare()
        }
      }
      resume()
    }
  }

  fun pause() {
    exoPlayer.pause()
    _playerState.update { it.copy(isPlaying = false) }
    savePlaybackState()
  }

  fun resume() {
    exoPlayer.play()
    _playerState.update { it.copy(isPlaying = true) }
    startProgressTracker()
    startPlaybackService()
    savePlaybackState()
  }

  fun next() {
    if (exoPlayer.hasNextMediaItem()) {
      exoPlayer.seekToNextMediaItem()
      exoPlayer.play()
    } else {
      // Loop to beginning if repeat mode is ALL
      if (_playerState.value.repeatMode == RepeatMode.ALL && exoPlayer.mediaItemCount > 0) {
        exoPlayer.seekTo(0, 0L)
        exoPlayer.play()
      }
    }
  }

  fun previous() {
    if (exoPlayer.currentPosition > 3000L) {
      exoPlayer.seekTo(0L)
    } else if (exoPlayer.hasPreviousMediaItem()) {
      exoPlayer.seekToPreviousMediaItem()
      exoPlayer.play()
    } else {
      // Loop to end if repeat mode is ALL
      if (_playerState.value.repeatMode == RepeatMode.ALL && exoPlayer.mediaItemCount > 0) {
        val lastIdx = exoPlayer.mediaItemCount - 1
        exoPlayer.seekTo(lastIdx, 0L)
        exoPlayer.play()
      }
    }
  }

  fun seekTo(positionMs: Long) {
    _playerState.update { it.copy(currentPositionMs = positionMs) }
    exoPlayer.seekTo(positionMs)
    savePlaybackState()
  }

  fun setPlaybackSpeed(speed: Float) {
    val clamped = speed.coerceIn(0.5f, 2.0f)
    exoPlayer.playbackParameters = PlaybackParameters(clamped)
    _playerState.update { it.copy(playbackSpeed = clamped) }
    prefs.edit().putFloat(KEY_SPEED, clamped).apply()
  }

  fun toggleShuffle() {
    val nextShuffle = !_playerState.value.isShuffle
    exoPlayer.shuffleModeEnabled = nextShuffle
    _playerState.update { it.copy(isShuffle = nextShuffle) }
    prefs.edit().putBoolean(KEY_SHUFFLE, nextShuffle).apply()
  }

  fun toggleRepeatMode() {
    val nextMode = when (_playerState.value.repeatMode) {
      RepeatMode.OFF -> RepeatMode.ALL
      RepeatMode.ALL -> RepeatMode.ONE
      RepeatMode.ONE -> RepeatMode.OFF
    }

    exoPlayer.repeatMode = when (nextMode) {
      RepeatMode.OFF -> Player.REPEAT_MODE_OFF
      RepeatMode.ALL -> Player.REPEAT_MODE_ALL
      RepeatMode.ONE -> Player.REPEAT_MODE_ONE
    }

    _playerState.update { it.copy(repeatMode = nextMode) }
    prefs.edit().putString(KEY_REPEAT, nextMode.name).apply()
  }

  fun toggleFavorite(song: SongEntity) {
    scope.launch(Dispatchers.IO) {
      val newFav = !song.isFavorite
      db.musicDao().updateFavoriteStatus(song.id, newFav)
      _playerState.update { state ->
        val updatedQueue = state.queue.map {
          if (it.id == song.id) it.copy(isFavorite = newFav) else it
        }
        val updatedCurrent = if (state.currentSong?.id == song.id) {
          state.currentSong.copy(isFavorite = newFav)
        } else state.currentSong
        state.copy(queue = updatedQueue, currentSong = updatedCurrent)
      }
    }
  }

  fun setSleepTimer(minutes: Int) {
    sleepTimerManager.startTimer(minutes)
  }

  fun playByCurrentTimeRule(currentMinutes: Int) {
    scope.launch(Dispatchers.IO) {
      val rules = db.musicDao().getRulesMatchingTime(currentMinutes)
      val rule = rules.firstOrNull() ?: return@launch
      val playlist = db.musicDao().getPlaylistById(rule.playlistId)
      val songs = db.musicDao().getSongsForPlaylistSync(rule.playlistId)
      if (songs.isNotEmpty()) {
        withContext(Dispatchers.Main) {
          playQueue(songs, 0, playlist, rule.situation)
        }
      }
    }
  }

  private fun startProgressTracker() {
    progressTrackerJob?.cancel()
    progressTrackerJob = scope.launch {
      while (isActive && exoPlayer.isPlaying) {
        val pos = exoPlayer.currentPosition
        val dur = exoPlayer.duration.coerceAtLeast(0L)
        _playerState.update { it.copy(currentPositionMs = pos, durationMs = dur) }
        delay(500L)
      }
    }
  }

  private fun startPlaybackService() {
    try {
      val intent = Intent(appContext, MusicPlaybackService::class.java).apply {
        action = MusicPlaybackService.ACTION_START
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        appContext.startForegroundService(intent)
      } else {
        appContext.startService(intent)
      }
    } catch (_: Exception) {}
  }

  private fun toMediaItem(song: SongEntity): MediaItem {
    val metadata = MediaMetadata.Builder()
      .setTitle(song.title)
      .setArtist(song.artist)
      .setAlbumTitle(song.album)
      .setDisplayTitle(song.title)
      .setArtworkUri(if (!song.albumArtUri.isNullOrBlank()) Uri.parse(song.albumArtUri) else null)
      .build()

    return MediaItem.Builder()
      .setMediaId(song.id)
      .setUri(Uri.parse(song.contentUri))
      .setMediaMetadata(metadata)
      .build()
  }

  private fun savePlaybackState() {
    try {
      val state = _playerState.value
      val queueIds = state.queue.joinToString(",") { it.id }
      prefs.edit()
        .putString(KEY_SAVED_QUEUE, queueIds)
        .putString(KEY_CURRENT_SONG_ID, state.currentSong?.id ?: "")
        .putInt(KEY_CURRENT_INDEX, state.currentIndex)
        .putLong(KEY_POSITION_MS, exoPlayer.currentPosition)
        .apply()
    } catch (_: Exception) {}
  }

  private fun restoreSavedState() {
    scope.launch(Dispatchers.IO) {
      try {
        val queueRaw = prefs.getString(KEY_SAVED_QUEUE, null)
        val lastSongId = prefs.getString(KEY_CURRENT_SONG_ID, null)
        val savedIndex = prefs.getInt(KEY_CURRENT_INDEX, 0)
        val savedPos = prefs.getLong(KEY_POSITION_MS, 0L)
        val savedSpeed = prefs.getFloat(KEY_SPEED, 1.0f)
        val savedShuffle = prefs.getBoolean(KEY_SHUFFLE, false)
        val savedRepeatStr = prefs.getString(KEY_REPEAT, RepeatMode.ALL.name)
        val savedRepeat = try { RepeatMode.valueOf(savedRepeatStr ?: RepeatMode.ALL.name) } catch (_: Exception) { RepeatMode.ALL }

        if (!queueRaw.isNullOrBlank()) {
          val ids = queueRaw.split(",").filter { it.isNotBlank() }
          val songsList = mutableListOf<SongEntity>()
          for (id in ids) {
            val s = db.musicDao().getSongById(id)
            if (s != null) songsList.add(s)
          }

          if (songsList.isNotEmpty()) {
            val safeIdx = savedIndex.coerceIn(0, songsList.size - 1)
            val currentSong = songsList[safeIdx]

            withContext(Dispatchers.Main) {
              _playerState.update {
                it.copy(
                  queue = songsList,
                  currentIndex = safeIdx,
                  currentSong = currentSong,
                  currentPositionMs = savedPos,
                  durationMs = currentSong.durationMs,
                  isShuffle = savedShuffle,
                  repeatMode = savedRepeat,
                  playbackSpeed = savedSpeed
                )
              }

              // Prepare ExoPlayer with saved state without auto-playing
              val items = songsList.map { toMediaItem(it) }
              exoPlayer.setMediaItems(items, safeIdx, savedPos)
              exoPlayer.shuffleModeEnabled = savedShuffle
              exoPlayer.repeatMode = when (savedRepeat) {
                RepeatMode.OFF -> Player.REPEAT_MODE_OFF
                RepeatMode.ALL -> Player.REPEAT_MODE_ALL
                RepeatMode.ONE -> Player.REPEAT_MODE_ONE
              }
              exoPlayer.playbackParameters = PlaybackParameters(savedSpeed)
              exoPlayer.prepare()
            }
          }
        }
      } catch (_: Exception) {}
    }
  }

  companion object {
    private const val KEY_SAVED_QUEUE = "saved_queue_ids"
    private const val KEY_CURRENT_SONG_ID = "current_song_id"
    private const val KEY_CURRENT_INDEX = "current_index"
    private const val KEY_POSITION_MS = "position_ms"
    private const val KEY_SPEED = "playback_speed"
    private const val KEY_SHUFFLE = "is_shuffle"
    private const val KEY_REPEAT = "repeat_mode"

    @Volatile
    private var instance: MusicPlayerManager? = null

    fun getInstance(context: Context): MusicPlayerManager {
      return instance ?: synchronized(this) {
        instance ?: MusicPlayerManager(context.applicationContext).also { instance = it }
      }
    }
  }
}
