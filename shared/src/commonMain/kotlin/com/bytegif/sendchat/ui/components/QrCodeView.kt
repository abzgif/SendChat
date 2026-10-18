package com.bytegif.sendchat.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun QrCodeView(
    matrix: Array<BooleanArray>?,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    qrColor: Color = Color(0xFF0F172A),
    backgroundColor: Color = Color.White
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        if (matrix == null) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        } else {
            val rows = matrix.size
            val cols = if (rows > 0) matrix[0].size else 0

            Canvas(modifier = Modifier.fillMaxSize().aspectRatio(1f)) {
                if (rows > 0 && cols > 0) {
                    val cellWidth = this.size.width / cols
                    val cellHeight = this.size.height / rows

                    for (y in 0 until rows) {
                        for (x in 0 until cols) {
                            if (matrix[y][x]) {
                                drawRect(
                                    color = qrColor,
                                    topLeft = Offset(x * cellWidth, y * cellHeight),
                                    size = Size(cellWidth, cellHeight)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
