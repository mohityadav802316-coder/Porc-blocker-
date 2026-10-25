package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.PlaylistEntity
import com.example.data.model.SongEntity
import com.example.music.EqualizerBand
import com.example.music.EqualizerManager
import com.example.music.FolderBlacklistManager
import com.example.music.LrcLine
import com.example.music.LrcLyricsManager
import com.example.music.MusicPlayerManager
import com.example.music.MusicPlayerState
import com.example.music.RepeatMode
import com.example.music.SleepTimerManager
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.GoldBrass
import com.example.ui.theme.ObsidianCharcoal
import com.example.ui.theme.SageGreen
import com.example.ui.theme.StatusComplete
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletNeon
import com.example.util.TimeUtils

// -------------------------------------------------------------
// 1. MINI MUSIC PLAYER (DOCK AT BOTTOM)
// -------------------------------------------------------------
@Composable
fun MiniMusicPlayer(
  playerState: MusicPlayerState,
  onExpandNowPlaying: () -> Unit,
  onTogglePlayPause: () -> Unit,
  onNext: () -> Unit,
  onToggleFavorite: (SongEntity) -> Unit,
  modifier: Modifier = Modifier
) {
  val song = playerState.currentSong ?: return

  Box(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 6.dp)
      .clip(RoundedCornerShape(16.dp))
      .background(DarkSurfaceElevated)
      .border(1.dp, CyanNeon.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
      .clickable { onExpandNowPlaying() }
      .testTag("mini_music_player")
  ) {
    Column {
      // Progress Bar along top edge
      LinearProgressIndicator(
        progress = { playerState.progressFraction },
        modifier = Modifier
          .fillMaxWidth()
          .height(2.5.dp),
        color = CyanNeon,
        trackColor = DarkSurfaceBorder
      )

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Thumbnail Album Art
        Box(
          modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceBorder),
          contentAlignment = Alignment.Center
        ) {
          if (!song.albumArtUri.isNullOrBlank()) {
            AsyncImage(
              model = song.albumArtUri,
              contentDescription = null,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize()
            )
          } else {
            Icon(Icons.Default.MusicNote, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(24.dp))
          }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title & Artist
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = song.title,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "${song.artist} • ${song.album}",
            color = TextMuted,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        // Favorite Toggle
        IconButton(
          onClick = { onToggleFavorite(song) },
          modifier = Modifier.size(38.dp)
        ) {
          Icon(
            imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = "Favorite",
            tint = if (song.isFavorite) Color(0xFFEC4899) else TextMuted,
            modifier = Modifier.size(20.dp)
          )
        }

        // Play/Pause Button
        IconButton(
          onClick = onTogglePlayPause,
          modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(CyanNeon.copy(alpha = 0.15f))
        ) {
          Icon(
            imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (playerState.isPlaying) "Pause" else "Play",
            tint = CyanNeon,
            modifier = Modifier.size(26.dp)
          )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Next Button
        IconButton(
          onClick = onNext,
          modifier = Modifier.size(38.dp)
        ) {
          Icon(
            Icons.Default.SkipNext,
            contentDescription = "Next",
            tint = TextSecondary,
            modifier = Modifier.size(24.dp)
          )
        }
      }
    }
  }
}

// -------------------------------------------------------------
// 2. FULL NOW PLAYING SCREEN DIALOG
// -------------------------------------------------------------
@Composable
fun NowPlayingScreenDialog(
  playerState: MusicPlayerState,
  onDismiss: () -> Unit,
  onTogglePlayPause: () -> Unit,
  onNext: () -> Unit,
  onPrevious: () -> Unit,
  onSeekTo: (Long) -> Unit,
  onToggleShuffle: () -> Unit,
  onToggleRepeat: () -> Unit,
  onToggleFavorite: (SongEntity) -> Unit,
  onOpenEqualizer: () -> Unit,
  onOpenSleepTimer: () -> Unit,
  onOpenLyrics: () -> Unit,
  onOpenQueue: () -> Unit,
  onSelectSpeed: (Float) -> Unit
) {
  val song = playerState.currentSong ?: return
  var showSpeedMenu by remember { mutableStateOf(false) }

  var isUserSeeking by remember { mutableStateOf(false) }
  var seekPositionFraction by remember { mutableFloatStateOf(0f) }

  val effectiveFraction = if (isUserSeeking) seekPositionFraction else playerState.progressFraction
  val displayedPositionMs = if (isUserSeeking && playerState.durationMs > 0) {
    (seekPositionFraction * playerState.durationMs).toLong()
  } else {
    playerState.currentPositionMs
  }

  val sleepTimerManager = SleepTimerManager.getInstance(LocalContext.current)
  val sleepTimerState by sleepTimerManager.state.collectAsState()

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxSize()
        .background(DarkBackground),
      color = DarkBackground
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .statusBarsPadding()
          .navigationBarsPadding()
          .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        // --- Top Bar ---
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(40.dp)
          ) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Close", tint = TextPrimary, modifier = Modifier.size(30.dp))
          }

          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "NOW PLAYING",
              color = CyanNeon,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.5.sp
            )
            if (sleepTimerState.isActive) {
              Text(
                text = "⏳ ${sleepTimerState.formattedRemaining}",
                color = GoldBrass,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
              )
            }
          }

          IconButton(
            onClick = onOpenQueue,
            modifier = Modifier.size(40.dp)
          ) {
            Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Queue", tint = TextPrimary)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- Large Album Art Card ---
        Box(
          modifier = Modifier
            .size(280.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(28.dp)),
          contentAlignment = Alignment.Center
        ) {
          if (!song.albumArtUri.isNullOrBlank()) {
            AsyncImage(
              model = song.albumArtUri,
              contentDescription = null,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize()
            )
          } else {
            Icon(
              imageVector = Icons.Default.MusicNote,
              contentDescription = null,
              tint = CyanNeon.copy(alpha = 0.6f),
              modifier = Modifier.size(96.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Song Title & Artist ---
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = song.title,
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "${song.artist} • ${song.album}",
            color = TextMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- Seekbar & Timestamps ---
        Column(modifier = Modifier.fillMaxWidth()) {
          Slider(
            value = effectiveFraction,
            onValueChange = {
              isUserSeeking = true
              seekPositionFraction = it
            },
            onValueChangeFinished = {
              if (playerState.durationMs > 0) {
                val targetMs = (seekPositionFraction * playerState.durationMs).toLong()
                onSeekTo(targetMs)
              }
              isUserSeeking = false
            },
            colors = SliderDefaults.colors(
              thumbColor = CyanNeon,
              activeTrackColor = CyanNeon,
              inactiveTrackColor = DarkSurfaceBorder
            ),
            modifier = Modifier.fillMaxWidth().testTag("now_playing_seekbar")
          )

          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = TimeUtils.formatDuration(displayedPositionMs),
              color = TextMuted,
              fontSize = 12.sp
            )
            val remainingMs = (playerState.durationMs - displayedPositionMs).coerceAtLeast(0L)
            Text(
              text = "-${TimeUtils.formatDuration(remainingMs)}",
              color = TextMuted,
              fontSize = 12.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // --- Main Playback Controls ---
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Shuffle Button
          IconButton(
            onClick = onToggleShuffle,
            modifier = Modifier.size(46.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Shuffle,
              contentDescription = "Shuffle",
              tint = if (playerState.isShuffle) CyanNeon else TextMuted
            )
          }

          // Previous Button
          IconButton(
            onClick = onPrevious,
            modifier = Modifier.size(54.dp)
          ) {
            Icon(
              imageVector = Icons.Default.SkipPrevious,
              contentDescription = "Previous",
              tint = TextPrimary,
              modifier = Modifier.size(34.dp)
            )
          }

          // Big Play/Pause Button
          Box(
            modifier = Modifier
              .size(72.dp)
              .clip(CircleShape)
              .background(CyanNeon)
              .clickable { onTogglePlayPause() },
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
              contentDescription = if (playerState.isPlaying) "Pause" else "Play",
              tint = ObsidianCharcoal,
              modifier = Modifier.size(40.dp)
            )
          }

          // Next Button
          IconButton(
            onClick = onNext,
            modifier = Modifier.size(54.dp)
          ) {
            Icon(
              imageVector = Icons.Default.SkipNext,
              contentDescription = "Next",
              tint = TextPrimary,
              modifier = Modifier.size(34.dp)
            )
          }

          // Repeat Mode Button
          IconButton(
            onClick = onToggleRepeat,
            modifier = Modifier.size(46.dp)
          ) {
            val (icon, tint) = when (playerState.repeatMode) {
              RepeatMode.OFF -> Icons.Default.Repeat to TextMuted
              RepeatMode.ALL -> Icons.Default.Repeat to CyanNeon
              RepeatMode.ONE -> Icons.Default.RepeatOne to CyanNeon
            }
            Icon(imageVector = icon, contentDescription = "Repeat", tint = tint)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- Secondary Tools Bar ---
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceElevated)
            .padding(horizontal = 8.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceAround,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Favorite
          IconButton(onClick = { onToggleFavorite(song) }) {
            Icon(
              imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
              contentDescription = "Favorite",
              tint = if (song.isFavorite) Color(0xFFEC4899) else TextMuted,
              modifier = Modifier.size(22.dp)
            )
          }

          // Equalizer
          IconButton(onClick = onOpenEqualizer) {
            Icon(
              imageVector = Icons.Default.Equalizer,
              contentDescription = "Equalizer",
              tint = VioletNeon,
              modifier = Modifier.size(22.dp)
            )
          }

          // Sleep Timer
          IconButton(onClick = onOpenSleepTimer) {
            Icon(
              imageVector = Icons.Default.Timer,
              contentDescription = "Sleep Timer",
              tint = if (sleepTimerState.isActive) GoldBrass else TextMuted,
              modifier = Modifier.size(22.dp)
            )
          }

          // Lyrics (.LRC)
          IconButton(onClick = onOpenLyrics) {
            Icon(
              imageVector = Icons.Default.Lyrics,
              contentDescription = "Lyrics",
              tint = SageGreen,
              modifier = Modifier.size(22.dp)
            )
          }

          // Playback Speed
          Box {
            TextButton(onClick = { showSpeedMenu = true }) {
              Text(
                text = "${playerState.playbackSpeed}x",
                color = CyanNeon,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
              )
            }

            androidx.compose.material3.DropdownMenu(
              expanded = showSpeedMenu,
              onDismissRequest = { showSpeedMenu = false },
              modifier = Modifier.background(DarkSurfaceElevated)
            ) {
              listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                androidx.compose.material3.DropdownMenuItem(
                  text = {
                    Text(
                      text = "${spd}x",
                      color = if (spd == playerState.playbackSpeed) CyanNeon else TextPrimary,
                      fontWeight = if (spd == playerState.playbackSpeed) FontWeight.Bold else FontWeight.Normal
                    )
                  },
                  onClick = {
                    onSelectSpeed(spd)
                    showSpeedMenu = false
                  }
                )
              }
            }
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// 3. HIGH-QUALITY EQUALIZER DIALOG
// -------------------------------------------------------------
@Composable
fun EqualizerDialog(
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val eqManager = remember { EqualizerManager.getInstance(context) }
  val eqState by eqManager.state.collectAsState()

  var showSavePresetDialog by remember { mutableStateOf(false) }
  var newPresetName by remember { mutableStateOf("") }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .fillMaxHeight(0.88f)
        .clip(RoundedCornerShape(24.dp))
        .background(ObsidianCharcoal)
        .border(1.dp, VioletNeon.copy(alpha = 0.45f), RoundedCornerShape(24.dp)),
      color = ObsidianCharcoal
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(20.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Top Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Equalizer, contentDescription = null, tint = VioletNeon, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("इक्वलाइज़र (10-Band EQ)", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
              checked = eqState.isEnabled,
              onCheckedChange = { eqManager.setEnabled(it) },
              colors = SwitchDefaults.colors(
                checkedThumbColor = VioletNeon,
                checkedTrackColor = VioletNeon.copy(alpha = 0.35f)
              )
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
              Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
            }
          }
        }

        // Presets Horizontal Row
        Text("प्रीसेट्स (Presets)", color = CyanNeon, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          eqState.availablePresets.forEach { preset ->
            val isSelected = eqState.currentPreset == preset
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (isSelected) VioletNeon else DarkSurfaceElevated)
                .border(1.dp, if (isSelected) VioletNeon else DarkSurfaceBorder, RoundedCornerShape(12.dp))
                .clickable(enabled = eqState.isEnabled) { eqManager.applyPreset(preset) }
                .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
              Text(
                text = preset,
                color = if (isSelected) ObsidianCharcoal else TextPrimary,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
              )
            }
          }

          // Save Custom Preset Button
          OutlinedButton(
            onClick = { showSavePresetDialog = true },
            enabled = eqState.isEnabled,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
          ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Save Preset", fontSize = 12.sp)
          }
        }

        // Sliders for Equalizer Bands
        if (eqState.bands.isNotEmpty()) {
          Text("फ़्रीक्वेंसी बैंड्स (Bands)", color = CyanNeon, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState())
              .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            eqState.bands.forEach { band ->
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(52.dp)
              ) {
                val dbVal = band.levelMb / 100
                Text(
                  text = if (dbVal > 0) "+$dbVal dB" else "$dbVal dB",
                  color = if (dbVal != 0) VioletNeon else TextMuted,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Medium
                )

                // Vertical slider simulated or interactive vertical slider
                Slider(
                  value = band.levelMb.toFloat(),
                  onValueChange = { newLvl ->
                    eqManager.setBandLevel(band.bandIndex, newLvl.toInt().toShort())
                  },
                  valueRange = band.minLevelMb.toFloat()..band.maxLevelMb.toFloat(),
                  enabled = eqState.isEnabled,
                  colors = SliderDefaults.colors(
                    thumbColor = VioletNeon,
                    activeTrackColor = VioletNeon,
                    inactiveTrackColor = DarkSurfaceBorder
                  ),
                  modifier = Modifier.fillMaxWidth()
                )

                Text(
                  text = band.formattedFreq,
                  color = TextPrimary,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }
          }
        }

        // Bass Boost, Virtualizer, Loudness Enhancer Sliders
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceElevated)
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          // Bass Boost
          Column {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("Bass Boost (बास बूस्ट)", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
              Text("${eqState.bassBoost / 10}%", color = GoldBrass, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
              value = eqState.bassBoost.toFloat(),
              onValueChange = { eqManager.setBassBoost(it.toInt()) },
              valueRange = 0f..1000f,
              enabled = eqState.isEnabled,
              colors = SliderDefaults.colors(thumbColor = GoldBrass, activeTrackColor = GoldBrass)
            )
          }

          // Virtualizer
          Column {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("Virtualizer (सराउंड साउंड)", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
              Text("${eqState.virtualizer / 10}%", color = CyanNeon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
              value = eqState.virtualizer.toFloat(),
              onValueChange = { eqManager.setVirtualizer(it.toInt()) },
              valueRange = 0f..1000f,
              enabled = eqState.isEnabled,
              colors = SliderDefaults.colors(thumbColor = CyanNeon, activeTrackColor = CyanNeon)
            )
          }

          // Loudness Boost
          Column {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("Loudness Boost (आवाज़ बूस्ट)", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
              Text("+${eqState.loudnessGain / 100} dB", color = VioletNeon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
              value = eqState.loudnessGain.toFloat(),
              onValueChange = { eqManager.setLoudnessGain(it.toInt()) },
              valueRange = 0f..1000f,
              enabled = eqState.isEnabled,
              colors = SliderDefaults.colors(thumbColor = VioletNeon, activeTrackColor = VioletNeon)
            )
          }

          // Volume Normalization Toggle
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text("Volume Normalization (ReplayGain)", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
              Text("सभी गानों की आवाज़ को एक समान स्तर पर रखें", color = TextMuted, fontSize = 11.sp)
            }
            Switch(
              checked = eqState.volumeNormalization,
              onCheckedChange = { eqManager.setVolumeNormalization(it) },
              colors = SwitchDefaults.colors(checkedThumbColor = SageGreen, checkedTrackColor = SageGreen.copy(alpha = 0.35f))
            )
          }
        }
      }
    }
  }

  if (showSavePresetDialog) {
    AlertDialog(
      onDismissRequest = { showSavePresetDialog = false },
      containerColor = DarkSurfaceElevated,
      shape = RoundedCornerShape(18.dp),
      title = { Text("कस्टम प्रीसेट सहेजें", color = TextPrimary) },
      text = {
        OutlinedTextField(
          value = newPresetName,
          onValueChange = { newPresetName = it },
          placeholder = { Text("प्रीसेट नाम (e.g. My Bass)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      },
      confirmButton = {
        Button(
          onClick = {
            if (newPresetName.isNotBlank()) {
              eqManager.saveCustomPreset(newPresetName.trim())
              newPresetName = ""
              showSavePresetDialog = false
              Toast.makeText(context, "प्रीसेट सहेजा गया!", Toast.LENGTH_SHORT).show()
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = VioletNeon, contentColor = ObsidianCharcoal)
        ) {
          Text("सहेजें")
        }
      },
      dismissButton = {
        TextButton(onClick = { showSavePresetDialog = false }) {
          Text("रद्द करें", color = TextMuted)
        }
      }
    )
  }
}

// -------------------------------------------------------------
// 4. SLEEP TIMER DIALOG
// -------------------------------------------------------------
@Composable
fun SleepTimerDialog(
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val sleepTimerManager = remember { SleepTimerManager.getInstance(context) }
  val state by sleepTimerManager.state.collectAsState()

  var customMinutesInput by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = DarkSurfaceElevated,
    shape = RoundedCornerShape(22.dp),
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Timer, contentDescription = null, tint = GoldBrass)
        Spacer(modifier = Modifier.width(8.dp))
        Text("स्लीप टाइमर (Sleep Timer)", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        if (state.isActive) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(GoldBrass.copy(alpha = 0.15f))
              .border(1.dp, GoldBrass.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
              .padding(12.dp)
          ) {
            Column {
              Text("सक्रिय टाइमर: ${state.formattedRemaining}", color = GoldBrass, fontSize = 15.sp, fontWeight = FontWeight.Bold)
              Text("समय पूरा होने के अंतिम 15 सेकंड में आवाज़ धीरे-धीरे कम होकर (fade-out) रुकेगी।", color = TextMuted, fontSize = 11.sp)
            }
          }
        }

        Text("अवधि चुनें:", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)

        val presetMinutes = listOf(5, 10, 15, 30, 45, 60)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          presetMinutes.take(3).forEach { min ->
            OutlinedButton(
              onClick = {
                sleepTimerManager.startTimer(min)
                Toast.makeText(context, "$min मिनट का स्लीप टाइमर शुरू हुआ!", Toast.LENGTH_SHORT).show()
                onDismiss()
              },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
            ) {
              Text("${min}m", fontSize = 12.sp)
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          presetMinutes.drop(3).forEach { min ->
            OutlinedButton(
              onClick = {
                sleepTimerManager.startTimer(min)
                Toast.makeText(context, "$min मिनट का स्लीप टाइमर शुरू हुआ!", Toast.LENGTH_SHORT).show()
                onDismiss()
              },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
            ) {
              Text("${min}m", fontSize = 12.sp)
            }
          }
        }

        // Option: End of current track
        Button(
          onClick = {
            sleepTimerManager.startStopAtEndOfTrack()
            Toast.makeText(context, "वर्तमान गाने के अंत में संगीत बंद होगा!", Toast.LENGTH_SHORT).show()
            onDismiss()
          },
          colors = ButtonDefaults.buttonColors(containerColor = CyanNeon.copy(alpha = 0.15f), contentColor = CyanNeon),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("वर्तमान गाने के अंत में बंद करें (End of Track)")
        }

        // Custom minutes input
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = customMinutesInput,
            onValueChange = { customMinutesInput = it.filter { ch -> ch.isDigit() }.take(3) },
            placeholder = { Text("कस्टम मिनट (e.g. 25)") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
          Button(
            onClick = {
              val mins = customMinutesInput.toIntOrNull()
              if (mins != null && mins > 0) {
                sleepTimerManager.startTimer(mins)
                Toast.makeText(context, "$mins मिनट का स्लीप टाइमर शुरू हुआ!", Toast.LENGTH_SHORT).show()
                onDismiss()
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = GoldBrass, contentColor = ObsidianCharcoal),
            shape = RoundedCornerShape(10.dp)
          ) {
            Text("शुरू करें")
          }
        }
      }
    },
    confirmButton = {
      if (state.isActive) {
        Button(
          onClick = {
            sleepTimerManager.cancel()
            Toast.makeText(context, "स्लीप टाइमर बंद कर दिया गया", Toast.LENGTH_SHORT).show()
            onDismiss()
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E), contentColor = Color.White)
        ) {
          Text("टाइमर बंद करें")
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("बंद करें", color = TextMuted)
      }
    }
  )
}

// -------------------------------------------------------------
// 5. SYNCED LRC LYRICS DIALOG
// -------------------------------------------------------------
@Composable
fun LrcLyricsDialog(
  song: SongEntity,
  currentPositionMs: Long,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  var lyricsLines by remember { mutableStateOf<List<LrcLine>?>(null) }
  var isLoading by remember { mutableStateOf(true) }

  LaunchedEffect(song.id) {
    isLoading = true
    val result = LrcLyricsManager.loadLyricsForSong(context, song.contentUri, song.title)
    lyricsLines = result
    isLoading = false
  }

  val activeIndex = remember(currentPositionMs, lyricsLines) {
    lyricsLines?.let { LrcLyricsManager.findCurrentLineIndex(it, currentPositionMs) } ?: -1
  }

  val listState = rememberLazyListState()

  LaunchedEffect(activeIndex) {
    if (activeIndex >= 0) {
      listState.animateScrollToItem((activeIndex - 2).coerceAtLeast(0))
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .fillMaxHeight(0.85f)
        .clip(RoundedCornerShape(24.dp))
        .background(ObsidianCharcoal)
        .border(1.dp, SageGreen.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
      color = ObsidianCharcoal
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(20.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text("सिंक्ड लिरिक्स (.LRC)", color = SageGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(song.title, color = TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("लिरिक्स खोजे जा रहे हैं...", color = TextMuted)
          }
        } else if (lyricsLines.isNullOrEmpty()) {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.padding(24.dp)
            ) {
              Icon(Icons.Default.Lyrics, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
              Text("कोई .lrc फ़ाइल नहीं मिली", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
              Text(
                text = "गाने के साथ उसी फ़ोल्डर में समान नाम वाली .lrc फ़ाइल रखें (जैसे: ${song.title}.lrc)। यह अपने आप टाइमिंग के साथ स्क्रॉल होगी।",
                color = TextMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
              )
            }
          }
        } else {
          LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 40.dp)
          ) {
            itemsIndexed(lyricsLines!!) { index, item ->
              val isCurrent = index == activeIndex
              Text(
                text = item.text,
                color = if (isCurrent) SageGreen else TextMuted.copy(alpha = 0.6f),
                fontSize = if (isCurrent) 19.sp else 15.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
              )
            }
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// 6. QUEUE BOTTOM SHEET DIALOG
// -------------------------------------------------------------
@Composable
fun QueueBottomSheetDialog(
  playerState: MusicPlayerState,
  onDismiss: () -> Unit,
  onSelectSong: (SongEntity, Int) -> Unit,
  onMoveItem: (Int, Int) -> Unit,
  onRemoveItem: (Int) -> Unit,
  onClearQueue: () -> Unit
) {
  val queue = playerState.queue

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .fillMaxHeight(0.85f)
        .clip(RoundedCornerShape(24.dp))
        .background(ObsidianCharcoal)
        .border(1.dp, CyanNeon.copy(alpha = 0.4f), RoundedCornerShape(24.dp)),
      color = ObsidianCharcoal
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(18.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text("वर्तमान कतार (Queue)", color = CyanNeon, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("${queue.size} गाने", color = TextMuted, fontSize = 12.sp)
          }

          Row {
            if (queue.isNotEmpty()) {
              TextButton(onClick = onClearQueue) {
                Text("Clear", color = Color(0xFFF43F5E), fontSize = 12.sp)
              }
            }
            IconButton(onClick = onDismiss) {
              Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (queue.isEmpty()) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("कतार खाली है", color = TextMuted)
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            itemsIndexed(queue) { index, song ->
              val isCurrent = index == playerState.currentIndex

              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .background(if (isCurrent) CyanNeon.copy(alpha = 0.12f) else DarkSurfaceElevated)
                  .border(1.dp, if (isCurrent) CyanNeon.copy(alpha = 0.5f) else DarkSurfaceBorder, RoundedCornerShape(12.dp))
                  .clickable { onSelectSong(song, index) }
                  .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "${index + 1}",
                  color = if (isCurrent) CyanNeon else TextMuted,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.width(26.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = song.title,
                    color = if (isCurrent) CyanNeon else TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    text = song.artist,
                    color = TextMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }

                // Reorder buttons (Move Up / Down)
                IconButton(
                  onClick = { if (index > 0) onMoveItem(index, index - 1) },
                  enabled = index > 0,
                  modifier = Modifier.size(28.dp)
                ) {
                  Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", tint = if (index > 0) TextSecondary else TextMuted.copy(alpha = 0.3f))
                }

                IconButton(
                  onClick = { if (index < queue.size - 1) onMoveItem(index, index + 1) },
                  enabled = index < queue.size - 1,
                  modifier = Modifier.size(28.dp)
                ) {
                  Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down", tint = if (index < queue.size - 1) TextSecondary else TextMuted.copy(alpha = 0.3f))
                }

                IconButton(
                  onClick = { onRemoveItem(index) },
                  modifier = Modifier.size(28.dp)
                ) {
                  Icon(Icons.Default.Delete, contentDescription = "Remove", tint = TextMuted, modifier = Modifier.size(16.dp))
                }
              }
            }
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// 7. CREATE MOOD PLAYLIST DIALOG
// -------------------------------------------------------------
@Composable
fun CreateMoodPlaylistDialog(
  onDismiss: () -> Unit,
  onCreate: (String, String, String) -> Unit
) {
  var moodName by remember { mutableStateOf("") }
  var selectedColorHex by remember { mutableStateOf("#FF5722") }
  var selectedIcon by remember { mutableStateOf("⚡") }

  val colorOptions = listOf(
    "#FF5722", // Deep Orange
    "#00F0FF", // Cyan Neon
    "#10B981", // Emerald
    "#EC4899", // Pink
    "#8B5CF6", // Violet
    "#F59E0B", // Amber
    "#6366F1", // Indigo
    "#14B8A6"  // Teal
  )

  val iconOptions = listOf("⚡", "🌙", "🎯", "🧘", "🚗", "🔥", "🎵", "🏖️", "💪", "☕")

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = DarkSurfaceElevated,
    shape = RoundedCornerShape(20.dp),
    title = {
      Text("नया मूड प्लेलिस्ट बनाएँ", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        OutlinedTextField(
          value = moodName,
          onValueChange = { moodName = it },
          placeholder = { Text("मूड का नाम (e.g. Gym, Late Night, Focus)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Text("रंग चुनें:", color = TextSecondary, fontSize = 13.sp)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          colorOptions.forEach { hex ->
            val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color.Cyan }
            val isSelected = selectedColorHex == hex
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color)
                .border(if (isSelected) 3.dp else 1.dp, if (isSelected) Color.White else Color.Transparent, CircleShape)
                .clickable { selectedColorHex = hex }
            )
          }
        }

        Text("आइकन चुनें:", color = TextSecondary, fontSize = 13.sp)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          iconOptions.forEach { icon ->
            val isSelected = selectedIcon == icon
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) DarkSurfaceBorder else Color.Transparent)
                .border(1.dp, if (isSelected) CyanNeon else Color.Transparent, RoundedCornerShape(10.dp))
                .clickable { selectedIcon = icon },
              contentAlignment = Alignment.Center
            ) {
              Text(icon, fontSize = 20.sp)
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (moodName.isNotBlank()) {
            onCreate(moodName.trim(), selectedColorHex, selectedIcon)
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = ObsidianCharcoal)
      ) {
        Text("बनाएँ", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("रद्द करें", color = TextMuted)
      }
    }
  )
}

// -------------------------------------------------------------
// 8. ADD TO MOOD PLAYLIST DIALOG
// -------------------------------------------------------------
@Composable
fun AddToMoodPlaylistDialog(
  song: SongEntity,
  playlists: List<PlaylistEntity>,
  songPlaylistsIds: List<Long>,
  onDismiss: () -> Unit,
  onToggleSongInPlaylist: (Long, Boolean) -> Unit,
  onOpenCreateMood: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = DarkSurfaceElevated,
    shape = RoundedCornerShape(20.dp),
    title = {
      Column {
        Text("मूड प्लेलिस्ट में जोड़ें", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(song.title, color = TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        if (playlists.isEmpty()) {
          Text(
            text = "अभी कोई मूड प्लेलिस्ट नहीं बनी है। नीचे '+ नया मूड बनाएँ' पर टैप करें।",
            color = TextMuted,
            fontSize = 13.sp
          )
        } else {
          playlists.forEach { playlist ->
            val isIn = songPlaylistsIds.contains(playlist.id)
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurface)
                .clickable { onToggleSongInPlaylist(playlist.id, !isIn) }
                .padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Checkbox(
                checked = isIn,
                onCheckedChange = { onToggleSongInPlaylist(playlist.id, it) },
                colors = CheckboxDefaults.colors(checkedColor = CyanNeon)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(playlist.icon, fontSize = 16.sp)
              Spacer(modifier = Modifier.width(8.dp))
              Text(playlist.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
          }
        }

        OutlinedButton(
          onClick = {
            onDismiss()
            onOpenCreateMood()
          },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("+ नया मूड बनाएँ")
        }
      }
    },
    confirmButton = {
      Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = ObsidianCharcoal)) {
        Text("पूर्ण")
      }
    }
  )
}

// -------------------------------------------------------------
// 9. FOLDER BLACKLIST DIALOG
// -------------------------------------------------------------
@Composable
fun FolderBlacklistDialog(
  onDismiss: () -> Unit,
  onRescanRequested: () -> Unit
) {
  val context = LocalContext.current
  val blacklistManager = remember { FolderBlacklistManager.getInstance(context) }
  var customFolders by remember { mutableStateOf(blacklistManager.getCustomBlacklist().toList()) }
  var newFolderInput by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = DarkSurfaceElevated,
    shape = RoundedCornerShape(22.dp),
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.FolderOff, contentDescription = null, tint = Color(0xFFF43F5E))
        Spacer(modifier = Modifier.width(8.dp))
        Text("फ़ोल्डर ब्लैकलिस्ट (Blacklist)", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "WhatsApp ऑडियो, वॉयस नोट्स और कॉल रिकॉर्डिंग्स अपने आप बाहर रखी जाती हैं। आप अतिरिक्त फ़ोल्डर भी जोड़ सकते हैं:",
          color = TextMuted,
          fontSize = 12.sp,
          lineHeight = 17.sp
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          OutlinedTextField(
            value = newFolderInput,
            onValueChange = { newFolderInput = it },
            placeholder = { Text("फ़ोल्डर का नाम या पाथ") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
          Button(
            onClick = {
              if (newFolderInput.isNotBlank()) {
                blacklistManager.addCustomBlacklist(newFolderInput.trim())
                customFolders = blacklistManager.getCustomBlacklist().toList()
                newFolderInput = ""
                onRescanRequested()
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = ObsidianCharcoal),
            shape = RoundedCornerShape(10.dp)
          ) {
            Text("Add")
          }
        }

        if (customFolders.isNotEmpty()) {
          Text("कस्टम ब्लैकलिस्टेड फ़ोल्डर्स:", color = CyanNeon, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
          customFolders.forEach { folder ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(DarkSurface)
                .padding(horizontal = 10.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(folder, color = TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
              IconButton(
                onClick = {
                  blacklistManager.removeCustomBlacklist(folder)
                  customFolders = blacklistManager.getCustomBlacklist().toList()
                  onRescanRequested()
                },
                modifier = Modifier.size(28.dp)
              ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFF43F5E), modifier = Modifier.size(16.dp))
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = ObsidianCharcoal)) {
        Text("ठीक है")
      }
    }
  )
}
