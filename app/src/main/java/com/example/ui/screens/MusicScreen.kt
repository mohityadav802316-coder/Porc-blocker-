package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.PlaylistEntity
import com.example.data.model.SongEntity
import com.example.music.FolderBlacklistManager
import com.example.music.LocalMusicScanner
import com.example.music.MusicPlayerState
import com.example.ui.components.AddToMoodPlaylistDialog
import com.example.ui.components.CreateMoodPlaylistDialog
import com.example.ui.components.EqualizerDialog
import com.example.ui.components.FolderBlacklistDialog
import com.example.ui.components.LrcLyricsDialog
import com.example.ui.components.MiniMusicPlayer
import com.example.ui.components.NowPlayingScreenDialog
import com.example.ui.components.QueueBottomSheetDialog
import com.example.ui.components.SleepTimerDialog
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.GoldBrass
import com.example.ui.theme.ObsidianCharcoal
import com.example.ui.theme.SageGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletNeon
import com.example.ui.viewmodel.LifeTrackerViewModel
import com.example.util.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class MusicMainTab(val titleHi: String, val titleEn: String) {
  SONGS("गाने", "Songs"),
  ALBUMS("एल्बम", "Albums"),
  ARTISTS("कलाकार", "Artists"),
  MOODS("मूड प्लेलिस्ट", "Moods"),
  FOLDERS("फ़ोल्डर", "Folders")
}

enum class SongFilterType {
  ALL, FAVORITES, RECENT
}

@Composable
fun MusicScreen(
  viewModel: LifeTrackerViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val playerState by viewModel.musicPlayerState.collectAsState()
  val allSongs by viewModel.allSongs.collectAsState()
  val allPlaylists by viewModel.allPlaylists.collectAsState()
  val favoriteSongs by viewModel.favoriteSongs.collectAsState()
  val recentlyPlayed by viewModel.recentlyPlayedSongs.collectAsState()

  var selectedTab by remember { mutableStateOf(MusicMainTab.SONGS) }
  var selectedFilter by remember { mutableStateOf(SongFilterType.ALL) }
  var searchQuery by remember { mutableStateOf("") }

  // Dialog states
  var showNowPlayingDialog by remember { mutableStateOf(false) }
  var showEqualizerDialog by remember { mutableStateOf(false) }
  var showSleepTimerDialog by remember { mutableStateOf(false) }
  var showLyricsDialogForSong by remember { mutableStateOf<SongEntity?>(null) }
  var showQueueDialog by remember { mutableStateOf(false) }
  var showCreateMoodDialog by remember { mutableStateOf(false) }
  var showAddToMoodDialogForSong by remember { mutableStateOf<SongEntity?>(null) }
  var songMoodPlaylistIds by remember { mutableStateOf<List<Long>>(emptyList()) }
  var showFolderBlacklistDialog by remember { mutableStateOf(false) }

  // Folder scanning state
  var folderList by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }

  // Media permission launcher
  val permissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) {
      viewModel.scanDeviceMusic()
      Toast.makeText(context, "म्यूजिक स्कैनिंग शुरू...", Toast.LENGTH_SHORT).show()
    }
  }

  LaunchedEffect(Unit) {
    if (LocalMusicScanner.hasStoragePermission(context)) {
      if (allSongs.isEmpty()) {
        viewModel.scanDeviceMusic()
      }
      coroutineScope.launch {
        folderList = LocalMusicScanner.getFolders(context)
      }
    }
  }

  // Filtered song list based on search and quick filter
  val displayedSongs = remember(allSongs, favoriteSongs, recentlyPlayed, selectedFilter, searchQuery) {
    val baseList = when (selectedFilter) {
      SongFilterType.ALL -> allSongs
      SongFilterType.FAVORITES -> favoriteSongs
      SongFilterType.RECENT -> recentlyPlayed
    }
    if (searchQuery.isBlank()) {
      baseList
    } else {
      val q = searchQuery.trim().lowercase()
      baseList.filter {
        it.title.lowercase().contains(q) ||
        it.artist.lowercase().contains(q) ||
        it.album.lowercase().contains(q)
      }
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(DarkBackground)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
    ) {
      // --- Top App Bar ---
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(CyanNeon.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.MusicNote, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(20.dp))
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text("ऑफलाइन म्यूजिक प्लेयर", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("${allSongs.size} गाने • स्थानीय स्टोरेज", color = TextMuted, fontSize = 11.sp)
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          // Folder Blacklist shortcut
          IconButton(onClick = { showFolderBlacklistDialog = true }) {
            Icon(Icons.Default.FolderOff, contentDescription = "Blacklist", tint = TextSecondary, modifier = Modifier.size(22.dp))
          }

          // Equalizer shortcut
          IconButton(onClick = { showEqualizerDialog = true }) {
            Icon(Icons.Default.Equalizer, contentDescription = "Equalizer", tint = VioletNeon, modifier = Modifier.size(22.dp))
          }

          // Sleep timer shortcut
          IconButton(onClick = { showSleepTimerDialog = true }) {
            Icon(Icons.Default.Timer, contentDescription = "Sleep Timer", tint = GoldBrass, modifier = Modifier.size(22.dp))
          }

          // Refresh / Rescan
          IconButton(
            onClick = {
              if (LocalMusicScanner.hasStoragePermission(context)) {
                viewModel.scanDeviceMusic()
                coroutineScope.launch { folderList = LocalMusicScanner.getFolders(context) }
                Toast.makeText(context, "म्यूजिक स्कैन चालू...", Toast.LENGTH_SHORT).show()
              } else {
                val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                  Manifest.permission.READ_MEDIA_AUDIO
                } else {
                  Manifest.permission.READ_EXTERNAL_STORAGE
                }
                permissionLauncher.launch(perm)
              }
            }
          ) {
            Icon(Icons.Default.Refresh, contentDescription = "Rescan", tint = TextPrimary)
          }
        }
      }

      // --- Search Bar ---
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp)
      ) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("गाना, कलाकार या एल्बम खोजें...", color = TextMuted, fontSize = 13.sp) },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = DarkSurfaceElevated,
            unfocusedContainerColor = DarkSurfaceElevated,
            focusedBorderColor = CyanNeon.copy(alpha = 0.5f),
            unfocusedBorderColor = DarkSurfaceBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
          ),
          modifier = Modifier.fillMaxWidth().testTag("music_search_bar")
        )
      }

      // --- Quick Filters: All / Favorites / Recent ---
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp)
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        FilterChipItem(
          label = "सभी गाने (${allSongs.size})",
          isSelected = selectedFilter == SongFilterType.ALL,
          onClick = { selectedFilter = SongFilterType.ALL }
        )
        FilterChipItem(
          label = "पसंदीदा (${favoriteSongs.size})",
          isSelected = selectedFilter == SongFilterType.FAVORITES,
          icon = Icons.Default.Favorite,
          iconTint = Color(0xFFEC4899),
          onClick = { selectedFilter = SongFilterType.FAVORITES }
        )
        FilterChipItem(
          label = "हाल ही में (${recentlyPlayed.size})",
          isSelected = selectedFilter == SongFilterType.RECENT,
          icon = Icons.Default.History,
          iconTint = GoldBrass,
          onClick = { selectedFilter = SongFilterType.RECENT }
        )
      }

      // --- 5 Main Tabs (Songs, Albums, Artists, Moods, Folders) ---
      ScrollableTabRow(
        selectedTabIndex = selectedTab.ordinal,
        containerColor = DarkBackground,
        contentColor = CyanNeon,
        edgePadding = 16.dp,
        indicator = { tabPositions ->
          TabRowDefaults.SecondaryIndicator(
            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
            color = CyanNeon
          )
        },
        divider = { Divider(color = DarkSurfaceBorder) }
      ) {
        MusicMainTab.entries.forEach { tab ->
          Tab(
            selected = selectedTab == tab,
            onClick = { selectedTab = tab },
            text = {
              Text(
                text = "${tab.titleHi} (${tab.titleEn})",
                fontSize = 13.sp,
                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
              )
            }
          )
        }
      }

      // --- Tab Content ---
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
      ) {
        when (selectedTab) {
          MusicMainTab.SONGS -> {
            SongsListTab(
              songs = displayedSongs,
              currentSongId = playerState.currentSong?.id,
              isPlaying = playerState.isPlaying,
              onPlaySong = { song, index ->
                viewModel.playMusicQueue(displayedSongs, index)
              },
              onPlayNext = { song ->
                viewModel.playNextSong(song)
                Toast.makeText(context, "अगला गाना सेट: ${song.title}", Toast.LENGTH_SHORT).show()
              },
              onAddToQueue = { song ->
                viewModel.addSongToQueue(song)
                Toast.makeText(context, "कतार में जोड़ा गया: ${song.title}", Toast.LENGTH_SHORT).show()
              },
              onAddToMood = { song ->
                viewModel.getPlaylistsForSong(song.id) { ids ->
                  songMoodPlaylistIds = ids
                  showAddToMoodDialogForSong = song
                }
              },
              onToggleFavorite = { viewModel.toggleSongFavorite(it) },
              onViewLyrics = { showLyricsDialogForSong = it },
              onRequestPermission = {
                val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                  Manifest.permission.READ_MEDIA_AUDIO
                } else {
                  Manifest.permission.READ_EXTERNAL_STORAGE
                }
                permissionLauncher.launch(perm)
              }
            )
          }

          MusicMainTab.ALBUMS -> {
            AlbumsGridTab(
              allSongs = allSongs,
              onPlayAlbum = { albumSongs ->
                viewModel.playMusicQueue(albumSongs, 0)
              }
            )
          }

          MusicMainTab.ARTISTS -> {
            ArtistsListTab(
              allSongs = allSongs,
              onPlayArtist = { artistSongs ->
                viewModel.playMusicQueue(artistSongs, 0)
              }
            )
          }

          MusicMainTab.MOODS -> {
            MoodPlaylistsTab(
              playlists = allPlaylists,
              allSongs = allSongs,
              onPlayMood = { playlist, shuffle ->
                coroutineScope.launch {
                  val db = com.example.data.db.LifeTrackerDatabase.getDatabase(context)
                  val playlistSongs = db.musicDao().getSongsForPlaylistSync(playlist.id)
                  if (playlistSongs.isNotEmpty()) {
                    val finalQueue = if (shuffle) playlistSongs.shuffled() else playlistSongs
                    viewModel.playMusicQueue(finalQueue, 0, playlist = playlist)
                  } else {
                    Toast.makeText(context, "इस मूड प्लेलिस्ट में कोई गाना नहीं है! गानों की लिस्ट से गाने जोड़ें।", Toast.LENGTH_SHORT).show()
                  }
                }
              },
              onOpenCreateMood = { showCreateMoodDialog = true },
              onDeleteMood = { playlistId ->
                viewModel.deletePlaylist(playlistId)
                Toast.makeText(context, "मूड प्लेलिस्ट हटाई गई", Toast.LENGTH_SHORT).show()
              }
            )
          }

          MusicMainTab.FOLDERS -> {
            FoldersTab(
              folders = folderList,
              onOpenFolderBlacklist = { showFolderBlacklistDialog = true }
            )
          }
        }
      }

      // --- Mini-Player Sticky at Bottom ---
      if (playerState.currentSong != null) {
        MiniMusicPlayer(
          playerState = playerState,
          onExpandNowPlaying = { showNowPlayingDialog = true },
          onTogglePlayPause = { viewModel.toggleMusicPlayPause() },
          onNext = { viewModel.nextMusicSong() },
          onToggleFavorite = { viewModel.toggleSongFavorite(it) }
        )
      }
    }
  }

  // --- DIALOGS ---

  // 1. Full Now Playing Screen
  if (showNowPlayingDialog && playerState.currentSong != null) {
    NowPlayingScreenDialog(
      playerState = playerState,
      onDismiss = { showNowPlayingDialog = false },
      onTogglePlayPause = { viewModel.toggleMusicPlayPause() },
      onNext = { viewModel.nextMusicSong() },
      onPrevious = { viewModel.previousMusicSong() },
      onSeekTo = { viewModel.seekMusicTo(it) },
      onToggleShuffle = { viewModel.toggleMusicShuffle() },
      onToggleRepeat = { viewModel.toggleMusicRepeat() },
      onToggleFavorite = { viewModel.toggleSongFavorite(it) },
      onOpenEqualizer = { showEqualizerDialog = true },
      onOpenSleepTimer = { showSleepTimerDialog = true },
      onOpenLyrics = { showLyricsDialogForSong = playerState.currentSong },
      onOpenQueue = { showQueueDialog = true },
      onSelectSpeed = { viewModel.setMusicPlaybackSpeed(it) }
    )
  }

  // 2. Equalizer Dialog
  if (showEqualizerDialog) {
    EqualizerDialog(onDismiss = { showEqualizerDialog = false })
  }

  // 3. Sleep Timer Dialog
  if (showSleepTimerDialog) {
    SleepTimerDialog(onDismiss = { showSleepTimerDialog = false })
  }

  // 4. Lyrics Dialog
  if (showLyricsDialogForSong != null) {
    LrcLyricsDialog(
      song = showLyricsDialogForSong!!,
      currentPositionMs = playerState.currentPositionMs,
      onDismiss = { showLyricsDialogForSong = null }
    )
  }

  // 5. Queue Bottom Sheet Dialog
  if (showQueueDialog) {
    QueueBottomSheetDialog(
      playerState = playerState,
      onDismiss = { showQueueDialog = false },
      onSelectSong = { song, index ->
        viewModel.playMusicQueue(playerState.queue, index)
      },
      onMoveItem = { from, to -> viewModel.moveQueueItem(from, to) },
      onRemoveItem = { index -> viewModel.removeFromQueue(index) },
      onClearQueue = { viewModel.clearMusicQueue() }
    )
  }

  // 6. Create Mood Playlist Dialog
  if (showCreateMoodDialog) {
    CreateMoodPlaylistDialog(
      onDismiss = { showCreateMoodDialog = false },
      onCreate = { name, colorHex, icon ->
        viewModel.createMoodPlaylist(name, colorHex, icon)
        showCreateMoodDialog = false
        Toast.makeText(context, "नया मूड '$name' बनाया गया!", Toast.LENGTH_SHORT).show()
      }
    )
  }

  // 7. Add To Mood Playlist Dialog
  if (showAddToMoodDialogForSong != null) {
    AddToMoodPlaylistDialog(
      song = showAddToMoodDialogForSong!!,
      playlists = allPlaylists,
      songPlaylistsIds = songMoodPlaylistIds,
      onDismiss = { showAddToMoodDialogForSong = null },
      onToggleSongInPlaylist = { playlistId, add ->
        if (add) {
          viewModel.addSongToPlaylist(playlistId, showAddToMoodDialogForSong!!.id)
          songMoodPlaylistIds = songMoodPlaylistIds + playlistId
          Toast.makeText(context, "मूड में जोड़ा गया", Toast.LENGTH_SHORT).show()
        } else {
          viewModel.removeSongFromPlaylist(playlistId, showAddToMoodDialogForSong!!.id)
          songMoodPlaylistIds = songMoodPlaylistIds - playlistId
          Toast.makeText(context, "मूड से हटाया गया", Toast.LENGTH_SHORT).show()
        }
      },
      onOpenCreateMood = {
        showAddToMoodDialogForSong = null
        showCreateMoodDialog = true
      }
    )
  }

  // 8. Folder Blacklist Dialog
  if (showFolderBlacklistDialog) {
    FolderBlacklistDialog(
      onDismiss = { showFolderBlacklistDialog = false },
      onRescanRequested = {
        viewModel.scanDeviceMusic()
        coroutineScope.launch { folderList = LocalMusicScanner.getFolders(context) }
      }
    )
  }
}

// -------------------------------------------------------------
// FILTER CHIP COMPOSABLE
// -------------------------------------------------------------
@Composable
private fun FilterChipItem(
  label: String,
  isSelected: Boolean,
  icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
  iconTint: Color = CyanNeon,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(20.dp))
      .background(if (isSelected) CyanNeon.copy(alpha = 0.2f) else DarkSurfaceElevated)
      .border(1.dp, if (isSelected) CyanNeon else DarkSurfaceBorder, RoundedCornerShape(20.dp))
      .clickable { onClick() }
      .padding(horizontal = 14.dp, vertical = 7.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      if (icon != null) {
        Icon(icon, contentDescription = null, tint = if (isSelected) iconTint else TextMuted, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
      }
      Text(
        text = label,
        color = if (isSelected) TextPrimary else TextMuted,
        fontSize = 12.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
      )
    }
  }
}

// -------------------------------------------------------------
// TAB 1: SONGS LIST (WITH A-Z FAST SCROLLER & 3-DOT MENU)
// -------------------------------------------------------------
@Composable
private fun SongsListTab(
  songs: List<SongEntity>,
  currentSongId: String?,
  isPlaying: Boolean,
  onPlaySong: (SongEntity, Int) -> Unit,
  onPlayNext: (SongEntity) -> Unit,
  onAddToQueue: (SongEntity) -> Unit,
  onAddToMood: (SongEntity) -> Unit,
  onToggleFavorite: (SongEntity) -> Unit,
  onViewLyrics: (SongEntity) -> Unit,
  onRequestPermission: () -> Unit
) {
  val listState = rememberLazyListState()
  val coroutineScope = rememberCoroutineScope()

  if (songs.isEmpty()) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(24.dp)
      ) {
        Icon(Icons.Default.MusicNote, contentDescription = null, tint = TextMuted, modifier = Modifier.size(54.dp))
        Text("कोई गाना नहीं मिला", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text(
          text = "यदि आपके डिवाइस में गाने हैं, तो कृपया अनुमति दें और ऊपर रीस्कैन बटन दबाएं।",
          color = TextMuted,
          fontSize = 12.sp,
          textAlign = TextAlign.Center
        )
        Button(
          onClick = onRequestPermission,
          colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = ObsidianCharcoal)
        ) {
          Text("स्टोरेज अनुमति दें (Grant Permission)")
        }
      }
    }
    return
  }

  // Fast scroll index letters
  val alphabet = remember(songs) {
    songs.map { it.title.firstOrNull()?.uppercaseChar() ?: '#' }.distinct()
  }

  Row(modifier = Modifier.fillMaxSize()) {
    // Songs LazyColumn
    LazyColumn(
      state = listState,
      modifier = Modifier.weight(1f).fillMaxHeight(),
      contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      itemsIndexed(songs, key = { _, s -> s.id }) { index, song ->
        val isCurrent = song.id == currentSongId

        var menuExpanded by remember { mutableStateOf(false) }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isCurrent) CyanNeon.copy(alpha = 0.12f) else DarkSurfaceElevated)
            .border(1.dp, if (isCurrent) CyanNeon.copy(alpha = 0.45f) else DarkSurfaceBorder, RoundedCornerShape(14.dp))
            .clickable { onPlaySong(song, index) }
            .padding(horizontal = 10.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Album Art thumbnail
          Box(
            modifier = Modifier
              .size(44.dp)
              .clip(RoundedCornerShape(8.dp))
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
              Icon(Icons.Default.MusicNote, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(22.dp))
            }
          }

          Spacer(modifier = Modifier.width(12.dp))

          // Title & Artist
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = song.title,
              color = if (isCurrent) CyanNeon else TextPrimary,
              fontSize = 14.sp,
              fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "${song.artist} • ${TimeUtils.formatDuration(song.durationMs)}",
              color = TextMuted,
              fontSize = 12.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }

          // Favorite toggle
          IconButton(
            onClick = { onToggleFavorite(song) },
            modifier = Modifier.size(34.dp)
          ) {
            Icon(
              imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
              contentDescription = "Favorite",
              tint = if (song.isFavorite) Color(0xFFEC4899) else TextMuted,
              modifier = Modifier.size(18.dp)
            )
          }

          // 3-dot dropdown menu
          Box {
            IconButton(
              onClick = { menuExpanded = true },
              modifier = Modifier.size(34.dp)
            ) {
              Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = TextMuted, modifier = Modifier.size(20.dp))
            }

            DropdownMenu(
              expanded = menuExpanded,
              onDismissRequest = { menuExpanded = false },
              modifier = Modifier.background(DarkSurfaceElevated)
            ) {
              DropdownMenuItem(
                text = { Text("अगला बजाएं (Play Next)") },
                onClick = {
                  menuExpanded = false
                  onPlayNext(song)
                }
              )
              DropdownMenuItem(
                text = { Text("कतार में जोड़ें (Add to Queue)") },
                onClick = {
                  menuExpanded = false
                  onAddToQueue(song)
                }
              )
              DropdownMenuItem(
                text = { Text("मूड प्लेलिस्ट में जोड़ें (Add to Mood)") },
                onClick = {
                  menuExpanded = false
                  onAddToMood(song)
                }
              )
              DropdownMenuItem(
                text = { Text("लिरिक्स देखें (View Lyrics)") },
                onClick = {
                  menuExpanded = false
                  onViewLyrics(song)
                }
              )
            }
          }
        }
      }
    }

    // A-Z fast scroller column
    if (alphabet.size > 5) {
      Column(
        modifier = Modifier
          .fillMaxHeight()
          .padding(vertical = 8.dp, horizontal = 2.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        alphabet.forEach { char ->
          Text(
            text = char.toString(),
            color = TextMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
              .clickable {
                val targetIndex = songs.indexOfFirst {
                  (it.title.firstOrNull()?.uppercaseChar() ?: '#') == char
                }
                if (targetIndex >= 0) {
                  coroutineScope.launch { listState.scrollToItem(targetIndex) }
                }
              }
              .padding(horizontal = 2.dp)
          )
        }
      }
    }
  }
}

// -------------------------------------------------------------
// TAB 2: ALBUMS GRID
// -------------------------------------------------------------
@Composable
private fun AlbumsGridTab(
  allSongs: List<SongEntity>,
  onPlayAlbum: (List<SongEntity>) -> Unit
) {
  val albums = remember(allSongs) {
    allSongs.groupBy { it.album }.toList().sortedBy { it.first.lowercase() }
  }

  if (albums.isEmpty()) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text("कोई एल्बम नहीं मिला", color = TextMuted)
    }
    return
  }

  LazyVerticalGrid(
    columns = GridCells.Fixed(2),
    contentPadding = PaddingValues(14.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = Modifier.fillMaxSize()
  ) {
    items(albums) { (albumName, songs) ->
      val firstCover = songs.firstOrNull { !it.albumArtUri.isNullOrBlank() }?.albumArtUri

      Column(
        modifier = Modifier
          .clip(RoundedCornerShape(16.dp))
          .background(DarkSurfaceElevated)
          .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(16.dp))
          .clickable { onPlayAlbum(songs) }
          .padding(10.dp)
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceBorder),
          contentAlignment = Alignment.Center
        ) {
          if (!firstCover.isNullOrBlank()) {
            AsyncImage(
              model = firstCover,
              contentDescription = null,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize()
            )
          } else {
            Icon(Icons.Default.Album, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(48.dp))
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = albumName,
          color = TextPrimary,
          fontSize = 14.sp,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = "${songs.size} गाने",
          color = TextMuted,
          fontSize = 12.sp
        )
      }
    }
  }
}

// -------------------------------------------------------------
// TAB 3: ARTISTS LIST
// -------------------------------------------------------------
@Composable
private fun ArtistsListTab(
  allSongs: List<SongEntity>,
  onPlayArtist: (List<SongEntity>) -> Unit
) {
  val artists = remember(allSongs) {
    allSongs.groupBy { it.artist }.toList().sortedBy { it.first.lowercase() }
  }

  if (artists.isEmpty()) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text("कोई कलाकार नहीं मिला", color = TextMuted)
    }
    return
  }

  LazyColumn(
    contentPadding = PaddingValues(14.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
    modifier = Modifier.fillMaxSize()
  ) {
    items(artists) { (artistName, songs) ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(DarkSurfaceElevated)
          .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(14.dp))
          .clickable { onPlayArtist(songs) }
          .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(VioletNeon.copy(alpha = 0.2f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.Person, contentDescription = null, tint = VioletNeon, modifier = Modifier.size(24.dp))
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(artistName, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
          Text("${songs.size} गाने", color = TextMuted, fontSize = 12.sp)
        }

        IconButton(onClick = { onPlayArtist(songs) }) {
          Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = CyanNeon)
        }
      }
    }
  }
}

// -------------------------------------------------------------
// TAB 4: MOOD PLAYLISTS (BLANK INITIALLY, USER CREATES MOODS)
// -------------------------------------------------------------
@Composable
private fun MoodPlaylistsTab(
  playlists: List<PlaylistEntity>,
  allSongs: List<SongEntity>,
  onPlayMood: (PlaylistEntity, Boolean) -> Unit,
  onOpenCreateMood: () -> Unit,
  onDeleteMood: (Long) -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(14.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text("मूड प्लेलिस्ट्स (Moods)", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("अपने अनुसार मूड बनाएँ और गाने जोड़ें", color = TextMuted, fontSize = 12.sp)
      }

      Button(
        onClick = onOpenCreateMood,
        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = ObsidianCharcoal),
        shape = RoundedCornerShape(12.dp)
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("+ नया मूड", fontWeight = FontWeight.Bold, fontSize = 13.sp)
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Requirement: Starts completely empty with friendly prompt
    if (playlists.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .clip(RoundedCornerShape(20.dp))
          .background(DarkSurfaceElevated)
          .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(20.dp))
          .padding(24.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text("🎨", fontSize = 48.sp)
          Text("अभी कोई मूड प्लेलिस्ट नहीं है", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
          Text(
            text = "कोई डेमो या प्री-सीडेड मूड नहीं रखा गया है। आप खुद अपने मूड्स (जैसे: जिम, रात, ड्राइविंग, फोकस) नाम और रंग चुनकर बनाएँ।",
            color = TextMuted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
          )
          Spacer(modifier = Modifier.height(6.dp))
          Button(
            onClick = onOpenCreateMood,
            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = ObsidianCharcoal),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text("पहला मूड प्लेलिस्ट बनाएँ")
          }
        }
      }
    } else {
      LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
      ) {
        items(playlists) { playlist ->
          val moodColor = try { Color(android.graphics.Color.parseColor(playlist.colorHex)) } catch (_: Exception) { CyanNeon }

          Column(
            modifier = Modifier
              .clip(RoundedCornerShape(18.dp))
              .background(DarkSurfaceElevated)
              .border(1.5.dp, moodColor.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
              .padding(14.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(playlist.icon, fontSize = 28.sp)
              IconButton(
                onClick = { onDeleteMood(playlist.id) },
                modifier = Modifier.size(28.dp)
              ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(16.dp))
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
              text = playlist.name,
              color = TextPrimary,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Play & Shuffle buttons
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Button(
                onClick = { onPlayMood(playlist, false) },
                colors = ButtonDefaults.buttonColors(containerColor = moodColor, contentColor = ObsidianCharcoal),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
              ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Play", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }

              OutlinedButton(
                onClick = { onPlayMood(playlist, true) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
              ) {
                Icon(Icons.Default.Shuffle, contentDescription = null, tint = moodColor, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Mix", fontSize = 12.sp, color = moodColor)
              }
            }
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// TAB 5: FOLDERS TAB
// -------------------------------------------------------------
@Composable
private fun FoldersTab(
  folders: List<Pair<String, Int>>,
  onOpenFolderBlacklist: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(14.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text("डिवाइस फ़ोल्डर्स", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("फ़ोल्डर के अनुसार म्यूजिक ब्राउज़ करें", color = TextMuted, fontSize = 12.sp)
      }

      OutlinedButton(
        onClick = onOpenFolderBlacklist,
        shape = RoundedCornerShape(10.dp)
      ) {
        Icon(Icons.Default.FolderOff, contentDescription = null, tint = Color(0xFFF43F5E), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("ब्लैकलिस्ट सेटिंग्स", color = TextPrimary, fontSize = 12.sp)
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    if (folders.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("कोई फ़ोल्डर नहीं मिला", color = TextMuted)
      }
    } else {
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
      ) {
        items(folders) { (folderName, count) ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .background(DarkSurfaceElevated)
              .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(14.dp))
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(GoldBrass.copy(alpha = 0.2f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Folder, contentDescription = null, tint = GoldBrass, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(folderName, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
              Text("$count संगीत ट्रैक्स", color = TextMuted, fontSize = 12.sp)
            }
          }
        }
      }
    }
  }
}
