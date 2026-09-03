package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CardElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.TitaniumWhite
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@Composable
fun RepWheelPicker(
  reps: Int,
  onRepsChange: (Int) -> Unit,
  minReps: Int = 1,
  maxReps: Int = 35,
  modifier: Modifier = Modifier
) {
  val repRange = remember(minReps, maxReps) { (minReps..maxReps).toList() }
  val itemHeight = 44.dp
  val visibleItemsCount = 3
  val totalHeight = itemHeight * visibleItemsCount

  val initialIndex = (reps - minReps).coerceIn(0, repRange.lastIndex)
  val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
  val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
  val coroutineScope = rememberCoroutineScope()

  // Track the center index as the user scrolls
  val centerItemIndex by remember {
    derivedStateOf {
      val firstVisible = listState.firstVisibleItemIndex
      val offset = listState.firstVisibleItemScrollOffset
      if (offset > 22) firstVisible + 1 else firstVisible
    }
  }

  // Notify parent when user scrolls to a new center rep
  LaunchedEffect(listState) {
    snapshotFlow { centerItemIndex }
      .distinctUntilChanged()
      .filter { it in repRange.indices }
      .collect { index ->
        val newReps = repRange[index]
        if (newReps != reps) {
          onRepsChange(newReps)
        }
      }
  }

  // Keep list state in sync if parent changes reps from outside (e.g. +/- buttons)
  LaunchedEffect(reps) {
    val targetIndex = (reps - minReps).coerceIn(0, repRange.lastIndex)
    if (listState.firstVisibleItemIndex != targetIndex && !listState.isScrollInProgress) {
      listState.animateScrollToItem(targetIndex)
    }
  }

  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      text = "REPS",
      fontSize = 11.sp,
      fontWeight = FontWeight.SemiBold,
      letterSpacing = 1.sp,
      color = TextSecondary
    )

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
      modifier = Modifier.padding(top = 6.dp)
    ) {
      // Step down button
      Surface(
        onClick = {
          if (reps > minReps) {
            val next = reps - 1
            onRepsChange(next)
            coroutineScope.launch {
              listState.animateScrollToItem((next - minReps).coerceAtLeast(0))
            }
          }
        },
        shape = CircleShape,
        color = CardElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.size(36.dp).testTag("decrease_reps_button")
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Default.Remove,
            contentDescription = "Decrease Reps",
            tint = TextPrimary,
            modifier = Modifier.size(16.dp)
          )
        }
      }

      // Vertical Scroll Wheel
      Box(
        modifier = Modifier
          .padding(horizontal = 10.dp)
          .width(76.dp)
          .height(totalHeight)
          .background(Color(0xFF0F1014), RoundedCornerShape(12.dp))
          .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
      ) {
        // Selection highlight band in center
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(itemHeight)
            .padding(horizontal = 6.dp)
            .background(Color(0xFF1E212B), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF3E4354), RoundedCornerShape(8.dp))
        )

        LazyColumn(
          state = listState,
          flingBehavior = flingBehavior,
          contentPadding = PaddingValues(vertical = itemHeight),
          modifier = Modifier
            .fillMaxWidth()
            .height(totalHeight)
            .testTag("reps_scroll_wheel")
        ) {
          items(repRange.size) { index ->
            val repVal = repRange[index]
            val isCurrent = repVal == reps
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .clickable {
                  onRepsChange(repVal)
                  coroutineScope.launch {
                    listState.animateScrollToItem(index)
                  }
                },
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "$repVal",
                fontSize = if (isCurrent) 20.sp else 14.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = if (isCurrent) TitaniumWhite else TextTertiary,
                modifier = Modifier.alpha(if (isCurrent) 1f else 0.4f)
              )
            }
          }
        }
      }

      // Step up button
      Surface(
        onClick = {
          if (reps < maxReps) {
            val next = reps + 1
            onRepsChange(next)
            coroutineScope.launch {
              listState.animateScrollToItem((next - minReps).coerceAtMost(repRange.lastIndex))
            }
          }
        },
        shape = CircleShape,
        color = CardElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.size(36.dp).testTag("increase_reps_button")
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Increase Reps",
            tint = TextPrimary,
            modifier = Modifier.size(16.dp)
          )
        }
      }
    }
  }
}
