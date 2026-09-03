package com.example.ui.components

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import com.example.model.ExerciseDefinition
import com.example.model.ExerciseLibrary
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseSelectorDialog(
  onDismiss: () -> Unit,
  onSelectExercise: (ExerciseDefinition) -> Unit,
  allAvailableExercises: List<ExerciseDefinition> = ExerciseLibrary.allExercises,
  onAddNewCustomExercise: () -> Unit = {}
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var searchQuery by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf("All") }

  val categories = listOf("All", "Chest", "Back", "Legs", "Shoulders", "Arms", "Core", "Full Body")

  val filteredExercises = remember(searchQuery, selectedCategory, allAvailableExercises) {
    allAvailableExercises.filter { def ->
      val matchesCat = selectedCategory == "All" || def.category.equals(selectedCategory, ignoreCase = true)
      val matchesSearch = searchQuery.isBlank() || def.name.contains(searchQuery, ignoreCase = true) ||
        def.primaryMuscle.contains(searchQuery, ignoreCase = true)
      matchesCat && matchesSearch
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MatteBlack,
    tonalElevation = 8.dp
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(0.85f)
        .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Select Exercise",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          Text(
            text = "${allAvailableExercises.size} exercises in your library",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
          )
        }

        // Dedicated in-built Add Custom Exercise Button
        Surface(
          onClick = {
            onDismiss()
            onAddNewCustomExercise()
          },
          shape = RoundedCornerShape(12.dp),
          color = SurfaceDark,
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight),
          modifier = Modifier.testTag("open_add_custom_exercise_btn")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = TitaniumWhite, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("+ Custom", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TitaniumWhite)
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Search field
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search by name or muscle (e.g. Bench, Squat)", color = TextSecondary) },
        leadingIcon = {
          Icon(Icons.Default.Search, contentDescription = "Search", tint = TitaniumSilver)
        },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = CardDark,
          unfocusedContainerColor = CardDark,
          focusedBorderColor = TitaniumWhite,
          unfocusedBorderColor = BorderSubtle,
          focusedTextColor = TextPrimary,
          unfocusedTextColor = TextPrimary
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("exercise_search_input")
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Category filter chips
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        items(categories) { category ->
          val isSelected = selectedCategory == category
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(10.dp))
              .background(if (isSelected) TitaniumWhite else CardDark)
              .border(
                width = 1.dp,
                color = if (isSelected) TitaniumWhite else BorderSubtle,
                shape = RoundedCornerShape(10.dp)
              )
              .clickable { selectedCategory = category }
              .padding(horizontal = 14.dp, vertical = 7.dp)
              .testTag("category_chip_$category")
          ) {
            Text(
              text = category,
              style = MaterialTheme.typography.labelMedium,
              color = if (isSelected) MatteBlack else TextSecondary,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Quick add if search query doesn't match
      if (searchQuery.isNotBlank() && filteredExercises.none { it.name.equals(searchQuery.trim(), ignoreCase = true) }) {
        Card(
          onClick = {
            onDismiss()
            onAddNewCustomExercise()
          },
          colors = CardDefaults.cardColors(containerColor = CardElevated),
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .testTag("add_custom_exercise_prompt")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(SurfaceDark)
                .border(1.dp, BorderSubtle, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Add, contentDescription = null, tint = TitaniumWhite)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Add \"${searchQuery.trim()}\"",
                fontWeight = FontWeight.Bold,
                color = TitaniumWhite,
                fontSize = 15.sp
              )
              Text(
                text = "Tap to save directly into your custom exercises",
                color = TextSecondary,
                fontSize = 12.sp
              )
            }
          }
        }
      }

      // Exercise List
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
      ) {
        items(filteredExercises) { exercise ->
          Card(
            onClick = { onSelectExercise(exercise) },
            colors = CardDefaults.cardColors(containerColor = CardDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("exercise_item_${exercise.name}")
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(38.dp)
                  .clip(CircleShape)
                  .background(SurfaceDark)
                  .border(1.dp, BorderSubtle, CircleShape),
                contentAlignment = Alignment.Center
              ) {
                MinimalDumbbellIcon(
                  tint = TitaniumSilver,
                  modifier = Modifier.size(18.dp)
                )
              }

              Spacer(modifier = Modifier.width(12.dp))

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = exercise.name,
                  fontWeight = FontWeight.Bold,
                  color = TextPrimary,
                  fontSize = 15.sp
                )
                Text(
                  text = "${exercise.category} • ${exercise.primaryMuscle}",
                  color = TextSecondary,
                  fontSize = 12.sp
                )
              }

              Icon(
                Icons.Default.Add,
                contentDescription = "Select",
                tint = TitaniumSilver,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }
      }
    }
  }
}
