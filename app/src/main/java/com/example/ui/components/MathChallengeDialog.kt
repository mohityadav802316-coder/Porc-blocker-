package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focus.MathChallengeGenerator
import com.example.focus.MathQuestion
import com.example.ui.theme.ChronoSerifFamily
import com.example.ui.theme.DustyRose
import com.example.ui.theme.GoldBrass
import com.example.ui.theme.GoldHighlight
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.SageGreen
import com.example.ui.theme.WarmMuted
import com.example.ui.theme.WarmOffWhite
import com.example.ui.theme.WarmParchment

/**
 * 10th-standard Mathematics Challenge Dialog for unlocking/exiting Focus Mode.
 * Requires answering 3 distinct questions correctly to verify intentional discipline exit.
 */
@Composable
fun MathChallengeDialog(
  onUnlockSuccess: () -> Unit,
  onDismiss: () -> Unit
) {
  var questions by remember { mutableStateOf(MathChallengeGenerator.generateThreeQuestions()) }
  var currentStep by remember { mutableIntStateOf(0) } // 0, 1, 2
  var answerInput by remember { mutableStateOf("") }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var showHint by remember { mutableStateOf(false) }

  val currentQuestion: MathQuestion = questions.getOrNull(currentStep)
    ?: questions.firstOrNull()
    ?: MathChallengeGenerator.generateThreeQuestions().first()

  fun handleSubmit() {
    if (answerInput.isBlank()) {
      errorMessage = "कृपया उत्तर दर्ज करें"
      return
    }

    val isCorrect = MathChallengeGenerator.isAnswerCorrect(currentQuestion, answerInput)
    if (isCorrect) {
      errorMessage = null
      showHint = false
      answerInput = ""
      if (currentStep < 2) {
        currentStep += 1
      } else {
        // All 3 answered correctly!
        onUnlockSuccess()
      }
    } else {
      errorMessage = "गलत उत्तर! नए प्रश्न लोड हो रहे हैं..."
      answerInput = ""
      showHint = false
      // Regenerate questions on wrong attempt to prevent brute force
      questions = MathChallengeGenerator.generateThreeQuestions()
      currentStep = 0
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    modifier = Modifier.testTag("math_challenge_dialog"),
    containerColor = ObsidianCard,
    tonalElevation = 10.dp,
    shape = RoundedCornerShape(24.dp),
    title = {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(GoldBrass.copy(alpha = 0.2f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.LockOpen, contentDescription = null, tint = GoldBrass, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "गणित चुनौती अनलॉक",
              color = WarmOffWhite,
              fontFamily = ChronoSerifFamily,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold
            )
          }

          IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = WarmMuted, modifier = Modifier.size(18.dp))
          }
        }

        // Progress indicators (Step 1, Step 2, Step 3)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          for (i in 0..2) {
            val isCurrent = i == currentStep
            val isDone = i < currentStep
            val barColor by animateColorAsState(
              targetValue = when {
                isDone -> SageGreen
                isCurrent -> GoldBrass
                else -> ObsidianBorder
              },
              label = "step_color_$i"
            )

            Box(
              modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .clip(CircleShape)
                .background(barColor)
            )
          }
        }

        Text(
          text = "प्रश्न ${currentStep + 1} / 3 • कक्षा 10 स्तर",
          color = GoldHighlight,
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold,
          letterSpacing = 0.5.sp
        )
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Question Topic Card
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ObsidianElevated)
            .border(1.dp, ObsidianBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
              text = currentQuestion.topic,
              color = GoldBrass,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )

            Text(
              text = currentQuestion.questionHi,
              color = WarmOffWhite,
              fontSize = 14.sp,
              fontWeight = FontWeight.SemiBold,
              lineHeight = 20.sp
            )

            Text(
              text = currentQuestion.questionEn,
              color = WarmParchment.copy(alpha = 0.8f),
              fontSize = 12.sp,
              lineHeight = 16.sp
            )

            // Hint toggle
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              TextButton(
                onClick = { showHint = !showHint },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
              ) {
                Icon(Icons.Default.HelpOutline, contentDescription = null, tint = GoldBrass, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (showHint) "संकेत छिपाएं" else "संकेत देखें (Hint)", color = GoldBrass, fontSize = 11.sp)
              }
            }

            AnimatedVisibility(visible = showHint) {
              Text(
                text = "💡 ${currentQuestion.hint}",
                color = GoldHighlight,
                fontSize = 11.sp,
                modifier = Modifier
                  .fillMaxWidth()
                  .background(GoldBrass.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                  .padding(8.dp)
              )
            }
          }
        }

        // Answer Input Field
        OutlinedTextField(
          value = answerInput,
          onValueChange = {
            answerInput = it.filter { char -> char.isDigit() || char == '-' }
            errorMessage = null
          },
          label = { Text("आपका उत्तर (पूर्णांक / Integer)", color = WarmMuted, fontSize = 12.sp) },
          placeholder = { Text("उदा. 5", color = WarmMuted.copy(alpha = 0.5f)) },
          singleLine = true,
          keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
          ),
          keyboardActions = KeyboardActions(onDone = { handleSubmit() }),
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = WarmOffWhite,
            unfocusedTextColor = WarmOffWhite,
            focusedBorderColor = GoldBrass,
            unfocusedBorderColor = ObsidianBorder,
            cursorColor = GoldBrass
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("math_challenge_input")
        )

        // Error message if any
        if (errorMessage != null) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp)
          ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = DustyRose, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(errorMessage ?: "", color = DustyRose, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
          }
        }

        Text(
          text = "फोकस मोड से बाहर निकलने के लिए तीनों प्रश्नों का सही उत्तर आवश्यक है।",
          color = WarmMuted,
          fontSize = 11.sp,
          lineHeight = 15.sp
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { handleSubmit() },
        colors = ButtonDefaults.buttonColors(containerColor = GoldBrass),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.testTag("math_challenge_submit")
      ) {
        Icon(Icons.Default.Check, contentDescription = null, tint = ObsidianElevated, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = if (currentStep == 2) "सत्यापित कर अनलॉक करें" else "अगला प्रश्न →",
          color = ObsidianElevated,
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp
        )
      }
    },
    dismissButton = {
      OutlinedButton(
        onClick = onDismiss,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
      ) {
        Text("फोकस में बने रहें", color = WarmParchment, fontSize = 13.sp)
      }
    }
  )
}
