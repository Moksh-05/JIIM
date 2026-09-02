package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ExerciseDefinition
import com.example.model.ExerciseLibrary
import com.example.ui.theme.VoltLime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseSelectorDialog(
  onDismiss: () -> Unit,
  onSelectExercise: (ExerciseDefinition) -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var searchQuery by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf("All") }

  val categories = listOf("All", "Chest", "Back", "Legs", "Shoulders", "Arms", "Core")

  val filteredExercises = remember(searchQuery, selectedCategory) {
    ExerciseLibrary.allExercises.filter { def ->
      val matchesCat = selectedCategory == "All" || def.category.equals(selectedCategory, ignoreCase = true)
      val matchesSearch = searchQuery.isBlank() || def.name.contains(searchQuery, ignoreCase = true) ||
        def.primaryMuscle.contains(searchQuery, ignoreCase = true)
      matchesCat && matchesSearch
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = Color(0xFF101723),
    tonalElevation = 6.dp
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(0.85f)
        .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
      Text(
        text = "Add Exercise",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = Color.White
      )
      Text(
        text = "Choose from the library or create a custom lift",
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFF94A3B8)
      )

      Spacer(modifier = Modifier.height(14.dp))

      // Search field
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search exercise (e.g. Bench, Squat)", color = Color(0xFF64748B)) },
        leadingIcon = {
          Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF94A3B8))
        },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = Color(0xFF172233),
          unfocusedContainerColor = Color(0xFF172233),
          focusedBorderColor = VoltLime,
          unfocusedBorderColor = Color(0xFF2B3A52),
          focusedTextColor = Color.White,
          unfocusedTextColor = Color.White
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
          Surface(
            onClick = { selectedCategory = category },
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected) VoltLime else Color(0xFF1B2638),
            modifier = Modifier.testTag("category_chip_$category")
          ) {
            Text(
              text = category,
              style = MaterialTheme.typography.labelMedium,
              color = if (isSelected) Color.Black else Color.White,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Custom exercise creation option if search query doesn't match fully
      if (searchQuery.isNotBlank() && filteredExercises.none { it.name.equals(searchQuery.trim(), ignoreCase = true) }) {
        Card(
          onClick = {
            val custom = ExerciseDefinition(
              name = searchQuery.trim(),
              category = if (selectedCategory == "All") "Custom" else selectedCategory,
              primaryMuscle = "Target Muscle"
            )
            onSelectExercise(custom)
          },
          colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2B3E)),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .testTag("add_custom_exercise_card")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .background(VoltLime.copy(alpha = 0.2f), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Add, contentDescription = null, tint = VoltLime)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Add \"${searchQuery.trim()}\"",
                fontWeight = FontWeight.Bold,
                color = VoltLime,
                fontSize = 15.sp
              )
              Text(
                text = "Create custom exercise entry",
                color = Color(0xFF94A3B8),
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
            colors = CardDefaults.cardColors(containerColor = Color(0xFF151E2B)),
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
                  .background(Color(0xFF223046), CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  Icons.Default.FitnessCenter,
                  contentDescription = null,
                  tint = VoltLime,
                  modifier = Modifier.size(18.dp)
                )
              }

              Spacer(modifier = Modifier.width(12.dp))

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = exercise.name,
                  fontWeight = FontWeight.Bold,
                  color = Color.White,
                  fontSize = 15.sp
                )
                Text(
                  text = "${exercise.category} • ${exercise.primaryMuscle}",
                  color = Color(0xFF94A3B8),
                  fontSize = 12.sp
                )
              }

              Icon(
                Icons.Default.Add,
                contentDescription = "Select",
                tint = VoltLime,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }
      }
    }
  }
}
