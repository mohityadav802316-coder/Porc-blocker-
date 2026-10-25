package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BreathPhase
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.VioletNeon
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

/**
 * Real-time dynamic audio wave animation synchronized with breath phases:
 * - INHALE: Swells from soft to full crescendo matching air inhalation audio
 * - EXHALE: Starts prominent and decrescendos smoothly into silence matching exhalation audio
 * - HOLD & REST: Settles into a serene, tranquil flat baseline representing complete silence
 * - PAUSED: Gracefully freezes in place
 */
@Composable
fun BreathWaveAnimation(
  phase: BreathPhase,
  phaseProgress: Float,
  isPlaying: Boolean,
  phaseColor: Color,
  modifier: Modifier = Modifier
) {
  // Compute acoustic intensity envelope matching procedural audio engine in BreathAudioPlayer
  val rawIntensity = when (phase) {
    BreathPhase.INHALE -> {
      val progress = phaseProgress.coerceIn(0f, 1f)
      val attack = (progress / 0.15f).coerceAtMost(1f)
      val fadeOut = if (progress > 0.85f) (1f - progress) / 0.15f else 1f
      (progress.pow(1.3f) * attack * fadeOut).coerceIn(0f, 1f)
    }
    BreathPhase.EXHALE -> {
      val progress = phaseProgress.coerceIn(0f, 1f)
      val attack = (progress / 0.10f).coerceAtMost(1f)
      val decay = (1f - progress).pow(1.2f)
      (decay * attack).coerceIn(0f, 1f)
    }
    BreathPhase.HOLD, BreathPhase.REST -> 0f
  }

  // Smooth out intensity changes to eliminate visual snapping
  val smoothedIntensity by animateFloatAsState(
    targetValue = if (isPlaying) rawIntensity else 0.05f,
    animationSpec = tween(durationMillis = 200, easing = LinearEasing),
    label = "breath_wave_intensity"
  )

  // Infinite flowing phase offset for real-time wave travel
  val infiniteTransition = rememberInfiniteTransition(label = "breath_wave_flow")
  val wavePhaseOffset by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = if (isPlaying && smoothedIntensity > 0.01f) (2 * PI).toFloat() else 0f,
    animationSpec = infiniteRepeatable(
      animation = tween(
        durationMillis = if (phase == BreathPhase.INHALE) 2200 else 2800,
        easing = LinearEasing
      ),
      repeatMode = RepeatMode.Restart
    ),
    label = "breath_wave_phase"
  )

  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(DarkSurfaceElevated.copy(alpha = 0.75f))
      .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(16.dp))
      .padding(horizontal = 14.dp, vertical = 10.dp)
  ) {
    Column {
      // Header status indicator
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            Icons.Default.GraphicEq,
            contentDescription = "Wave Visualizer",
            tint = if (smoothedIntensity > 0.05f) phaseColor else TextMuted,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = when (phase) {
              BreathPhase.INHALE -> "BREATH IN • SOUND CRESCENDO"
              BreathPhase.EXHALE -> "BREATH OUT • SOUND RELEASE"
              BreathPhase.HOLD -> "HOLD • COMPLETE SILENCE"
              BreathPhase.REST -> "REST • COMPLETE STILLNESS"
            },
            color = if (smoothedIntensity > 0.05f) phaseColor else TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
          )
        }

        // Live intensity percentage or silence badge
        Box(
          modifier = Modifier
            .clip(CircleShape)
            .background(
              if (smoothedIntensity > 0.05f) phaseColor.copy(alpha = 0.15f) else Color.Transparent
            )
            .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
          Text(
            text = if (smoothedIntensity > 0.02f) {
              "${(smoothedIntensity * 100).toInt()}% Intensity"
            } else {
              "0% Silence"
            },
            color = if (smoothedIntensity > 0.05f) phaseColor else TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Real-time Canvas Waveform Visualizer
      Canvas(
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
      ) {
        val width = size.width
        val height = size.height
        val midY = height / 2f

        if (width <= 0f || height <= 0f) return@Canvas

        // Amplitude is directly driven by the sound's real-time intensity envelope
        val maxWaveAmplitude = (height * 0.42f) * smoothedIntensity

        // 1. Draw calm tranquil baseline
        drawLine(
          color = Color(0xFF1E293B),
          start = Offset(0f, midY),
          end = Offset(width, midY),
          strokeWidth = 1.5f
        )

        // 2. Primary Wave with glowing fill underneath
        val primaryPath = Path()
        val fillPath = Path()
        fillPath.moveTo(0f, height)
        primaryPath.moveTo(0f, midY)
        fillPath.lineTo(0f, midY)

        val steps = 80
        val dx = width / steps

        for (i in 0..steps) {
          val x = i * dx
          val normX = x / width

          // Windowing envelope so wave tapers smoothly at edges
          val window = sin(normX * PI).toFloat().pow(1.2f)

          // Multi-harmonic natural airflow curve
          val harmonic1 = sin(normX * 4 * PI + wavePhaseOffset).toFloat()
          val harmonic2 = 0.35f * sin(normX * 8 * PI - wavePhaseOffset * 1.4f).toFloat()
          val harmonic3 = 0.15f * sin(normX * 12 * PI + wavePhaseOffset * 0.7f).toFloat()

          val combinedWave = (harmonic1 + harmonic2 + harmonic3) * maxWaveAmplitude * window
          val y = midY + combinedWave

          if (i == 0) {
            primaryPath.moveTo(x, y)
            fillPath.lineTo(x, y)
          } else {
            primaryPath.lineTo(x, y)
            fillPath.lineTo(x, y)
          }
        }

        fillPath.lineTo(width, height)
        fillPath.close()

        // Draw translucent gradient under the primary wave
        drawPath(
          path = fillPath,
          brush = Brush.verticalGradient(
            colors = listOf(
              phaseColor.copy(alpha = 0.25f * smoothedIntensity),
              Color.Transparent
            ),
            startY = 0f,
            endY = height
          )
        )

        // Draw the primary wave line
        drawPath(
          path = primaryPath,
          color = phaseColor.copy(alpha = (0.3f + 0.7f * smoothedIntensity).coerceIn(0.2f, 1f)),
          style = Stroke(
            width = if (smoothedIntensity > 0.05f) 2.5f else 1.2f,
            cap = StrokeCap.Round
          )
        )

        // 3. Secondary subtle harmonic counter-wave
        if (smoothedIntensity > 0.08f) {
          val secondaryPath = Path()
          val secAmplitude = maxWaveAmplitude * 0.65f

          for (i in 0..steps) {
            val x = i * dx
            val normX = x / width
            val window = sin(normX * PI).toFloat()
            val secHarmonic = sin(normX * 5 * PI - wavePhaseOffset * 1.2f + PI / 3).toFloat()
            val y = midY + (secHarmonic * secAmplitude * window)

            if (i == 0) {
              secondaryPath.moveTo(x, y)
            } else {
              secondaryPath.lineTo(x, y)
            }
          }

          drawPath(
            path = secondaryPath,
            color = VioletNeon.copy(alpha = 0.55f * smoothedIntensity),
            style = Stroke(width = 1.5f, cap = StrokeCap.Round)
          )
        }
      }
    }
  }
}
