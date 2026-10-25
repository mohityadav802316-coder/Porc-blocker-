package com.example.ui.components

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.focus.AdultBlocklistManager
import com.example.focus.FocusModeManager
import com.example.focus.LocalDnsVpnService
import com.example.ui.theme.GoldBrass
import com.example.ui.theme.GoldHighlight
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCharcoal
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.WarmMuted
import com.example.ui.theme.WarmOffWhite
import com.example.ui.theme.WarmParchment

/**
 * Dialog for managing Adult Blocklist (Domains, Keywords) and Safe-Words (False-Positive Whitelist),
 * plus On-Device Local DNS Filter VPN controls.
 */
@Composable
fun AdultBlocklistDialog(
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val isStrictLock = remember { FocusModeManager.isStrictLockActive(context) }

  var selectedTabIndex by remember { mutableIntStateOf(0) }
  val tabTitles = listOf("डोमेन (Domains)", "शब्द (Keywords)", "सुरक्षित शब्द (Safe)", "DNS VPN")

  var customDomains by remember { mutableStateOf(AdultBlocklistManager.getCustomBlockedDomains(context).toList().sorted()) }
  var customKeywords by remember { mutableStateOf(AdultBlocklistManager.getCustomBlockedKeywords(context).toList().sorted()) }
  var customSafeWords by remember { mutableStateOf(AdultBlocklistManager.getCustomSafeWords(context).toList().sorted()) }

  var inputDomain by remember { mutableStateOf("") }
  var inputKeyword by remember { mutableStateOf("") }
  var inputSafeWord by remember { mutableStateOf("") }

  val isVpnActive by LocalDnsVpnService.isVpnRunning.collectAsState()

  var showPinDialog by remember { mutableStateOf(false) }
  var pendingPinAction by remember { mutableStateOf<(() -> Unit)?>(null) }
  var pendingActionTitle by remember { mutableStateOf("") }

  fun executeWithPin(actionTitle: String, action: () -> Unit) {
    if (com.example.focus.AccountabilityPinManager.isPinSet(context) &&
      !com.example.focus.AlwaysOnSecurityManager.isTemporarilyUnlocked(context)
    ) {
      pendingPinAction = action
      pendingActionTitle = actionTitle
      showPinDialog = true
    } else {
      action()
    }
  }

  // VPN Permission launcher
  val vpnLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
      LocalDnsVpnService.start(context)
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.94f)
        .fillMaxHeight(0.88f)
        .clip(RoundedCornerShape(24.dp))
        .border(1.2.dp, if (isStrictLock) Color(0xFFEF4444) else GoldBrass.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
      color = ObsidianCharcoal
    ) {
      Column(
        modifier = Modifier
          .padding(20.dp)
      ) {
        // 1. Header
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
                .background(Color(0xFF3B1515)),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text("ब्लॉक-लिस्ट व सुरक्षित शब्द", color = WarmOffWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
              Text("100% ऑन-डिवाइस फ़िल्टरिंग", color = Color(0xFF10B981), fontSize = 11.sp)
            }
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = WarmMuted)
          }
        }

        // Strict Lock Warning
        if (isStrictLock) {
          Spacer(modifier = Modifier.height(10.dp))
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(Color(0xFF281313))
              .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f), RoundedCornerShape(10.dp))
              .padding(horizontal = 12.dp, vertical = 8.dp)
          ) {
            Text(
              text = "🚫 15-मिनट सख्त लॉक सक्रिय: समाप्त होने तक सूची में कोई भी बदलाव वर्जित है।",
              color = Color(0xFFFFB4AB),
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Tabs
        TabRow(
          selectedTabIndex = selectedTabIndex,
          containerColor = ObsidianCard,
          contentColor = GoldBrass,
          indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
              Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
              color = GoldBrass
            )
          }
        ) {
          tabTitles.forEachIndexed { index, title ->
            Tab(
              selected = selectedTabIndex == index,
              onClick = { selectedTabIndex = index },
              text = {
                Text(
                  text = title,
                  fontSize = 11.sp,
                  fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                  color = if (selectedTabIndex == index) GoldBrass else WarmMuted
                )
              }
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Tab Contents
        when (selectedTabIndex) {
          0 -> {
            // DOMAINS TAB
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "150+ अंतर्निहित एडल्ट डोमेन स्वतः सक्रिय हैं। आप नए डोमेन जोड़ या हटा सकते हैं:",
                color = WarmParchment,
                fontSize = 12.sp
              )
              Spacer(modifier = Modifier.height(8.dp))

              // Add domain input row
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
              ) {
                OutlinedTextField(
                  value = inputDomain,
                  onValueChange = { inputDomain = it },
                  placeholder = { Text("उदा. baddomain.com", color = WarmMuted, fontSize = 12.sp) },
                  singleLine = true,
                  enabled = !isStrictLock,
                  modifier = Modifier.weight(1f),
                  colors = TextFieldDefaults.colors(
                    focusedContainerColor = ObsidianElevated,
                    unfocusedContainerColor = ObsidianElevated,
                    focusedTextColor = WarmOffWhite,
                    unfocusedTextColor = WarmOffWhite
                  ),
                  shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                  onClick = {
                    if (AdultBlocklistManager.addCustomDomain(context, inputDomain)) {
                      customDomains = AdultBlocklistManager.getCustomBlockedDomains(context).toList().sorted()
                      inputDomain = ""
                    }
                  },
                  enabled = !isStrictLock && inputDomain.isNotBlank(),
                  colors = ButtonDefaults.buttonColors(containerColor = GoldBrass),
                  shape = RoundedCornerShape(12.dp)
                ) {
                  Icon(Icons.Default.Add, contentDescription = "Add", tint = ObsidianCharcoal)
                }
              }

              Spacer(modifier = Modifier.height(12.dp))
              Text("आपके जोड़े गए डोमेन (${customDomains.size}):", color = WarmOffWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              Spacer(modifier = Modifier.height(6.dp))

              if (customDomains.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                  Text("कोई कस्टम डोमेन नहीं जोड़ा गया।\nअंतर्निहित 150+ डोमेन सक्रिय हैं।", color = WarmMuted, fontSize = 12.sp)
                }
              } else {
                LazyColumn(
                  modifier = Modifier.weight(1f),
                  verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  items(customDomains) { domain ->
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianElevated)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Text(domain, color = WarmOffWhite, fontSize = 13.sp)
                      if (!isStrictLock) {
                        IconButton(
                          onClick = {
                            executeWithPin("ब्लॉक डोमेन '$domain' हटाना") {
                              AdultBlocklistManager.removeCustomDomain(context, domain, pinVerified = true)
                              customDomains = AdultBlocklistManager.getCustomBlockedDomains(context).toList().sorted()
                            }
                          },
                          modifier = Modifier.size(24.dp)
                        ) {
                          Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFFF6B6B))
                        }
                      }
                    }
                  }
                }
              }
            }
          }

          1 -> {
            // KEYWORDS TAB
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "हिंदी, English व Hinglish के शब्द व वाक्यांश। आप नए शब्द जोड़ या हटा सकते हैं:",
                color = WarmParchment,
                fontSize = 12.sp
              )
              Spacer(modifier = Modifier.height(8.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
              ) {
                OutlinedTextField(
                  value = inputKeyword,
                  onValueChange = { inputKeyword = it },
                  placeholder = { Text("उदा. खराब शब्द / phrase", color = WarmMuted, fontSize = 12.sp) },
                  singleLine = true,
                  enabled = !isStrictLock,
                  modifier = Modifier.weight(1f),
                  colors = TextFieldDefaults.colors(
                    focusedContainerColor = ObsidianElevated,
                    unfocusedContainerColor = ObsidianElevated,
                    focusedTextColor = WarmOffWhite,
                    unfocusedTextColor = WarmOffWhite
                  ),
                  shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                  onClick = {
                    if (AdultBlocklistManager.addCustomKeyword(context, inputKeyword)) {
                      customKeywords = AdultBlocklistManager.getCustomBlockedKeywords(context).toList().sorted()
                      inputKeyword = ""
                    }
                  },
                  enabled = !isStrictLock && inputKeyword.isNotBlank(),
                  colors = ButtonDefaults.buttonColors(containerColor = GoldBrass),
                  shape = RoundedCornerShape(12.dp)
                ) {
                  Icon(Icons.Default.Add, contentDescription = "Add", tint = ObsidianCharcoal)
                }
              }

              Spacer(modifier = Modifier.height(12.dp))
              Text("आपके जोड़े गए शब्द (${customKeywords.size}):", color = WarmOffWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              Spacer(modifier = Modifier.height(6.dp))

              if (customKeywords.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                  Text("कोई कस्टम शब्द नहीं जोड़ा गया।\nसभी मुख्य हिंदी/English शब्द अंतर्निहित हैं।", color = WarmMuted, fontSize = 12.sp)
                }
              } else {
                LazyColumn(
                  modifier = Modifier.weight(1f),
                  verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  items(customKeywords) { kw ->
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianElevated)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Text(kw, color = WarmOffWhite, fontSize = 13.sp)
                      if (!isStrictLock) {
                        IconButton(
                          onClick = {
                            executeWithPin("ब्लॉक शब्द '$kw' हटाना") {
                              AdultBlocklistManager.removeCustomKeyword(context, kw, pinVerified = true)
                              customKeywords = AdultBlocklistManager.getCustomBlockedKeywords(context).toList().sorted()
                            }
                          },
                          modifier = Modifier.size(24.dp)
                        ) {
                          Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFFF6B6B))
                        }
                      }
                    }
                  }
                }
              }
            }
          }

          2 -> {
            // SAFE WORDS TAB (WHITELIST)
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "सुरक्षित शब्द नियम (False-Positive Prevention): इन शब्दों से युक्त वाक्यों पर लॉक नहीं लगेगा (उदा. Essex, sex education, biology):",
                color = WarmParchment,
                fontSize = 12.sp
              )
              Spacer(modifier = Modifier.height(8.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
              ) {
                OutlinedTextField(
                  value = inputSafeWord,
                  onValueChange = { inputSafeWord = it },
                  placeholder = { Text("उदा. medical study, genetics", color = WarmMuted, fontSize = 12.sp) },
                  singleLine = true,
                  enabled = !isStrictLock,
                  modifier = Modifier.weight(1f),
                  colors = TextFieldDefaults.colors(
                    focusedContainerColor = ObsidianElevated,
                    unfocusedContainerColor = ObsidianElevated,
                    focusedTextColor = WarmOffWhite,
                    unfocusedTextColor = WarmOffWhite
                  ),
                  shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                  onClick = {
                    executeWithPin("सुरक्षित शब्द जोड़ना") {
                      if (AdultBlocklistManager.addCustomSafeWord(context, inputSafeWord, pinVerified = true)) {
                        customSafeWords = AdultBlocklistManager.getCustomSafeWords(context).toList().sorted()
                        inputSafeWord = ""
                      }
                    }
                  },
                  enabled = !isStrictLock && inputSafeWord.isNotBlank(),
                  colors = ButtonDefaults.buttonColors(containerColor = GoldBrass),
                  shape = RoundedCornerShape(12.dp)
                ) {
                  Icon(Icons.Default.Add, contentDescription = "Add", tint = ObsidianCharcoal)
                }
              }

              Spacer(modifier = Modifier.height(12.dp))
              Text("आपके जोड़े गए सुरक्षित शब्द (${customSafeWords.size}):", color = WarmOffWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              Spacer(modifier = Modifier.height(6.dp))

              if (customSafeWords.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                  Text("कोई कस्टम सुरक्षित शब्द नहीं जोड़ा गया।\nअंतर्निहित शैक्षिक व भौगोलिक शब्द सक्रिय हैं।", color = WarmMuted, fontSize = 12.sp)
                }
              } else {
                LazyColumn(
                  modifier = Modifier.weight(1f),
                  verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  items(customSafeWords) { sw ->
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianElevated)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Text(sw, color = WarmOffWhite, fontSize = 13.sp)
                      if (!isStrictLock) {
                        IconButton(
                          onClick = {
                            AdultBlocklistManager.removeCustomSafeWord(context, sw)
                            customSafeWords = AdultBlocklistManager.getCustomSafeWords(context).toList().sorted()
                          },
                          modifier = Modifier.size(24.dp)
                        ) {
                          Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFFF6B6B))
                        }
                      }
                    }
                  }
                }
              }
            }
          }

          3 -> {
            // LOCAL DNS VPN TAB
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(14.dp))
                  .background(ObsidianCard)
                  .padding(16.dp)
              ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text("DNS-स्तर पर डोमेन फ़िल्टर", color = WarmOffWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                      Text(
                        text = if (isVpnActive) "🟢 सक्रिय: सभी ऐप्स व ब्राउज़रों में DNS पर ख़राब डोमेन अवरोधित" else "⚪ निष्क्रिय: चालू करने पर लोकल VPN द्वारा डोमेन रुकेंगे",
                        color = if (isVpnActive) Color(0xFF10B981) else WarmMuted,
                        fontSize = 11.sp
                      )
                    }
                    Switch(
                      checked = isVpnActive,
                      enabled = !isStrictLock,
                      onCheckedChange = { enable ->
                        if (enable) {
                          val prepareIntent = VpnService.prepare(context)
                          if (prepareIntent != null) {
                            vpnLauncher.launch(prepareIntent)
                          } else {
                            LocalDnsVpnService.start(context)
                          }
                        } else {
                          executeWithPin("DNS फ़िल्टर बंद करना") {
                            LocalDnsVpnService.stop(context, pinVerified = true)
                          }
                        }
                      },
                      colors = SwitchDefaults.colors(
                        checkedThumbColor = GoldBrass,
                        checkedTrackColor = GoldBrass.copy(alpha = 0.35f)
                      )
                    )
                  }

                  Text(
                    text = "• 100% ऑन-डिवाइस: कोई डेटा किसी बाहरी सर्वर पर नहीं जाता।\n• Brave, Chrome, Firefox, Samsung, Opera सहित पूरे सिस्टम में प्रभावी।\n• ख़राब डोमेन की DNS क्वेरी आते ही 15 मिनट का Strict Lock स्वतः शुरू हो जाता है।",
                    color = WarmParchment,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Done button
        Button(
          onClick = onDismiss,
          colors = ButtonDefaults.buttonColors(containerColor = GoldBrass),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().height(46.dp)
        ) {
          Text("पूर्ण (Done)", color = ObsidianCharcoal, fontWeight = FontWeight.Bold)
        }
      }
    }
  }

  if (showPinDialog) {
    AccountabilityPinDialog(
      onDismiss = {
        showPinDialog = false
        pendingPinAction = null
      },
      onActionApproved = {
        pendingPinAction?.invoke()
        showPinDialog = false
        pendingPinAction = null
      },
      mode = AccountabilityPinDialogMode.VERIFY_AND_ROTATE,
      actionTitle = pendingActionTitle
    )
  }
}
