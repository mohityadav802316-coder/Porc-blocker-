package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BreathPhase
import com.example.data.model.MeditationSessionEntity
import com.example.data.model.MeditationType
import com.example.ui.theme.CardBorderGradient
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.GlassGradient
import com.example.ui.theme.StatusComplete
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletGlow
import com.example.ui.theme.VioletNeon
import com.example.ui.viewmodel.LifeTrackerViewModel
import com.example.ui.viewmodel.MainTab
import com.example.util.TimeUtils

@Composable
fun MeditationScreen(
  viewModel: LifeTrackerViewModel,
  modifier: Modifier = Modifier
) {
  val state by viewModel.meditationState.collectAsState()
  val userName by viewModel.userName.collectAsState()
  val chimeEnabled by viewModel.meditationChimeEnabled.collectAsState()
  val voiceLang by viewModel.meditationVoiceLanguage.collectAsState()
  val allSessions by viewModel.allMeditationSessions.collectAsState()
  val stats by viewModel.meditationStats.collectAsState()

  var selectedType by remember { mutableStateOf(MeditationType.BREATHING) }
  var selectedDurationMinutes by remember { mutableIntStateOf(10) }
  var showBreathSettingsDialog by remember { mutableStateOf(false) }

  val context = androidx.compose.ui.platform.LocalContext.current
  androidx.compose.runtime.DisposableEffect(Unit) {
    onDispose {
      val manager = com.example.meditation.MeditationManager.getInstance(context)
      manager.breathAudioPlayer.stop()
      manager.stopSession(savePartial = true)
    }
  }

  androidx.activity.compose.BackHandler {
    if (state.isActive) {
      viewModel.stopMeditation()
    } else {
      viewModel.navigateBack()
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(DarkBackground)
      .statusBarsPadding()
  ) {
    if (state.isCompleted) {
      // 1. Completion Celebration View
      MeditationCompletedView(
        state = state,
        userName = userName,
        onDone = {
          viewModel.dismissMeditationCompletion()
        }
      )
    } else if (state.isActive) {
      // 2. Active Immersive Full-Screen Sanctuary
      ActiveMeditationView(
        state = state,
        onPause = { viewModel.pauseMeditation() },
        onResume = { viewModel.resumeMeditation() },
        onStop = { viewModel.stopMeditation() }
      )
    } else {
      // 3. Meditation Dashboard & Setup View
      MeditationSetupView(
        userName = userName,
        selectedType = selectedType,
        onSelectType = { selectedType = it },
        selectedDuration = selectedDurationMinutes,
        onSelectDuration = { selectedDurationMinutes = it },
        chimeEnabled = chimeEnabled,
        onToggleChime = {
          viewModel.updateMeditationSettings(!chimeEnabled, voiceLang)
        },
        voiceLang = voiceLang,
        onToggleLang = {
          val next = if (voiceLang == "HI") "EN" else "HI"
          viewModel.updateMeditationSettings(chimeEnabled, next)
        },
        onOpenBreathSettings = {
          showBreathSettingsDialog = true
        },
        onStart = {
          viewModel.startMeditation(
            type = selectedType,
            durationMinutes = selectedDurationMinutes,
            chimeEnabled = chimeEnabled,
            voiceLanguage = voiceLang
          )
        },
        onBack = {
          viewModel.navigateBack()
        },
        stats = stats,
        recentSessions = allSessions.take(5)
      )
    }

    if (showBreathSettingsDialog) {
      com.example.ui.components.MeditationBreathSettingsDialog(
        onDismiss = { showBreathSettingsDialog = false }
      )
    }
  }
}

@Composable
private fun MeditationSetupView(
  userName: String,
  selectedType: MeditationType,
  onSelectType: (MeditationType) -> Unit,
  selectedDuration: Int,
  onSelectDuration: (Int) -> Unit,
  chimeEnabled: Boolean,
  onToggleChime: () -> Unit,
  voiceLang: String,
  onToggleLang: () -> Unit,
  onOpenBreathSettings: () -> Unit,
  onStart: () -> Unit,
  onBack: () -> Unit,
  stats: com.example.data.model.MeditationStats,
  recentSessions: List<MeditationSessionEntity>
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(top = 12.dp, bottom = 130.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Top Bar
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = onBack,
            modifier = Modifier.size(38.dp)
          ) {
            Icon(
              Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = TextPrimary
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = "ध्यान साधना",
              color = CyanNeon,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp
            )
            Text(
              text = "Meditation Sanctuary",
              color = TextPrimary,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          // Voice Language Toggle
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .background(DarkSurfaceElevated)
              .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
              .clickable { onToggleLang() }
              .padding(horizontal = 8.dp, vertical = 6.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Translate, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = if (voiceLang == "HI") "हिन्दी" else "EN",
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }

          Spacer(modifier = Modifier.width(8.dp))

          // Breath Sounds Settings
          IconButton(
            onClick = onOpenBreathSettings,
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(DarkSurfaceElevated)
              .border(1.dp, DarkSurfaceBorder, CircleShape)
          ) {
            Icon(
              Icons.Default.Air,
              contentDescription = "Breath Sound Settings",
              tint = CyanNeon,
              modifier = Modifier.size(18.dp)
            )
          }

          Spacer(modifier = Modifier.width(8.dp))

          // Bell Chime Toggle
          IconButton(
            onClick = onToggleChime,
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(DarkSurfaceElevated)
              .border(1.dp, DarkSurfaceBorder, CircleShape)
          ) {
            Icon(
              if (chimeEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
              contentDescription = "Chime Bell",
              tint = if (chimeEnabled) CyanNeon else TextMuted,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }
    }

    // Hero Sanctuary Greeting
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(22.dp))
          .background(
            Brush.radialGradient(
              colors = listOf(
                Color(0xFF0F2B36),
                Color(0xFF0A121E),
                DarkBackground
              ),
              radius = 800f
            )
          )
          .border(1.dp, CardBorderGradient, RoundedCornerShape(22.dp))
          .padding(20.dp)
      ) {
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "स्वागत है, $userName ✨",
                color = TextPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Close your eyes & follow calm voice guidance",
                color = TextSecondary,
                fontSize = 12.sp
              )
            }
            Box(
              modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(
                  Brush.linearGradient(listOf(CyanNeon.copy(alpha = 0.3f), VioletNeon.copy(alpha = 0.3f)))
                )
                .border(1.dp, CyanNeon.copy(alpha = 0.5f), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                Icons.Default.SelfImprovement,
                contentDescription = null,
                tint = CyanNeon,
                modifier = Modifier.size(26.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Mini statistics bar
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(DarkSurface.copy(alpha = 0.7f))
              .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceAround
          ) {
            StatPill(title = "Sessions", value = "${stats.totalSessions}")
            StatPill(title = "Minutes", value = "${stats.totalMinutes}m")
            StatPill(title = "Completed", value = "${stats.completedSessions}")
          }
        }
      }
    }

    // Meditation Mode Selector (All 10 modes)
    item {
      Text(
        text = "MEDITATION FLOW (10 MODES)",
        color = CyanNeon,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
      )
      Spacer(modifier = Modifier.height(8.dp))

      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
      ) {
        items(MeditationType.entries) { type ->
          val isSelected = type == selectedType
          Box(
            modifier = Modifier
              .width(155.dp)
              .clip(RoundedCornerShape(16.dp))
              .background(
                if (isSelected) {
                  Brush.linearGradient(
                    listOf(Color(0xFF0F3642), Color(0xFF161F36))
                  )
                } else androidx.compose.ui.graphics.SolidColor(DarkSurfaceElevated)
              )
              .border(
                1.5.dp,
                if (isSelected) CyanNeon else DarkSurfaceBorder,
                RoundedCornerShape(16.dp)
              )
              .clickable { onSelectType(type) }
              .padding(14.dp)
              .testTag("mode_${type.id}")
          ) {
            Column {
              Box(
                modifier = Modifier
                  .size(34.dp)
                  .clip(CircleShape)
                  .background(if (isSelected) CyanNeon.copy(alpha = 0.2f) else DarkSurface)
                  .border(1.dp, if (isSelected) CyanNeon else DarkSurfaceBorder, CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  getIconForTag(type.iconTag),
                  contentDescription = null,
                  tint = if (isSelected) CyanNeon else TextSecondary,
                  modifier = Modifier.size(18.dp)
                )
              }
              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = type.englishTitle,
                color = if (isSelected) TextPrimary else TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = type.hindiTitle,
                color = if (isSelected) CyanNeon else TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "${type.cycleDurationSec}s Rhythm",
                color = TextMuted,
                fontSize = 10.sp
              )
            }
          }
        }
      }
    }

    // Selected Mode Details Card
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(GlassGradient)
          .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(16.dp))
          .padding(14.dp)
      ) {
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "${selectedType.englishTitle} (${selectedType.hindiTitle})",
              color = TextPrimary,
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = selectedType.description,
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 16.sp
          )
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            CadenceBadge("Inhale", "${selectedType.inhaleSec}s")
            CadenceBadge("Hold", "${selectedType.holdSec}s")
            CadenceBadge("Exhale", "${selectedType.exhaleSec}s")
            CadenceBadge("Relax", "${selectedType.restSec}s")
          }
        }
      }
    }

    // Duration Selector (5 / 10 / 15 / 20 / 30 Minutes)
    item {
      Text(
        text = "SESSION DURATION",
        color = CyanNeon,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
      )
      Spacer(modifier = Modifier.height(8.dp))

      val durations = listOf(5, 10, 15, 20, 30)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        durations.forEach { mins ->
          val isSelected = selectedDuration == mins
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(12.dp))
              .background(
                if (isSelected) {
                  Brush.linearGradient(listOf(CyanNeon.copy(alpha = 0.25f), VioletNeon.copy(alpha = 0.25f)))
                } else androidx.compose.ui.graphics.SolidColor(DarkSurfaceElevated)
              )
              .border(
                1.5.dp,
                if (isSelected) CyanNeon else DarkSurfaceBorder,
                RoundedCornerShape(12.dp)
              )
              .clickable { onSelectDuration(mins) }
              .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "${mins}m",
              color = if (isSelected) TextPrimary else TextSecondary,
              fontSize = 13.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
          }
        }
      }
    }

    // Begin Meditation Main Action Button
    item {
      Button(
        onClick = onStart,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp)
          .testTag("start_meditation_button"),
        colors = ButtonDefaults.buttonColors(
          containerColor = CyanNeon,
          contentColor = Color(0xFF003038)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 1.dp)
      ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Begin ${selectedType.englishTitle} ($selectedDuration min)",
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold
        )
      }

      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "🔒 Screen-off & Background playback supported with voice cues & Tibetan chimes",
        color = TextMuted,
        fontSize = 11.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
      )
    }

    // Recent Sessions History
    if (recentSessions.isNotEmpty()) {
      item {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "RECENT MEDITATIONS",
          color = VioletNeon,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp
        )
      }

      items(recentSessions) { session ->
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(32.dp)
                  .clip(CircleShape)
                  .background(if (session.isCompleted) StatusComplete.copy(alpha = 0.2f) else DarkSurface),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  if (session.isCompleted) Icons.Default.Check else Icons.Default.Spa,
                  contentDescription = null,
                  tint = if (session.isCompleted) StatusComplete else TextSecondary,
                  modifier = Modifier.size(16.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "${session.type} Meditation",
                  color = TextPrimary,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = "${TimeUtils.formatDateDisplay(session.date)} • ${session.durationMinutes} min",
                  color = TextMuted,
                  fontSize = 11.sp
                )
              }
            }

            Text(
              text = if (session.isCompleted) "Completed" else "Partial (${session.completedSeconds / 60}m)",
              color = if (session.isCompleted) StatusComplete else TextMuted,
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ActiveMeditationView(
  state: com.example.meditation.MeditationState,
  onPause: () -> Unit,
  onResume: () -> Unit,
  onStop: () -> Unit
) {
  val type = state.type
  val phase = state.currentPhase

  // Breathing circle dynamic animation
  val targetScale = when (phase) {
    BreathPhase.INHALE -> 1.25f
    BreathPhase.HOLD -> 1.22f
    BreathPhase.EXHALE -> 0.82f
    BreathPhase.REST -> 0.80f
  }

  val animatedScale by animateFloatAsState(
    targetValue = targetScale,
    animationSpec = tween(
      durationMillis = when (phase) {
        BreathPhase.INHALE -> (type.inhaleSec * 1000).coerceAtLeast(1000)
        BreathPhase.HOLD -> 800
        BreathPhase.EXHALE -> (type.exhaleSec * 1000).coerceAtLeast(1000)
        BreathPhase.REST -> 800
      },
      easing = FastOutSlowInEasing
    ),
    label = "breathing_scale"
  )

  val phaseColor by animateColorAsState(
    targetValue = when (phase) {
      BreathPhase.INHALE -> CyanNeon
      BreathPhase.HOLD -> VioletNeon
      BreathPhase.EXHALE -> Color(0xFF38BDF8)
      BreathPhase.REST -> Color(0xFF818CF8)
    },
    animationSpec = tween(600),
    label = "phase_color"
  )

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(20.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    // Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "${type.englishTitle} Meditation",
          color = TextPrimary,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = type.hindiTitle,
          color = CyanNeon,
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold
        )
      }

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(10.dp))
          .background(DarkSurfaceElevated)
          .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(10.dp))
          .padding(horizontal = 10.dp, vertical = 6.dp)
      ) {
        Text(
          text = if (state.isPlaying) "Playing • Audio Active" else "Paused",
          color = if (state.isPlaying) StatusComplete else TextMuted,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }

    // Center Breathing Circle
    Box(
      modifier = Modifier
        .size(310.dp),
      contentAlignment = Alignment.Center
    ) {
      // Ambient Glowing Aura
      Box(
        modifier = Modifier
          .size(290.dp)
          .scale(animatedScale)
          .clip(CircleShape)
          .background(
            Brush.radialGradient(
              colors = listOf(
                phaseColor.copy(alpha = 0.35f),
                phaseColor.copy(alpha = 0.12f),
                Color.Transparent
              )
            )
          )
      )

      // Outer Ring
      Box(
        modifier = Modifier
          .size(240.dp)
          .scale(animatedScale)
          .clip(CircleShape)
          .background(
            Brush.linearGradient(
              listOf(
                DarkSurfaceElevated.copy(alpha = 0.85f),
                Color(0xFF0F172A).copy(alpha = 0.95f)
              )
            )
          )
          .border(2.5.dp, phaseColor.copy(alpha = 0.8f), CircleShape)
      )

      // Inner Text & Phase
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Text(
          text = phase.enName.uppercase(),
          color = phaseColor,
          fontSize = 24.sp,
          fontWeight = FontWeight.ExtraBold,
          letterSpacing = 2.sp
        )
        Text(
          text = phase.hiName,
          color = TextSecondary,
          fontSize = 13.sp,
          fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = "${state.phaseRemainingSeconds}s",
          color = TextPrimary,
          fontSize = 42.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }

    // Real-time Wave Animation Synchronized with Breath Audio Phase & Intensity
    com.example.ui.components.BreathWaveAnimation(
      phase = phase,
      phaseProgress = state.phaseProgressFraction,
      isPlaying = state.isPlaying,
      phaseColor = phaseColor,
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .padding(vertical = 4.dp)
    )

    // Live Instruction & Overall Timer
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.fillMaxWidth()
    ) {
      Text(
        text = state.stageInstruction,
        color = TextPrimary,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp)
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Overall Countdown
      Text(
        text = TimeUtils.formatSecondsToMmSs(state.remainingSeconds),
        color = TextPrimary,
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
      )
      Text(
        text = "Time Remaining (${state.targetDurationSeconds / 60}m total)",
        color = TextMuted,
        fontSize = 11.sp
      )

      Spacer(modifier = Modifier.height(10.dp))

      LinearProgressIndicator(
        progress = { state.totalProgressFraction },
        modifier = Modifier
          .fillMaxWidth(0.8f)
          .height(6.dp)
          .clip(RoundedCornerShape(3.dp)),
        color = CyanNeon,
        trackColor = DarkSurfaceElevated
      )
    }

    // Controls
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 16.dp),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // End Session Button
      IconButton(
        onClick = onStop,
        modifier = Modifier
          .size(54.dp)
          .clip(CircleShape)
          .background(DarkSurfaceElevated)
          .border(1.dp, DarkSurfaceBorder, CircleShape)
          .testTag("end_meditation_button")
      ) {
        Icon(Icons.Default.Stop, contentDescription = "End Session", tint = Color(0xFFF43F5E), modifier = Modifier.size(24.dp))
      }

      // Play / Pause Main Button
      Box(
        modifier = Modifier
          .size(72.dp)
          .clip(CircleShape)
          .background(
            Brush.linearGradient(
              listOf(CyanNeon, VioletNeon)
            )
          )
          .clickable {
            if (state.isPlaying) onPause() else onResume()
          }
          .testTag("toggle_play_meditation_button"),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
          contentDescription = if (state.isPlaying) "Pause" else "Resume",
          tint = Color(0xFF00222A),
          modifier = Modifier.size(34.dp)
        )
      }
    }
  }
}

@Composable
private fun MeditationCompletedView(
  state: com.example.meditation.MeditationState,
  userName: String,
  onDone: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(DarkBackground)
      .padding(24.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Box(
        modifier = Modifier
          .size(90.dp)
          .clip(CircleShape)
          .background(
            Brush.linearGradient(listOf(CyanNeon.copy(alpha = 0.3f), StatusComplete.copy(alpha = 0.3f)))
          )
          .border(2.dp, StatusComplete, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          Icons.Default.Check,
          contentDescription = null,
          tint = StatusComplete,
          modifier = Modifier.size(48.dp)
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      Text(
        text = "साधना पूर्ण हुई ✨",
        color = StatusComplete,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold
      )

      Spacer(modifier = Modifier.height(8.dp))

      val cleanName = userName.trim()
      val personalText = if (cleanName.isNotBlank()) {
        "$cleanName, आपका ${state.type.englishTitle} मेडिटेशन कंप्लीट हो गया है।"
      } else {
        "आपका ${state.type.englishTitle} मेडिटेशन कंप्लीट हो गया है।"
      }

      Text(
        text = personalText,
        color = TextPrimary,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center
      )
      Text(
        text = "अब आप आँखें खोल सकते हैं। मन को शांत और ऊर्जावान महसूस करें।",
        color = TextSecondary,
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 4.dp)
      )

      Spacer(modifier = Modifier.height(24.dp))

      // Summary Card
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(GlassGradient)
          .border(1.dp, CardBorderGradient, RoundedCornerShape(16.dp))
          .padding(16.dp)
      ) {
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Mode", color = TextSecondary, fontSize = 12.sp)
            Text("${state.type.englishTitle} (${state.type.hindiTitle})", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Duration", color = TextSecondary, fontSize = 12.sp)
            Text("${state.targetDurationSeconds / 60} Minutes Completed", color = StatusComplete, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }

      Spacer(modifier = Modifier.height(30.dp))

      Button(
        onClick = onDone,
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .clip(RoundedCornerShape(14.dp))
          .testTag("done_meditation_button"),
        colors = ButtonDefaults.buttonColors(
          containerColor = CyanNeon,
          contentColor = Color(0xFF003038)
        )
      ) {
        Text("Done / वापस जाएँ", fontSize = 15.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
private fun StatPill(title: String, value: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(text = value, color = CyanNeon, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    Text(text = title, color = TextMuted, fontSize = 10.sp)
  }
}

@Composable
private fun CadenceBadge(phaseName: String, seconds: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(text = seconds, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    Text(text = phaseName, color = TextMuted, fontSize = 10.sp)
  }
}

private fun getIconForTag(tag: String): ImageVector {
  return when (tag) {
    "air" -> Icons.Default.Air
    "mind" -> Icons.Default.SelfImprovement
    "focus" -> Icons.Default.CenterFocusStrong
    "relax" -> Icons.Default.Spa
    "sleep" -> Icons.Default.Bedtime
    "morning" -> Icons.Default.WbSunny
    "vision" -> Icons.Default.LightMode
    "sound" -> Icons.Default.GraphicEq
    "heart" -> Icons.Default.Favorite
    "walk" -> Icons.Default.DirectionsWalk
    else -> Icons.Default.SelfImprovement
  }
}
