package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JomCyan
import com.example.ui.theme.JomDeepBlue
import com.example.ui.theme.JomRoyalBlue
import com.example.ui.theme.SpaceGroteskFontFamily

@Composable
fun JomBrandEmblem(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    showText: Boolean = true
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1A56FF), JomRoyalBlue, JomDeepBlue)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val minDim = minOf(w, h)
            val strokeOuter = minDim * 0.065f
            val strokeInner = minDim * 0.055f

            // Outer Interlocking Cyan & White Swirl Rings
            val outerPad = minDim * 0.14f
            val outerSize = Size(w - outerPad * 2, h - outerPad * 2)

            drawArc(
                color = JomCyan,
                startAngle = 140f,
                sweepAngle = 190f,
                useCenter = false,
                topLeft = Offset(outerPad, outerPad),
                size = outerSize,
                style = Stroke(width = strokeOuter, cap = StrokeCap.Round)
            )

            drawArc(
                color = Color.White,
                startAngle = -35f,
                sweepAngle = 175f,
                useCenter = false,
                topLeft = Offset(outerPad, outerPad),
                size = outerSize,
                style = Stroke(width = strokeOuter, cap = StrokeCap.Round)
            )

            // Middle Sky-Blue Interlocking Ring
            val midPad = minDim * 0.22f
            val midSize = Size(w - midPad * 2, h - midPad * 2)
            drawArc(
                color = Color(0xFF29B6F6),
                startAngle = 35f,
                sweepAngle = 285f,
                useCenter = false,
                topLeft = Offset(midPad, midPad),
                size = midSize,
                style = Stroke(width = strokeInner, cap = StrokeCap.Round)
            )

            // Inner Core Cyan Ring framing "Jom!"
            val innerPad = minDim * 0.29f
            val innerSize = Size(w - innerPad * 2, h - innerPad * 2)
            drawArc(
                color = JomCyan,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(innerPad, innerPad),
                size = innerSize,
                style = Stroke(width = strokeInner, cap = StrokeCap.Round)
            )

            // Glowing Cyan Plus (+) Symbol at Top-Right
            val plusCenterX = w * 0.77f
            val plusCenterY = h * 0.23f
            val plusArm = minDim * 0.075f
            drawLine(
                color = JomCyan,
                start = Offset(plusCenterX - plusArm, plusCenterY),
                end = Offset(plusCenterX + plusArm, plusCenterY),
                strokeWidth = strokeOuter,
                cap = StrokeCap.Round
            )
            drawLine(
                color = JomCyan,
                start = Offset(plusCenterX, plusCenterY - plusArm),
                end = Offset(plusCenterX, plusCenterY + plusArm),
                strokeWidth = strokeOuter,
                cap = StrokeCap.Round
            )
        }

        if (showText) {
            Text(
                text = "Jom!",
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 15.sp
            )
        }
    }
}
