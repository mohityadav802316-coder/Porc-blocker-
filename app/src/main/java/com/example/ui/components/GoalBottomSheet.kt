package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GoalEntity
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.StatusMissed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalBottomSheet(
  initialGoal: GoalEntity? = null,
  onDismiss: () -> Unit,
  onSave: (id: Long, title: String, description: String, progress: Int, deadline: String?, isCompleted: Boolean) -> Unit,
  onDelete: ((Long) -> Unit)? = null
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  var title by remember { mutableStateOf(initialGoal?.title ?: "") }
  var description by remember { mutableStateOf(initialGoal?.description ?: "") }
  var progressFloat by remember { mutableFloatStateOf((initialGoal?.progress ?: 0).toFloat()) }
  var deadline by remember { mutableStateOf(initialGoal?.deadline ?: "") }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = DarkSurface,
    scrimColor = Color.Black.copy(alpha = 0.65f),
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp)
        .padding(bottom = 36.dp)
        .testTag("goal_bottom_sheet")
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = if (initialGoal != null) "Edit Goal" else "Define New Goal",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Track high-impact personal milestones",
            color = TextSecondary,
            fontSize = 12.sp
          )
        }

        if (initialGoal != null && onDelete != null) {
          OutlinedButton(
            onClick = {
              onDelete(initialGoal.id)
              onDismiss()
            },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusMissed),
            shape = RoundedCornerShape(10.dp)
          ) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Delete", fontSize = 12.sp)
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      OutlinedTextField(
        value = title,
        onValueChange = { title = it },
        label = { Text("Goal Title") },
        placeholder = { Text("e.g., Read 12 Books, Run Half Marathon") },
        modifier = Modifier.fillMaxWidth().testTag("goal_title_input"),
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = TextPrimary,
          unfocusedTextColor = TextPrimary,
          focusedBorderColor = CyanNeon,
          unfocusedBorderColor = DarkSurfaceBorder,
          focusedContainerColor = DarkSurfaceElevated,
          unfocusedContainerColor = DarkSurfaceElevated
        ),
        shape = RoundedCornerShape(14.dp),
        singleLine = true
      )

      Spacer(modifier = Modifier.height(14.dp))

      OutlinedTextField(
        value = description,
        onValueChange = { description = it },
        label = { Text("Description (Optional)") },
        placeholder = { Text("Core motivations and measurable benchmarks") },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = TextPrimary,
          unfocusedTextColor = TextPrimary,
          focusedBorderColor = CyanNeon,
          unfocusedBorderColor = DarkSurfaceBorder,
          focusedContainerColor = DarkSurfaceElevated,
          unfocusedContainerColor = DarkSurfaceElevated
        ),
        shape = RoundedCornerShape(14.dp),
        maxLines = 3
      )

      Spacer(modifier = Modifier.height(14.dp))

      // Progress Slider (0..100%)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Current Progress", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text("${progressFloat.roundToInt()}%", color = CyanNeon, fontSize = 15.sp, fontWeight = FontWeight.Black)
      }
      Slider(
        value = progressFloat,
        onValueChange = { progressFloat = it },
        valueRange = 0f..100f,
        steps = 19,
        colors = SliderDefaults.colors(
          thumbColor = CyanNeon,
          activeTrackColor = CyanNeon,
          inactiveTrackColor = DarkSurfaceBorder
        )
      )

      Spacer(modifier = Modifier.height(10.dp))

      OutlinedTextField(
        value = deadline,
        onValueChange = { deadline = it },
        label = { Text("Target Deadline (Optional)") },
        placeholder = { Text("e.g., Dec 31, 2026 or Q4 2026") },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = TextPrimary,
          unfocusedTextColor = TextPrimary,
          focusedBorderColor = CyanNeon,
          unfocusedBorderColor = DarkSurfaceBorder,
          focusedContainerColor = DarkSurfaceElevated,
          unfocusedContainerColor = DarkSurfaceElevated
        ),
        shape = RoundedCornerShape(14.dp),
        singleLine = true
      )

      Spacer(modifier = Modifier.height(24.dp))

      Button(
        onClick = {
          if (title.isNotBlank()) {
            val progressInt = progressFloat.roundToInt()
            onSave(
              initialGoal?.id ?: 0L,
              title.trim(),
              description.trim(),
              progressInt,
              deadline.trim().ifEmpty { null },
              progressInt >= 100
            )
            onDismiss()
          }
        },
        enabled = title.isNotBlank(),
        colors = ButtonDefaults.buttonColors(
          containerColor = CyanNeon,
          contentColor = Color(0xFF00363D)
        ),
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_goal_button"),
        shape = RoundedCornerShape(14.dp)
      ) {
        Text(
          text = if (initialGoal != null) "Update Goal" else "Save Goal",
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}
