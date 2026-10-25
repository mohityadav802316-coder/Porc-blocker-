package com.example.ui.components

import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import com.example.focus.FocusModeManager
import com.example.ui.theme.GoldBrass
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldHighlight
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCharcoal
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.WarmMuted
import com.example.ui.theme.WarmOffWhite
import com.example.ui.theme.WarmParchment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledAppItem(
  val packageName: String,
  val label: String,
  val isCoreAlwaysAllowed: Boolean = false,
  val isAllowed: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllowedAppsDialog(
  onDismiss: () -> Unit,
  onSave: (Set<String>) -> Unit
) {
  val context = LocalContext.current
  val pm = context.packageManager

  var searchQuery by remember { mutableStateOf("") }
  var installedApps by remember { mutableStateOf<List<InstalledAppItem>>(emptyList()) }
  var allowedSet by remember { mutableStateOf(FocusModeManager.getAllowedPackages(context).toMutableSet()) }
  var isLoading by remember { mutableStateOf(true) }
  val isStrictLock = remember { FocusModeManager.isStrictLockActive(context) }

  val corePackages = remember { FocusModeManager.getCoreWhitelistedPackages(context) }

  // Query installed apps in background
  LaunchedEffect(Unit) {
    withContext(Dispatchers.IO) {
      val intent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
      }
      val resolveList = pm.queryIntentActivities(intent, 0)
      val appItems = resolveList.mapNotNull { resolveInfo ->
        val pkg = resolveInfo.activityInfo.packageName
        val label = resolveInfo.loadLabel(pm).toString()
        val isCore = corePackages.contains(pkg)
        InstalledAppItem(
          packageName = pkg,
          label = label,
          isCoreAlwaysAllowed = isCore,
          isAllowed = isCore || allowedSet.contains(pkg)
        )
      }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }

      withContext(Dispatchers.Main) {
        installedApps = appItems
        isLoading = false
      }
    }
  }

  val filteredApps = remember(installedApps, searchQuery, allowedSet) {
    val q = searchQuery.trim().lowercase()
    installedApps.filter { item ->
      q.isBlank() || item.label.lowercase().contains(q) || item.packageName.lowercase().contains(q)
    }.map { item ->
      item.copy(isAllowed = item.isCoreAlwaysAllowed || allowedSet.contains(item.packageName))
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.94f)
        .fillMaxHeight(0.85f)
        .clip(RoundedCornerShape(24.dp))
        .border(1.2.dp, GoldBrass.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
      color = ObsidianCharcoal
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
      ) {
        // Top Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isStrictLock) Color(0xFFEF4444).copy(alpha = 0.2f) else GoldBrass.copy(alpha = 0.2f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Shield, contentDescription = null, tint = if (isStrictLock) Color(0xFFEF4444) else GoldBrass, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "अनुमत ऐप्स (Allowed Apps)",
                color = WarmOffWhite,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = if (isStrictLock) "🔒 लॉक के दौरान संपादन बंद है" else "${allowedSet.size} ऐप्स फोकस सत्र में चालू रहेंगे",
                color = if (isStrictLock) Color(0xFFFF6B6B) else WarmParchment,
                fontSize = 12.sp
              )
            }
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = WarmMuted)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isStrictLock) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(Color(0xFF3B1515))
              .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f), RoundedCornerShape(10.dp))
              .padding(10.dp)
          ) {
            Text(
              text = "🔒 सख्त सुरक्षा लॉक सक्रिय है। सुरक्षा नियमों के अनुसार लॉक के दौरान अनुमत ऐप्स में कोई बदलाव नहीं किया जा सकता।",
              color = Color(0xFFFFB4AB),
              fontSize = 11.sp,
              lineHeight = 15.sp
            )
          }
          Spacer(modifier = Modifier.height(10.dp))
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search Bar
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("ऐप का नाम खोजें...", color = WarmMuted, fontSize = 13.sp) },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GoldBrass, modifier = Modifier.size(18.dp)) },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
          shape = RoundedCornerShape(12.dp),
          colors = TextFieldDefaults.colors(
            focusedContainerColor = ObsidianElevated,
            unfocusedContainerColor = ObsidianElevated,
            focusedTextColor = WarmOffWhite,
            unfocusedTextColor = WarmOffWhite,
            focusedIndicatorColor = GoldBrass,
            unfocusedIndicatorColor = ObsidianBorder
          )
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Info banner explaining core apps
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(GoldBrass.copy(alpha = 0.08f))
            .border(0.8.dp, GoldBrass.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
          Text(
            text = "फ़ोन/डायलर, होम स्क्रीन और Life Tracker स्वतः अनुमत हैं ताकि आपातकालीन कॉल कभी न रुकें।",
            color = WarmParchment,
            fontSize = 11.sp,
            lineHeight = 15.sp
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // App List
        if (isLoading) {
          Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("ऐप्स लोड हो रहे हैं...", color = WarmMuted, fontSize = 13.sp)
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(filteredApps, key = { it.packageName }) { appItem ->
              val isCore = appItem.isCoreAlwaysAllowed
              val isChecked = appItem.isAllowed

              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(14.dp))
                  .background(if (isChecked) ObsidianElevated else ObsidianCard)
                  .border(
                    width = 0.8.dp,
                    color = if (isChecked) GoldBrass.copy(alpha = 0.35f) else ObsidianBorder,
                    shape = RoundedCornerShape(14.dp)
                  )
                  .clickable(enabled = !isCore) {
                    val updated = allowedSet.toMutableSet()
                    if (updated.contains(appItem.packageName)) {
                      updated.remove(appItem.packageName)
                    } else {
                      updated.add(appItem.packageName)
                    }
                    allowedSet = updated
                  }
                  .padding(horizontal = 14.dp, vertical = 10.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                  ) {
                    // App Icon
                    val iconDrawable = remember(appItem.packageName) {
                      try {
                        pm.getApplicationIcon(appItem.packageName)
                      } catch (_: Exception) {
                        null
                      }
                    }
                    if (iconDrawable != null) {
                      val bitmap = remember(iconDrawable) { iconDrawable.toBitmap(96, 96) }
                      Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(36.dp)
                      )
                    } else {
                      Box(
                        modifier = Modifier
                          .size(36.dp)
                          .clip(CircleShape)
                          .background(ObsidianBorder)
                      )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                      Text(
                        text = appItem.label,
                        color = if (isChecked) WarmOffWhite else WarmMuted,
                        fontSize = 14.sp,
                        fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal
                      )
                      Text(
                        text = if (isCore) "हमेशा अनुमत (सिस्टम)" else appItem.packageName,
                        color = if (isCore) GoldHighlight else WarmMuted.copy(alpha = 0.6f),
                        fontSize = 10.sp
                      )
                    }
                  }

                  if (isCore) {
                    Icon(
                      imageVector = Icons.Default.Check,
                      contentDescription = "Always Allowed",
                      tint = GoldBrass,
                      modifier = Modifier.size(20.dp)
                    )
                  } else {
                    Switch(
                      checked = isChecked,
                      enabled = !isStrictLock,
                      onCheckedChange = { checked ->
                        val updated = allowedSet.toMutableSet()
                        if (checked) {
                          updated.add(appItem.packageName)
                        } else {
                          updated.remove(appItem.packageName)
                        }
                        allowedSet = updated
                      },
                      colors = SwitchDefaults.colors(
                        checkedThumbColor = GoldBrass,
                        checkedTrackColor = GoldBrass.copy(alpha = 0.35f),
                        uncheckedThumbColor = WarmMuted,
                        uncheckedTrackColor = ObsidianBorder
                      )
                    )
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Save Button
        Button(
          onClick = {
            FocusModeManager.setAllowedPackages(context, allowedSet)
            onSave(allowedSet)
            onDismiss()
          },
          enabled = !isStrictLock,
          colors = ButtonDefaults.buttonColors(
            containerColor = GoldBrass,
            disabledContainerColor = ObsidianElevated
          ),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("save_allowed_apps_button")
        ) {
          Text(
            text = if (isStrictLock) "🚫 सख्त लॉक में बदलाव वर्जित" else "अनुमत ऐप्स सहेजें (${allowedSet.size})",
            color = if (isStrictLock) WarmMuted else ObsidianCharcoal,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
          )
        }
      }
    }
  }
}
