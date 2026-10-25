package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.data.model.JournalistCategory
import com.example.data.model.JournalistEntryEntity
import com.example.data.model.JournalistPersonEntity
import com.example.ui.components.JournalistEntryBottomSheet
import com.example.ui.theme.CardBorderGradient
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceHighlight
import com.example.ui.theme.GlassGradient
import com.example.ui.theme.StatusComplete
import com.example.ui.theme.StatusMissed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletNeon
import com.example.ui.viewmodel.LifeTrackerViewModel
import com.example.util.ExportImportHelper
import com.example.util.TimeUtils
import kotlinx.coroutines.launch

@Composable
fun ReflectionScreen(
  viewModel: LifeTrackerViewModel,
  modifier: Modifier = Modifier
) {
  val persons by viewModel.allJournalistPersons.collectAsState()
  val allEntries by viewModel.allJournalistEntries.collectAsState()
  val selectedPersonId by viewModel.selectedPersonId.collectAsState()

  val categoryFilter by viewModel.journalistCategoryFilter.collectAsState()
  val searchQuery by viewModel.journalistSearchQuery.collectAsState()
  val profileEntries by viewModel.profileEntries.collectAsState()

  var isAddingEntry by remember { mutableStateOf(false) }
  var editingEntry by remember { mutableStateOf<JournalistEntryEntity?>(null) }
  var isAddingPersonDialog by remember { mutableStateOf(false) }

  val activePerson = persons.firstOrNull { it.id == selectedPersonId }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(DarkBackground)
  ) {
    if (activePerson == null) {
      // SCREEN 1: JOURNALIST HOME (Persons List)
      JournalistHomeScreen(
        persons = persons,
        allEntries = allEntries,
        onSelectPerson = { viewModel.setSelectedPerson(it.id) },
        onAddPersonClick = { isAddingPersonDialog = true },
        onQuickAddEntryClick = {
          editingEntry = null
          isAddingEntry = true
        },
        onBack = { viewModel.navigateBack() }
      )
    } else {
      // SCREEN 2: PERSON PROFILE (Detailed Timeline, Filters, Download)
      val personEntries = remember(allEntries, activePerson.id) {
        allEntries.filter { it.personId == activePerson.id }
      }

      JournalistProfileScreen(
        person = activePerson,
        allPersonEntries = personEntries,
        filteredEntries = profileEntries,
        categoryFilter = categoryFilter,
        searchQuery = searchQuery,
        onCategoryFilterChange = { viewModel.setJournalistCategoryFilter(it) },
        onSearchQueryChange = { viewModel.setJournalistSearchQuery(it) },
        onBack = { viewModel.setSelectedPerson(null) },
        onAddEntryClick = {
          editingEntry = null
          isAddingEntry = true
        },
        onEditEntryClick = {
          editingEntry = it
          isAddingEntry = true
        },
        onDeletePerson = {
          viewModel.deleteJournalistPerson(activePerson.id)
        }
      )
    }

    // Add / Edit Entry Bottom Sheet
    if (isAddingEntry) {
      val defaultId = activePerson?.id ?: "self"
      JournalistEntryBottomSheet(
        initialEntry = editingEntry,
        persons = if (persons.isNotEmpty()) persons else listOf(
          JournalistPersonEntity("self", "मैं (स्वयं)", "👤")
        ),
        defaultPersonId = defaultId,
        onDismiss = {
          isAddingEntry = false
          editingEntry = null
        },
        onSave = { id, personId, personName, category, text, date, time, context, intensity ->
          viewModel.saveJournalistEntry(
            id = id,
            personId = personId,
            personName = personName,
            category = category,
            text = text,
            date = date,
            time = time,
            context = context,
            intensity = intensity
          )
        },
        onDelete = { entryId ->
          viewModel.deleteJournalistEntry(entryId)
        }
      )
    }

    // Add Person Dialog
    if (isAddingPersonDialog) {
      AddPersonDialog(
        onDismiss = { isAddingPersonDialog = false },
        onConfirm = { name, emoji ->
          viewModel.addJournalistPerson(name, emoji)
          isAddingPersonDialog = false
        }
      )
    }
  }
}

// ─────────────────────────────────────────────────────────────
// 1. PERSONS LIST VIEW (JOURNALIST HOME)
// ─────────────────────────────────────────────────────────────

@Composable
private fun JournalistHomeScreen(
  persons: List<JournalistPersonEntity>,
  allEntries: List<JournalistEntryEntity>,
  onSelectPerson: (JournalistPersonEntity) -> Unit,
  onAddPersonClick: () -> Unit,
  onQuickAddEntryClick: () -> Unit,
  onBack: () -> Unit = {}
) {
  Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Header
      item {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = "Back",
                  tint = TextPrimary
                )
              }
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "NEUTRAL JOURNALIST",
                color = CyanNeon,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
              )
            }

            OutlinedButton(
              onClick = onAddPersonClick,
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon),
              border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.5f)),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
              modifier = Modifier.testTag("btn_add_person")
            ) {
              Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Add Person", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }

          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Observer of Self & Others",
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "खुद और दूसरों को एक निष्पक्ष पत्रकार की नज़र से observe करें।",
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 16.sp
          )
        }
      }

      // Total Statistics Banner
      item {
        val totalGood = allEntries.count { it.category == JournalistCategory.GOOD }
        val totalBad = allEntries.count { it.category == JournalistCategory.BAD }
        val totalObs = allEntries.count { it.category == JournalistCategory.OBSERVATION }

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
          ) {
            MetricStatMini(label = "व्यक्तियों की संख्या", value = "${persons.size}", color = CyanNeon)
            Box(modifier = Modifier.width(1.dp).height(28.dp).background(DarkSurfaceBorder))
            MetricStatMini(label = "🟢 अच्छाई", value = "$totalGood", color = StatusComplete)
            Box(modifier = Modifier.width(1.dp).height(28.dp).background(DarkSurfaceBorder))
            MetricStatMini(label = "🔴 बुराई", value = "$totalBad", color = StatusMissed)
            Box(modifier = Modifier.width(1.dp).height(28.dp).background(DarkSurfaceBorder))
            MetricStatMini(label = "⚪ Observation", value = "$totalObs", color = TextPrimary)
          }
        }
      }

      // Section Title
      item {
        Text(
          text = "व्यक्तियों की सूची (SELECT PROFILE)",
          color = TextSecondary,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.8.sp
        )
      }

      // Persons List
      items(persons, key = { it.id }) { person ->
        val personEntries = remember(allEntries, person.id) {
          allEntries.filter { it.personId == person.id }
        }
        val goodCount = remember(personEntries) {
          personEntries.count { it.category == JournalistCategory.GOOD }
        }
        val badCount = remember(personEntries) {
          personEntries.count { it.category == JournalistCategory.BAD }
        }
        val obsCount = remember(personEntries) {
          personEntries.count { it.category == JournalistCategory.OBSERVATION }
        }

        PersonCard(
          person = person,
          totalCount = personEntries.size,
          goodCount = goodCount,
          badCount = badCount,
          obsCount = obsCount,
          onClick = { onSelectPerson(person) }
        )
      }
    }

    // Floating Action Button to Quick Add Entry
    FloatingActionButton(
      onClick = onQuickAddEntryClick,
      containerColor = CyanNeon,
      contentColor = Color.Black,
      shape = CircleShape,
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(end = 20.dp, bottom = 84.dp)
        .size(56.dp)
        .testTag("fab_quick_add_observation")
    ) {
      Icon(Icons.Default.Add, contentDescription = "Add Observation", modifier = Modifier.size(26.dp))
    }
  }
}

@Composable
private fun MetricStatMini(label: String, value: String, color: Color) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(text = value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Black)
    Spacer(modifier = Modifier.height(2.dp))
    Text(text = label, color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
  }
}

@Composable
private fun PersonCard(
  person: JournalistPersonEntity,
  totalCount: Int,
  goodCount: Int,
  badCount: Int,
  obsCount: Int,
  onClick: () -> Unit
) {
  val isSelf = person.id == "self"
  val borderBrush = if (isSelf) CardBorderGradient else null

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(DarkSurfaceElevated)
      .then(
        if (borderBrush != null) Modifier.border(1.dp, borderBrush, RoundedCornerShape(16.dp))
        else Modifier.border(1.dp, DarkSurfaceBorder, RoundedCornerShape(16.dp))
      )
      .clickable { onClick() }
      .padding(14.dp)
      .testTag("person_card_${person.id}")
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Left: Avatar + Name + Subtitle
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (isSelf) CyanNeon.copy(alpha = 0.15f) else DarkSurface)
            .border(1.dp, if (isSelf) CyanNeon.copy(alpha = 0.5f) else DarkSurfaceBorder, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Text(text = person.emoji, fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = person.name,
              color = TextPrimary,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold
            )
            if (isSelf) {
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "DEFAULT",
                color = CyanNeon,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                  .clip(RoundedCornerShape(4.dp))
                  .background(CyanNeon.copy(alpha = 0.15f))
                  .padding(horizontal = 5.dp, vertical = 2.dp)
              )
            }
          }
          Spacer(modifier = Modifier.height(3.dp))
          Text(
            text = "$totalCount Observations",
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
          )
        }
      }

      // Right: Mini Badge Counts
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (goodCount > 0) {
          MiniCountBadge("🟢 $goodCount", StatusComplete)
        }
        if (badCount > 0) {
          MiniCountBadge("🔴 $badCount", StatusMissed)
        }
        if (obsCount > 0) {
          MiniCountBadge("⚪ $obsCount", TextSecondary)
        }
        if (totalCount == 0) {
          Text("खाली", color = TextMuted, fontSize = 11.sp)
        }
      }
    }
  }
}

@Composable
private fun MiniCountBadge(text: String, color: Color) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(6.dp))
      .background(color.copy(alpha = 0.12f))
      .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
      .padding(horizontal = 6.dp, vertical = 3.dp)
  ) {
    Text(text = text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
  }
}

// ─────────────────────────────────────────────────────────────
// 2. PERSON PROFILE SCREEN (Counters, Filter, Timeline, Export)
// ─────────────────────────────────────────────────────────────

@Composable
private fun JournalistProfileScreen(
  person: JournalistPersonEntity,
  allPersonEntries: List<JournalistEntryEntity>,
  filteredEntries: List<JournalistEntryEntity>,
  categoryFilter: JournalistCategory?,
  searchQuery: String,
  onCategoryFilterChange: (JournalistCategory?) -> Unit,
  onSearchQueryChange: (String) -> Unit,
  onBack: () -> Unit,
  onAddEntryClick: () -> Unit,
  onEditEntryClick: (JournalistEntryEntity) -> Unit,
  onDeletePerson: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  var showExportMenu by remember { mutableStateOf(false) }
  var showDeletePersonConfirm by remember { mutableStateOf(false) }

  val goodCount = remember(allPersonEntries) {
    allPersonEntries.count { it.category == JournalistCategory.GOOD }
  }
  val badCount = remember(allPersonEntries) {
    allPersonEntries.count { it.category == JournalistCategory.BAD }
  }
  val obsCount = remember(allPersonEntries) {
    allPersonEntries.count { it.category == JournalistCategory.OBSERVATION }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Profile Top Bar (Back button, Name, Download/Share menu)
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = onBack,
              modifier = Modifier.testTag("btn_back_to_persons")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = CyanNeon
              )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column {
              Text(
                text = "${person.emoji} ${person.name}",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
              )
              Text(
                text = "${allPersonEntries.size} Recorded Entries",
                color = TextMuted,
                fontSize = 11.sp
              )
            }
          }

          // Action Menu (Download / Share / Delete Person)
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
              OutlinedButton(
                onClick = { showExportMenu = true },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.5f)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.testTag("btn_download_person_report")
              ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("डाउनलोड", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
              }

              DropdownMenu(
                expanded = showExportMenu,
                onDismissRequest = { showExportMenu = false },
                modifier = Modifier.background(DarkSurfaceElevated)
              ) {
                DropdownMenuItem(
                  text = { Text("📄 Download as TXT", color = TextPrimary, fontSize = 13.sp) },
                  onClick = {
                    showExportMenu = false
                    val txt = ExportImportHelper.exportPersonAsTxt(person, allPersonEntries)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                      type = "text/plain"
                      putExtra(Intent.EXTRA_SUBJECT, "Journalist Report: ${person.name}")
                      putExtra(Intent.EXTRA_TEXT, txt)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Download / Share TXT"))
                  }
                )
                DropdownMenuItem(
                  text = { Text("💾 Download as JSON", color = TextPrimary, fontSize = 13.sp) },
                  onClick = {
                    showExportMenu = false
                    val json = ExportImportHelper.exportPersonAsJson(person, allPersonEntries)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                      type = "text/plain"
                      putExtra(Intent.EXTRA_SUBJECT, "Journalist Report: ${person.name} (JSON)")
                      putExtra(Intent.EXTRA_TEXT, json)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Download / Share JSON"))
                  }
                )

                if (person.id != "self") {
                  DropdownMenuItem(
                    text = { Text("🗑️ Delete Profile", color = StatusMissed, fontSize = 13.sp) },
                    onClick = {
                      showExportMenu = false
                      showDeletePersonConfirm = true
                    }
                  )
                }
              }
            }
          }
        }
      }

      // 1. Counter Cards (Good, Bad, Observation) with Filter toggle
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          ProfileCounterCard(
            title = "अच्छाई",
            count = goodCount,
            accentColor = StatusComplete,
            iconPrefix = "🟢",
            isSelected = categoryFilter == JournalistCategory.GOOD,
            onClick = {
              onCategoryFilterChange(
                if (categoryFilter == JournalistCategory.GOOD) null else JournalistCategory.GOOD
              )
            },
            modifier = Modifier.weight(1f)
          )

          ProfileCounterCard(
            title = "बुराई",
            count = badCount,
            accentColor = StatusMissed,
            iconPrefix = "🔴",
            isSelected = categoryFilter == JournalistCategory.BAD,
            onClick = {
              onCategoryFilterChange(
                if (categoryFilter == JournalistCategory.BAD) null else JournalistCategory.BAD
              )
            },
            modifier = Modifier.weight(1f)
          )

          ProfileCounterCard(
            title = "Observation",
            count = obsCount,
            accentColor = CyanNeon,
            iconPrefix = "⚪",
            isSelected = categoryFilter == JournalistCategory.OBSERVATION,
            onClick = {
              onCategoryFilterChange(
                if (categoryFilter == JournalistCategory.OBSERVATION) null else JournalistCategory.OBSERVATION
              )
            },
            modifier = Modifier.weight(1f)
          )
        }
      }

      // 2. Search Bar inside Person Profile
      item {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = onSearchQueryChange,
          placeholder = { Text("Search this person's observations or context...", fontSize = 12.sp) },
          leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
          },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { onSearchQueryChange("") }) {
                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(18.dp))
              }
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("journalist_profile_search"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedBorderColor = CyanNeon,
            unfocusedBorderColor = DarkSurfaceBorder,
            focusedContainerColor = DarkSurfaceElevated,
            unfocusedContainerColor = DarkSurfaceElevated
          ),
          shape = RoundedCornerShape(12.dp),
          singleLine = true
        )
      }

      // 3. Timeline Observations List
      if (filteredEntries.isEmpty()) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 40.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "🔎",
                fontSize = 40.sp
              )
              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = if (searchQuery.isNotEmpty() || categoryFilter != null)
                  "फ़िल्टर के अनुसार कोई entry नहीं मिली"
                else
                  "इस profile में अभी कोई entry नहीं है",
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "नीचे '+' बटन दबाकर नई entry जोड़ें",
                color = TextMuted,
                fontSize = 12.sp
              )
            }
          }
        }
      } else {
        item {
          Text(
            text = "TIMELINE (${filteredEntries.size})",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
          )
        }

        items(filteredEntries, key = { it.id }) { entry ->
          JournalistTimelineCard(
            entry = entry,
            onClick = { onEditEntryClick(entry) }
          )
        }
      }
    }

    // Floating Action Button to Add Entry for this person
    FloatingActionButton(
      onClick = onAddEntryClick,
      containerColor = CyanNeon,
      contentColor = Color.Black,
      shape = CircleShape,
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(end = 20.dp, bottom = 84.dp)
        .size(56.dp)
        .testTag("fab_add_person_entry")
    ) {
      Icon(Icons.Default.Add, contentDescription = "Add Entry", modifier = Modifier.size(26.dp))
    }

    // Delete Person Confirmation Dialog
    if (showDeletePersonConfirm) {
      AlertDialog(
        onDismissRequest = { showDeletePersonConfirm = false },
        title = { Text("Profile हटाएं?", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
          Text(
            "क्या आप '${person.name}' और इनकी सारी observations हटाना चाहते हैं? यह क्रिया वापस नहीं ली जा सकती।",
            color = TextSecondary,
            fontSize = 13.sp
          )
        },
        confirmButton = {
          Button(
            onClick = {
              showDeletePersonConfirm = false
              onDeletePerson()
            },
            colors = ButtonDefaults.buttonColors(containerColor = StatusMissed)
          ) {
            Text("हटाएं", color = Color.White, fontWeight = FontWeight.Bold)
          }
        },
        dismissButton = {
          TextButton(onClick = { showDeletePersonConfirm = false }) {
            Text("रद्द करें", color = TextSecondary)
          }
        },
        containerColor = DarkSurfaceElevated
      )
    }
  }
}

@Composable
private fun ProfileCounterCard(
  title: String,
  count: Int,
  accentColor: Color,
  iconPrefix: String,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .background(if (isSelected) accentColor.copy(alpha = 0.22f) else DarkSurfaceElevated)
      .border(
        width = 1.dp,
        color = if (isSelected) accentColor else DarkSurfaceBorder,
        shape = RoundedCornerShape(12.dp)
      )
      .clickable { onClick() }
      .padding(vertical = 12.dp, horizontal = 10.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = "$iconPrefix $count",
        color = if (isSelected) accentColor else TextPrimary,
        fontSize = 18.sp,
        fontWeight = FontWeight.Black
      )
      Spacer(modifier = Modifier.height(3.dp))
      Text(
        text = title,
        color = if (isSelected) accentColor else TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
      )
    }
  }
}

@Composable
private fun JournalistTimelineCard(
  entry: JournalistEntryEntity,
  onClick: () -> Unit
) {
  val accentColor = when (entry.category) {
    JournalistCategory.GOOD -> StatusComplete
    JournalistCategory.BAD -> StatusMissed
    JournalistCategory.OBSERVATION -> CyanNeon
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(DarkSurfaceElevated)
      .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(14.dp))
      .clickable { onClick() }
      .padding(14.dp)
      .testTag("entry_card_${entry.id}")
  ) {
    Column {
      // Row 1: Category Tag + Natural Hindi Time
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Category Pill
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(accentColor.copy(alpha = 0.15f))
            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(entry.category.iconPrefix, fontSize = 10.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = entry.category.label,
              color = accentColor,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        // Natural Hindi Time display (e.g. "🌅 सुबह 5:00 बजे")
        val hindiTime = TimeUtils.timeStringToHindi(entry.time)
        val displayDate = TimeUtils.formatDateDisplay(entry.date)
        Text(
          text = "$displayDate • $hindiTime",
          color = TextMuted,
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Row 2: Observation Text
      Text(
        text = entry.text,
        color = TextPrimary,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal
      )

      // Row 3: Context and Intensity Badges (if present)
      if (entry.context.isNotBlank() || entry.intensity.isNotBlank()) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (entry.context.isNotBlank()) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(DarkSurface)
                .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(6.dp))
                .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
              Text(
                text = "📍 ${entry.context}",
                color = VioletNeon,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
              )
            }
          }

          if (entry.intensity.isNotBlank()) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(DarkSurface)
                .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(6.dp))
                .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
              Text(
                text = "⚡ ${entry.intensity}",
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
              )
            }
          }
        }
      }
    }
  }
}

// ─────────────────────────────────────────────────────────────
// 3. ADD PERSON DIALOG
// ─────────────────────────────────────────────────────────────

@Composable
private fun AddPersonDialog(
  onDismiss: () -> Unit,
  onConfirm: (name: String, emoji: String) -> Unit
) {
  var name by remember { mutableStateOf("") }
  var selectedEmoji by remember { mutableStateOf("👤") }
  var isError by remember { mutableStateOf(false) }

  val emojiOptions = listOf("👤", "👨", "👩", "💼", "🤝", "🏡", "🧠", "🎯")

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = DarkSurfaceElevated,
    title = {
      Text(
        text = "नया व्यक्ति जोड़ें",
        color = CyanNeon,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = "नाम (PERSON NAME) *",
          color = TextSecondary,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
          value = name,
          onValueChange = {
            name = it
            if (it.isNotBlank()) isError = false
          },
          placeholder = { Text("जैसे: राहुल, बॉस, पार्टनर...", fontSize = 13.sp) },
          isError = isError,
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedBorderColor = CyanNeon,
            unfocusedBorderColor = DarkSurfaceBorder,
            focusedContainerColor = DarkSurface,
            unfocusedContainerColor = DarkSurface
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_new_person_name")
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = "इमोजी अवतार (AVATAR)",
          color = TextSecondary,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          emojiOptions.forEach { emo ->
            val isSelected = selectedEmoji == emo
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isSelected) CyanNeon.copy(alpha = 0.25f) else DarkSurface)
                .border(1.dp, if (isSelected) CyanNeon else DarkSurfaceBorder, CircleShape)
                .clickable { selectedEmoji = emo },
              contentAlignment = Alignment.Center
            ) {
              Text(emo, fontSize = 16.sp)
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.isBlank()) {
            isError = true
            return@Button
          }
          onConfirm(name.trim(), selectedEmoji)
        },
        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color.Black),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.testTag("btn_confirm_add_person")
      ) {
        Text("जोड़ें", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("रद्द करें", color = TextSecondary)
      }
    }
  )
}
