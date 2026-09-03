package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.BorderHighlight
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CardDark
import com.example.ui.theme.MatteBlack
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TitaniumSilver
import com.example.ui.theme.TitaniumWhite

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddCustomExerciseDialog(
  onDismiss: () -> Unit,
  onSave: (name: String, category: String, primaryMuscle: String) -> Boolean
) {
  var name by remember { mutableStateOf("") }
  var category by remember { mutableStateOf("Chest") }
  var primaryMuscle by remember { mutableStateOf("") }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  val categories = listOf("Chest", "Back", "Legs", "Shoulders", "Arms", "Core", "Full Body")

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = CardDark,
      border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .testTag("add_custom_exercise_dialog")
    ) {
      Column(
        modifier = Modifier.padding(20.dp)
      ) {
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
                .background(SurfaceDark)
                .border(1.dp, BorderSubtle, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                Icons.Default.Add,
                contentDescription = null,
                tint = TitaniumSilver,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Add Exercise",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
              )
              Text(
                text = "Add to your personal library",
                fontSize = 12.sp,
                color = TextSecondary
              )
            }
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "EXERCISE NAME",
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold,
          color = TitaniumSilver,
          letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
          value = name,
          onValueChange = {
            name = it
            errorMessage = null
          },
          placeholder = { Text("e.g. Pendlay Row, Hack Squat...", color = TextSecondary) },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("custom_exercise_name_input"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedBorderColor = TitaniumWhite,
            unfocusedBorderColor = BorderSubtle,
            focusedContainerColor = SurfaceDark,
            unfocusedContainerColor = SurfaceDark
          ),
          shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = "MUSCLE GROUP / CATEGORY",
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold,
          color = TitaniumSilver,
          letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          categories.forEach { cat ->
            val isSelected = category == cat
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isSelected) TitaniumWhite else SurfaceDark)
                .border(
                  width = 1.dp,
                  color = if (isSelected) TitaniumWhite else BorderSubtle,
                  shape = RoundedCornerShape(8.dp)
                )
                .clickable { category = cat }
                .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
              Text(
                text = cat,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MatteBlack else TextSecondary
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = "PRIMARY MUSCLE / EQUIPMENT (OPTIONAL)",
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold,
          color = TitaniumSilver,
          letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
          value = primaryMuscle,
          onValueChange = { primaryMuscle = it },
          placeholder = { Text("e.g. Lats, Upper Chest, Barbell...", color = TextSecondary) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedBorderColor = TitaniumWhite,
            unfocusedBorderColor = BorderSubtle,
            focusedContainerColor = SurfaceDark,
            unfocusedContainerColor = SurfaceDark
          ),
          shape = RoundedCornerShape(12.dp)
        )

        if (errorMessage != null) {
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = errorMessage ?: "",
            color = Color(0xFFEF4444),
            fontSize = 12.sp
          )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
          onClick = {
            if (name.isBlank()) {
              errorMessage = "Please enter an exercise name"
              return@Button
            }
            val success = onSave(name.trim(), category, primaryMuscle.trim())
            if (success) {
              onDismiss()
            } else {
              errorMessage = "This exercise already exists in your library"
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("save_custom_exercise_btn"),
          colors = ButtonDefaults.buttonColors(
            containerColor = TitaniumWhite,
            contentColor = MatteBlack
          ),
          shape = RoundedCornerShape(14.dp)
        ) {
          Text("Save & Add to Library", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
      }
    }
  }
}
