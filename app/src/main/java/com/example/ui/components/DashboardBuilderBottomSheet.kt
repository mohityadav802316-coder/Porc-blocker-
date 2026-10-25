package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.example.data.model.DashboardPreset
import com.example.data.model.DashboardSectionId
import com.example.ui.theme.ChronoSerifFamily
import com.example.ui.theme.DustyRose
import com.example.ui.theme.GoldBrass
import com.example.ui.theme.GoldHighlight
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCharcoal
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.SageGreen
import com.example.ui.theme.WarmMuted
import com.example.ui.theme.WarmOffWhite
import com.example.ui.theme.WarmParchment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardBuilderBottomSheet(
  sheetState: SheetState,
  currentPresetKey: String,
  currentOrderKeys: String,
  currentDisabledKeys: String,
  onApply: (preset: DashboardPreset, ordered: List<DashboardSectionId>, disabled: Set<DashboardSectionId>) -> Unit,
  onResetDefault: () -> Unit,
  onDismiss: () -> Unit
) {
  // Parse initial order
  val initialOrderedList = remember(currentOrderKeys) {
    val keys = currentOrderKeys.split(",").map { it.trim() }.filter { it.isNotBlank() }
    val mapped = keys.mapNotNull { DashboardSectionId.fromKey(it) }.toMutableList()
    // Add any missing sections at the end
    DashboardSectionId.defaultOrderedList.forEach { sec ->
      if (!mapped.contains(sec)) mapped.add(sec)
    }
    mapped
  }

  val initialDisabledSet = remember(currentDisabledKeys) {
    currentDisabledKeys.split(",").map { it.trim() }
      .mapNotNull { DashboardSectionId.fromKey(it) }
      .toSet()
  }

  val sections = remember { mutableStateListOf<DashboardSectionId>().apply { addAll(initialOrderedList) } }
  val disabledSections = remember { mutableStateListOf<DashboardSectionId>().apply { addAll(initialDisabledSet) } }
  var selectedPreset by remember { mutableStateOf(DashboardPreset.fromKey(currentPresetKey)) }
  var showPreviewDialog by remember { mutableStateOf(false) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = ObsidianElevated,
    scrimColor = Color.Black.copy(alpha = 0.75f),
    dragHandle = null
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 16.dp, bottom = 28.dp)
        .testTag("dashboard_builder_sheet")
    ) {
      // Header
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(GoldBrass.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.DashboardCustomize,
              contentDescription = null,
              tint = GoldBrass,
              modifier = Modifier.size(20.dp)
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "Personal Dashboard Builder",
              color = WarmOffWhite,
              fontFamily = ChronoSerifFamily,
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Customize cards, order & layout presets",
              color = WarmMuted,
              fontSize = 12.sp
            )
          }
        }

        IconButton(
          onClick = onDismiss,
          modifier = Modifier.size(32.dp)
        ) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = WarmMuted)
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Section 1: Presets Selector
      Column(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "CHOOSE A PRESET",
            color = GoldBrass,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.0.sp
          )
          Text(
            text = "Active: ${selectedPreset.titleEn}",
            color = WarmParchment,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
          contentPadding = PaddingValues(horizontal = 20.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(DashboardPreset.entries) { preset ->
            val isSelected = selectedPreset == preset
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (isSelected) GoldBrass.copy(alpha = 0.2f) else ObsidianCard)
                .border(
                  width = if (isSelected) 1.2.dp else 0.8.dp,
                  color = if (isSelected) GoldBrass else ObsidianBorder,
                  shape = RoundedCornerShape(12.dp)
                )
                .clickable {
                  selectedPreset = preset
                  if (preset != DashboardPreset.CUSTOM) {
                    sections.clear()
                    sections.addAll(preset.sections)
                    DashboardSectionId.defaultOrderedList.forEach { s ->
                      if (!sections.contains(s)) sections.add(s)
                    }
                    disabledSections.clear()
                    DashboardSectionId.defaultOrderedList.forEach { s ->
                      if (!preset.sections.contains(s)) disabledSections.add(s)
                    }
                  }
                }
                .padding(horizontal = 14.dp, vertical = 9.dp)
                .testTag("preset_${preset.key.lowercase()}")
            ) {
              Column {
                Text(
                  text = preset.titleEn,
                  color = if (isSelected) GoldHighlight else WarmOffWhite,
                  fontSize = 13.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                )
                Text(
                  text = "${preset.sections.size} sections",
                  color = if (isSelected) GoldBrass.copy(alpha = 0.8f) else WarmMuted,
                  fontSize = 10.sp
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Section 2: Reorder & Show/Hide List
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "SECTIONS & ORDER",
          color = GoldBrass,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.0.sp
        )
        Text(
          text = "${sections.size - disabledSections.size} of ${sections.size} Visible",
          color = WarmParchment,
          fontSize = 11.sp
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 280.dp)
          .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        itemsIndexed(sections, key = { _, s -> s.key }) { index, section ->
          val isEnabled = !disabledSections.contains(section)

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(if (isEnabled) ObsidianCard else ObsidianCharcoal.copy(alpha = 0.5f))
              .border(
                width = 0.8.dp,
                color = if (isEnabled) ObsidianBorder else ObsidianBorder.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
              )
              .padding(horizontal = 12.dp, vertical = 8.dp)
              .testTag("dash_item_${section.key}")
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              // Left: Reorder Arrows & Title
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
              ) {
                // Up Arrow
                IconButton(
                  onClick = {
                    if (index > 0) {
                      val item = sections.removeAt(index)
                      sections.add(index - 1, item)
                      selectedPreset = DashboardPreset.CUSTOM
                    }
                  },
                  enabled = index > 0,
                  modifier = Modifier.size(26.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Move Up",
                    tint = if (index > 0) WarmParchment else ObsidianBorder,
                    modifier = Modifier.size(18.dp)
                  )
                }

                // Down Arrow
                IconButton(
                  onClick = {
                    if (index < sections.size - 1) {
                      val item = sections.removeAt(index)
                      sections.add(index + 1, item)
                      selectedPreset = DashboardPreset.CUSTOM
                    }
                  },
                  enabled = index < sections.size - 1,
                  modifier = Modifier.size(26.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Move Down",
                    tint = if (index < sections.size - 1) WarmParchment else ObsidianBorder,
                    modifier = Modifier.size(18.dp)
                  )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Icon(
                  imageVector = section.getIcon(),
                  contentDescription = null,
                  tint = if (isEnabled) GoldBrass else WarmMuted,
                  modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                  Text(
                    text = section.titleEn,
                    color = if (isEnabled) WarmOffWhite else WarmMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                  )
                  Text(
                    text = section.description,
                    color = WarmMuted,
                    fontSize = 10.sp,
                    maxLines = 1
                  )
                }
              }

              // Right: Toggle Switch
              Switch(
                checked = isEnabled,
                onCheckedChange = { checked ->
                  if (checked) {
                    disabledSections.remove(section)
                  } else {
                    if (!disabledSections.contains(section)) {
                      disabledSections.add(section)
                    }
                  }
                  selectedPreset = DashboardPreset.CUSTOM
                },
                colors = SwitchDefaults.colors(
                  checkedThumbColor = ObsidianCharcoal,
                  checkedTrackColor = GoldBrass,
                  uncheckedThumbColor = WarmMuted,
                  uncheckedTrackColor = ObsidianCard
                )
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Bottom Action Bar: Reset, Preview Changes, and Apply
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedButton(
          onClick = {
            onResetDefault()
            sections.clear()
            sections.addAll(DashboardSectionId.defaultOrderedList)
            disabledSections.clear()
            selectedPreset = DashboardPreset.CUSTOM
          },
          modifier = Modifier.weight(0.9f),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = WarmParchment)
        ) {
          Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Reset", fontSize = 11.sp)
        }

        // Preview Changes Experience
        OutlinedButton(
          onClick = { showPreviewDialog = true },
          modifier = Modifier.weight(1.1f),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldBrass)
        ) {
          Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Preview", fontSize = 11.sp)
        }

        Button(
          onClick = {
            onApply(selectedPreset, sections.toList(), disabledSections.toSet())
            onDismiss()
          },
          modifier = Modifier.weight(1.3f),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = GoldBrass)
        ) {
          Text("Apply", color = ObsidianCharcoal, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
      }
    }
  }

  // Interactive Preview Changes Experience Modal
  if (showPreviewDialog) {
    val activeSections = sections.filter { !disabledSections.contains(it) }

    AlertDialog(
      onDismissRequest = { showPreviewDialog = false },
      containerColor = ObsidianElevated,
      title = {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "PREVIEW CHANGES",
              color = GoldBrass,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.0.sp
            )
            Text(
              text = "${selectedPreset.titleEn} Dashboard Layout",
              color = WarmOffWhite,
              fontFamily = ChronoSerifFamily,
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold
            )
          }
          IconButton(onClick = { showPreviewDialog = false }, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = WarmMuted)
          }
        }
      },
      text = {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text(
            text = "This layout will show ${activeSections.size} active sections in this precise order on your Home Screen:",
            color = WarmParchment,
            fontSize = 12.sp
          )

          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 240.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            itemsIndexed(activeSections) { index, sec ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .background(ObsidianCard)
                  .border(0.6.dp, ObsidianBorder, RoundedCornerShape(8.dp))
                  .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "${index + 1}.",
                  color = GoldBrass,
                  fontFamily = ChronoSerifFamily,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(sec.getIcon(), contentDescription = null, tint = GoldBrass, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = sec.titleEn,
                  color = WarmOffWhite,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            showPreviewDialog = false
            onApply(selectedPreset, sections.toList(), disabledSections.toSet())
            onDismiss()
          },
          colors = ButtonDefaults.buttonColors(containerColor = GoldBrass),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text("Confirm & Apply", color = ObsidianCharcoal, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
      },
      dismissButton = {
        TextButton(onClick = { showPreviewDialog = false }) {
          Text("Back to Reorder", color = WarmMuted, fontSize = 11.sp)
        }
      }
    )
  }
}
