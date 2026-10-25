package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyNoteEntity
import com.example.ui.theme.CardBorderGradient
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.GlassGradient
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletNeon

data class MoodOption(
  val code: String,
  val emoji: String,
  val label: String
)

val MOOD_OPTIONS = listOf(
  MoodOption("GREAT", "😊", "Great"),
  MoodOption("GOOD", "🙂", "Good"),
  MoodOption("NORMAL", "😐", "Normal"),
  MoodOption("LOW", "😕", "Low"),
  MoodOption("BAD", "😞", "Bad")
)

@Composable
fun DailyReflectionCard(
  dailyNote: DailyNoteEntity?,
  onSave: (note: String, mood: String) -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedMood by remember(dailyNote) { mutableStateOf(dailyNote?.mood ?: "") }
  var noteText by remember(dailyNote) { mutableStateOf(dailyNote?.note ?: "") }
  var isSavedFeedback by remember { mutableStateOf(false) }

  LaunchedEffect(isSavedFeedback) {
    if (isSavedFeedback) {
      kotlinx.coroutines.delay(2000)
      isSavedFeedback = false
    }
  }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .background(GlassGradient)
      .border(1.dp, CardBorderGradient, RoundedCornerShape(20.dp))
      .padding(16.dp)
      .testTag("daily_reflection_card")
  ) {
    Column {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.EditNote,
            contentDescription = null,
            tint = CyanNeon,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "DAILY REFLECTION & MOOD",
            color = CyanNeon,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.8.sp
          )
        }

        if (isSavedFeedback) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Saved", color = Color(0xFF4CAF50), fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Mood Selection Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        MOOD_OPTIONS.forEach { mood ->
          val isSelected = selectedMood.equals(mood.code, ignoreCase = true)
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .background(if (isSelected) VioletNeon.copy(alpha = 0.2f) else DarkSurfaceElevated)
              .border(
                1.dp,
                if (isSelected) VioletNeon else DarkSurfaceBorder,
                RoundedCornerShape(12.dp)
              )
              .clickable {
                selectedMood = mood.code
                onSave(noteText, mood.code)
                isSavedFeedback = true
              }
              .padding(horizontal = 8.dp, vertical = 6.dp)
              .testTag("mood_${mood.code.lowercase()}")
          ) {
            Text(text = mood.emoji, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = mood.label,
              color = if (isSelected) VioletNeon else TextSecondary,
              fontSize = 10.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Note text field
      OutlinedTextField(
        value = noteText,
        onValueChange = { noteText = it },
        placeholder = {
          Text(
            "आज का दिन कैसा रहा? Write a quick reflection...",
            color = TextMuted,
            fontSize = 12.sp
          )
        },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("daily_note_input"),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = DarkSurface,
          unfocusedContainerColor = DarkSurface,
          focusedBorderColor = CyanNeon,
          unfocusedBorderColor = DarkSurfaceBorder,
          focusedTextColor = TextPrimary,
          unfocusedTextColor = TextPrimary
        ),
        shape = RoundedCornerShape(12.dp),
        minLines = 2,
        maxLines = 4
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Save note button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
      ) {
        Button(
          onClick = {
            onSave(noteText, selectedMood)
            isSavedFeedback = true
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = CyanNeon,
            contentColor = DarkBackground
          ),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .height(34.dp)
            .testTag("save_daily_note_button")
        ) {
          Text(
            text = "Save Reflection",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}
