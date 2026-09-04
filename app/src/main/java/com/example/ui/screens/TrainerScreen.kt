package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderHighlight
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CardDark
import com.example.ui.theme.CardElevated
import com.example.ui.theme.MatteBlack
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TitaniumSilver
import com.example.ui.theme.TitaniumWhite
import com.example.viewmodel.GymViewModel
import com.example.viewmodel.TrainerMessage

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.example.data.GeminiChatModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrainerScreen(
  viewModel: GymViewModel,
  modifier: Modifier = Modifier
) {
  val messages by viewModel.trainerMessages.collectAsState()
  val isTyping by viewModel.isTrainerTyping.collectAsState()
  val profile by viewModel.userProfile.collectAsState()
  val isOnline by viewModel.isOnline.collectAsState()
  val selectedModel by viewModel.selectedChatModel.collectAsState()

  var inputText by remember { mutableStateOf("") }
  val listState = rememberLazyListState()

  LaunchedEffect(messages.size, isTyping) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.size - 1)
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MatteBlack)
      .windowInsetsPadding(WindowInsets.navigationBars)
      .imePadding()
      .testTag("trainer_screen")
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Top Jim Header
      Surface(
        color = SurfaceDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(44.dp)
                  .clip(CircleShape)
                  .background(
                    Brush.linearGradient(
                      listOf(Color(0xFF2E3440), Color(0xFF1E222B))
                    )
                  )
                  .border(1.5.dp, TitaniumSilver, CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "JIM",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Black,
                  color = TitaniumWhite,
                  letterSpacing = 1.sp
                )
              }

              Spacer(modifier = Modifier.width(12.dp))

              Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = "JIM • AI TRAINER",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = 0.5.sp
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Box(
                    modifier = Modifier
                      .clip(RoundedCornerShape(6.dp))
                      .background(Color(0xFF10B981).copy(alpha = 0.15f))
                      .border(1.dp, Color(0xFF10B981).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                      .padding(horizontal = 6.dp, vertical = 2.dp)
                  ) {
                    Text(
                      text = if (isOnline) "GEMINI ONLINE" else "LOCAL ENGINE",
                      fontSize = 9.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFF10B981)
                    )
                  }
                }
                Text(
                  text = "Personal Biomechanics & Overload Strategist",
                  fontSize = 12.sp,
                  color = TextSecondary
                )
              }
            }

            IconButton(
              onClick = { viewModel.resetTrainerChat() },
              modifier = Modifier.size(36.dp)
            ) {
              Icon(Icons.Default.Refresh, contentDescription = "Reset Chat", tint = TextSecondary)
            }
          }

          // Gemini Model Selector
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState())
              .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            GeminiChatModel.values().forEach { model ->
              val isSelected = selectedModel == model
              Surface(
                onClick = { viewModel.setChatModel(model) },
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) CardElevated else Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(
                  1.dp,
                  if (isSelected) TitaniumSilver else BorderSubtle
                )
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Box(
                    modifier = Modifier
                      .size(6.dp)
                      .clip(CircleShape)
                      .background(if (isSelected) Color(0xFF10B981) else TextSecondary)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = model.displayName,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) TitaniumWhite else TextSecondary
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "• ${model.badge}",
                    fontSize = 10.sp,
                    color = if (isSelected) TitaniumSilver else TextSecondary.copy(alpha = 0.7f)
                  )
                }
              }
            }
          }
          Spacer(modifier = Modifier.height(4.dp))
        }
      }

      // Conversation Stream
      LazyColumn(
        state = listState,
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        item {
          // Coach Insight Banner
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = CardDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  Icons.Default.Psychology,
                  contentDescription = null,
                  tint = TitaniumSilver,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "JIM'S STRATEGY FOCUS",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = TitaniumSilver,
                  letterSpacing = 1.sp
                )
              }
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "I analyze your volume, progressive overload, mechanical tension, and recovery metrics. Ask me any biomechanics question, request custom mesocycle programming, or tap a suggestion below.",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 17.sp
              )
            }
          }
        }

        items(messages, key = { it.id }) { msg ->
          MessageBubble(
            message = msg,
            onFollowUpClicked = { followUpText ->
              viewModel.sendTrainerMessage(followUpText)
            }
          )
        }

        if (isTyping) {
          item {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(start = 12.dp, top = 4.dp)
            ) {
              CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = TitaniumSilver
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "Jim is analyzing biomechanics...",
                fontSize = 12.sp,
                color = TextSecondary
              )
            }
          }
        }
      }

      // Bottom Input Bar & Suggestions
      Surface(
        color = SurfaceDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          // Quick prompt chips
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 10.dp)
          ) {
            listOf(
              "Check recovery today",
              "Bench press sticking point",
              "Design 4-day hypertrophy split",
              "Deload week signs"
            ).forEach { prompt ->
              Surface(
                onClick = { viewModel.sendTrainerMessage(prompt) },
                shape = RoundedCornerShape(10.dp),
                color = CardElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    Icons.Default.ArrowOutward,
                    contentDescription = null,
                    tint = TitaniumSilver,
                    modifier = Modifier.size(12.dp)
                  )
                  Spacer(modifier = Modifier.width(5.dp))
                  Text(
                    text = prompt,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                  )
                }
              }
            }
          }

          // Text Field & Send
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            OutlinedTextField(
              value = inputText,
              onValueChange = { inputText = it },
              placeholder = { Text("Ask Jim (e.g. How to break my bench press plateau?)", color = TextSecondary, fontSize = 13.sp) },
              modifier = Modifier
                .weight(1f)
                .testTag("trainer_input_field"),
              shape = RoundedCornerShape(14.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CardDark,
                unfocusedContainerColor = CardDark,
                focusedBorderColor = TitaniumWhite,
                unfocusedBorderColor = BorderSubtle,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
              ),
              maxLines = 3
            )

            Spacer(modifier = Modifier.width(10.dp))

            Surface(
              onClick = {
                if (inputText.isNotBlank()) {
                  val text = inputText.trim()
                  inputText = ""
                  viewModel.sendTrainerMessage(text)
                }
              },
              shape = CircleShape,
              color = if (inputText.isNotBlank()) TitaniumWhite else CardElevated,
              border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight),
              modifier = Modifier
                .size(48.dp)
                .testTag("send_trainer_message_btn")
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  Icons.AutoMirrored.Filled.Send,
                  contentDescription = "Send",
                  tint = if (inputText.isNotBlank()) MatteBlack else TextSecondary,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MessageBubble(
  message: TrainerMessage,
  onFollowUpClicked: (String) -> Unit
) {
  val isJim = message.sender == "JIM" || message.sender == "JIIM AI" || message.sender == "JJ" || message.sender == "COACH"

  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = if (isJim) Alignment.Start else Alignment.End
  ) {
    if (isJim) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
      ) {
        Text(
          text = "JIM",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = TitaniumSilver,
          letterSpacing = 0.5.sp
        )
        if (!message.modelTag.isNullOrBlank()) {
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "• ${message.modelTag}",
            fontSize = 9.sp,
            color = TextSecondary
          )
        }
      }
    }

    Surface(
      shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isJim) 4.dp else 16.dp,
        bottomEnd = if (isJim) 16.dp else 4.dp
      ),
      color = if (isJim) CardDark else SurfaceDark,
      border = androidx.compose.foundation.BorderStroke(
        1.dp,
        if (isJim) BorderHighlight else BorderSubtle
      ),
      modifier = Modifier
        .fillMaxWidth(if (isJim) 0.95f else 0.85f)
        .testTag("chat_bubble_${message.id}")
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Text(
          text = message.text,
          fontSize = 14.sp,
          color = TextPrimary,
          lineHeight = 20.sp
        )

        // Probing follow-up chips if provided by Jim
        if (isJim && message.promptFollowUps.isNotEmpty()) {
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "TAP QUICK RESPONSE:",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = TitaniumSilver,
            letterSpacing = 1.sp
          )
          Spacer(modifier = Modifier.height(6.dp))
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            message.promptFollowUps.forEach { followUp ->
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .background(CardElevated)
                  .border(1.dp, BorderHighlight, RoundedCornerShape(8.dp))
                  .clickable { onFollowUpClicked(followUp) }
                  .padding(horizontal = 10.dp, vertical = 6.dp)
              ) {
                Text(
                  text = followUp,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Medium,
                  color = TitaniumWhite
                )
              }
            }
          }
        }
      }
    }
  }
}
