package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun MinimalDumbbellIcon(
  modifier: Modifier = Modifier,
  size: Dp = 24.dp,
  tint: Color = Color.White,
  accentTint: Color = Color(0xFF9CA3AF)
) {
  Canvas(modifier = modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height

    // Bar / Handle
    val barHeight = h * 0.14f
    val barY = (h - barHeight) / 2f
    drawRoundRect(
      color = tint,
      topLeft = Offset(w * 0.28f, barY),
      size = Size(w * 0.44f, barHeight),
      cornerRadius = CornerRadius(barHeight / 2f, barHeight / 2f)
    )

    // Left Inner Plate
    val innerPlateW = w * 0.09f
    val innerPlateH = h * 0.68f
    val innerPlateY = (h - innerPlateH) / 2f
    drawRoundRect(
      color = tint,
      topLeft = Offset(w * 0.24f, innerPlateY),
      size = Size(innerPlateW, innerPlateH),
      cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
    )

    // Left Outer Plate
    val outerPlateW = w * 0.08f
    val outerPlateH = h * 0.48f
    val outerPlateY = (h - outerPlateH) / 2f
    drawRoundRect(
      color = accentTint,
      topLeft = Offset(w * 0.14f, outerPlateY),
      size = Size(outerPlateW, outerPlateH),
      cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
    )

    // Right Inner Plate
    drawRoundRect(
      color = tint,
      topLeft = Offset(w * 0.67f, innerPlateY),
      size = Size(innerPlateW, innerPlateH),
      cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
    )

    // Right Outer Plate
    drawRoundRect(
      color = accentTint,
      topLeft = Offset(w * 0.78f, outerPlateY),
      size = Size(outerPlateW, outerPlateH),
      cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
    )
  }
}
