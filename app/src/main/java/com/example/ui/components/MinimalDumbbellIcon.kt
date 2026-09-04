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

/**
 * Minimalist, high-contrast dumbbell emblem matching the sleek MacroFactor aesthetic.
 */
@Composable
fun MinimalDumbbellIcon(
  modifier: Modifier = Modifier,
  size: Dp = 24.dp,
  tint: Color = Color.White,
  accentTint: Color = Color(0xFF94A3B8)
) {
  Canvas(modifier = modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height

    // Main Center Bar / Knurled Handle
    val barHeight = h * 0.15f
    val barY = (h - barHeight) / 2f
    val barStart = w * 0.28f
    val barWidth = w * 0.44f

    drawRoundRect(
      color = tint,
      topLeft = Offset(barStart, barY),
      size = Size(barWidth, barHeight),
      cornerRadius = CornerRadius(barHeight / 3f, barHeight / 3f)
    )

    // Handle Knurl Rings (High-contrast precision detailing)
    val ringWidth = w * 0.025f
    val ringHeight = barHeight * 1.3f
    val ringY = (h - ringHeight) / 2f

    // Inner Collars
    drawRoundRect(
      color = tint,
      topLeft = Offset(barStart - ringWidth, ringY),
      size = Size(ringWidth, ringHeight),
      cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
    )
    drawRoundRect(
      color = tint,
      topLeft = Offset(barStart + barWidth, ringY),
      size = Size(ringWidth, ringHeight),
      cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
    )

    // Left Inner Heavy Plate (Tall, high-contrast)
    val innerPlateW = w * 0.09f
    val innerPlateH = h * 0.76f
    val innerPlateY = (h - innerPlateH) / 2f
    drawRoundRect(
      color = tint,
      topLeft = Offset(w * 0.16f, innerPlateY),
      size = Size(innerPlateW, innerPlateH),
      cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
    )

    // Left Outer Plate (Slightly shorter, accent tint)
    val outerPlateW = w * 0.08f
    val outerPlateH = h * 0.54f
    val outerPlateY = (h - outerPlateH) / 2f
    drawRoundRect(
      color = accentTint,
      topLeft = Offset(w * 0.06f, outerPlateY),
      size = Size(outerPlateW, outerPlateH),
      cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
    )

    // Right Inner Heavy Plate (Tall, high-contrast)
    drawRoundRect(
      color = tint,
      topLeft = Offset(w * 0.75f, innerPlateY),
      size = Size(innerPlateW, innerPlateH),
      cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
    )

    // Right Outer Plate (Slightly shorter, accent tint)
    drawRoundRect(
      color = accentTint,
      topLeft = Offset(w * 0.86f, outerPlateY),
      size = Size(outerPlateW, outerPlateH),
      cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
    )
  }
}

