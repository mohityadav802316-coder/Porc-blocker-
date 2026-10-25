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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import com.example.data.model.ReflectionCategory
import com.example.data.model.ReflectionEntity
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.StatusComplete
import com.example.ui.theme.StatusMissed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletNeon
import com.example.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReflectionBottomSheet(
  initialReflection: ReflectionEntity? = null,
  selectedDate: String,
  onDismiss: () -> Unit,
  onSave: (id: Long, title: String, description: String, date: String, category: ReflectionCategory, tags: String) -> Unit,
  onDelete: ((Long) -> Unit)? = null
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  var title by remember { mutableStateOf(initialReflection?.title ?: "") }
  var description by remember { mutableStateOf(initialReflection?.description ?: "") }
  var category by remember { mutableStateOf(initialReflection?.category ?: ReflectionCategory.OBSERVATION) }
  var tags by remember { mutableStateOf(initialReflection?.tags ?: "") }
  var showDeleteConfirmation by remember { mutableStateOf(false) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = DarkSurface,
    scrimColor = Color.Black.copy(alpha = 0.65f),
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
  ) {
    if (showDeleteConfirmation && initialReflection != null && onDelete != null) {
      androidx.compose.material3.AlertDialog(
        onDismissRequest = { showDeleteConfirmation = false },
        containerColor = DarkSurfaceElevated,
        shape = RoundedCornerShape(18.dp),
        title = { Text("Delete Reflection?", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = { Text("Are you sure you want to delete this reflection entry? This action cannot be undone.", color = TextSecondary, fontSize = 13.sp) },
        confirmButton = {
          Button(
            onClick = {
              showDeleteConfirmation = false
              onDelete(initialReflection.id)
              onDismiss()
            },
            colors = ButtonDefaults.buttonColors(containerColor = StatusMissed, contentColor = Color.White)
          ) {
            Text("Delete")
          }
        },
        dismissButton = {
          androidx.compose.material3.TextButton(onClick = { showDeleteConfirmation = false }) {
            Text("Cancel", color = TextSecondary)
          }
        }
      )
    }

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp)
        .padding(bottom = 36.dp)
        .testTag("reflection_bottom_sheet")
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = if (initialReflection != null) "Edit Reflection" else "New Reflection Log",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Review lessons, actions, and observations",
            color = TextSecondary,
            fontSize = 12.sp
          )
        }

        if (initialReflection != null && onDelete != null) {
          OutlinedButton(
            onClick = {
              showDeleteConfirmation = true
            },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusMissed),
            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(StatusMissed.copy(alpha = 0.5f))),
            shape = RoundedCornerShape(10.dp)
          ) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Delete", fontSize = 12.sp)
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Category Selector: 3 Explicit Categories
      Text(
        text = "Category (Select One)",
        color = TextSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold
      )
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        ReflectionCategoryTab(
          category = ReflectionCategory.MISTAKE,
          isSelected = category == ReflectionCategory.MISTAKE,
          accentColor = StatusMissed,
          onClick = { category = ReflectionCategory.MISTAKE },
          modifier = Modifier.weight(1f)
        )
        ReflectionCategoryTab(
          category = ReflectionCategory.GOOD_DEED,
          isSelected = category == ReflectionCategory.GOOD_DEED,
          accentColor = StatusComplete,
          onClick = { category = ReflectionCategory.GOOD_DEED },
          modifier = Modifier.weight(1f)
        )
        ReflectionCategoryTab(
          category = ReflectionCategory.OBSERVATION,
          isSelected = category == ReflectionCategory.OBSERVATION,
          accentColor = CyanNeon,
          onClick = { category = ReflectionCategory.OBSERVATION },
          modifier = Modifier.weight(1f)
        )
      }

      Spacer(modifier = Modifier.height(18.dp))

      // Title Input
      OutlinedTextField(
        value = title,
        onValueChange = { title = it },
        label = { Text("Title / Summary") },
        placeholder = { Text("e.g., Stayed up late, Reached workout PR...") },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("reflection_title_input"),
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

      // Description
      OutlinedTextField(
        value = description,
        onValueChange = { description = it },
        label = { Text("Description & Insights") },
        placeholder = { Text("What caused it? How can you optimize or repeat it next time?") },
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
        minLines = 3,
        maxLines = 6
      )

      Spacer(modifier = Modifier.height(14.dp))

      // Tags Input
      OutlinedTextField(
        value = tags,
        onValueChange = { tags = it },
        label = { Text("Tags (Comma-separated)") },
        placeholder = { Text("e.g., Health, Discipline, Sleep, Work") },
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
            onSave(
              initialReflection?.id ?: 0L,
              title.trim(),
              description.trim(),
              initialReflection?.date ?: selectedDate,
              category,
              tags.trim()
            )
            onDismiss()
          }
        },
        enabled = title.isNotBlank(),
        colors = ButtonDefaults.buttonColors(
          containerColor = CyanNeon,
          contentColor = Color(0xFF00363D)
        ),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("save_reflection_button"),
        shape = RoundedCornerShape(14.dp)
      ) {
        Text(
          text = if (initialReflection != null) "Update Reflection" else "Save Reflection",
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}

@Composable
private fun ReflectionCategoryTab(
  category: ReflectionCategory,
  isSelected: Boolean,
  accentColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .height(58.dp)
      .clip(RoundedCornerShape(12.dp))
      .background(if (isSelected) accentColor.copy(alpha = 0.2f) else DarkSurfaceElevated)
      .border(
        width = if (isSelected) 1.5.dp else 1.dp,
        color = if (isSelected) accentColor else DarkSurfaceBorder,
        shape = RoundedCornerShape(12.dp)
      )
      .clickable { onClick() }
      .padding(horizontal = 4.dp, vertical = 6.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = category.iconPrefix,
        fontSize = 14.sp
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = category.label,
        color = if (isSelected) accentColor else TextSecondary,
        fontSize = 11.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        maxLines = 1
      )
    }
  }
}
