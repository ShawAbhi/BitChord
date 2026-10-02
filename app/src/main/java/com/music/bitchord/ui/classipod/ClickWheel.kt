package com.music.bitchord.ui.classipod

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.atan2

@Composable
fun ClickWheel(
    onScroll: (Int) -> Boolean,
    onClickMenu: () -> Unit,
    onClickPlayPause: () -> Unit,
    onClickNext: () -> Unit,
    onClickPrev: () -> Unit,
    onClickCenter: () -> Unit
) {
    val view = LocalView.current
    val currentOnScroll by rememberUpdatedState(onScroll)
    var lastAngle by remember { mutableStateOf(0f) }
    var accumulatedAngle by remember { mutableStateOf(0f) }

    // Outer wheel (Grey)
    Box(
        modifier = Modifier
            .size(240.dp)
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFEAEAEA), Color(0xFFD4D4D4)) // Light metallic grey
                )
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val x = offset.x - size.width / 2
                        val y = offset.y - size.height / 2
                        lastAngle = Math.toDegrees(atan2(y.toDouble(), x.toDouble())).toFloat()
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val x = change.position.x - size.width / 2
                        val y = change.position.y - size.height / 2
                        val currentAngle = Math.toDegrees(atan2(y.toDouble(), x.toDouble())).toFloat()
                        
                        var delta = currentAngle - lastAngle
                        if (delta > 180) delta -= 360
                        if (delta < -180) delta += 360
                        
                        accumulatedAngle += delta
                        lastAngle = currentAngle

                        if (Math.abs(accumulatedAngle) > 15f) {
                            val ticks = (accumulatedAngle / 15f).toInt()
                            accumulatedAngle -= ticks * 15f
                            if (currentOnScroll(ticks)) {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING)
                            }
                        }
                    }
                )
            }
    ) {
        // Center button (Pure White)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(80.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFFFFF))
                .clickable { 
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING)
                    onClickCenter() 
                }
        )

        val textColor = Color(0xFF8B8B8B)
        val textWeight = FontWeight.Bold

        // Menu (Top)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
                .size(60.dp)
                .clip(CircleShape)
                .clickable { 
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING)
                    onClickMenu() 
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "MENU",
                color = textColor,
                fontWeight = textWeight,
                fontSize = 15.sp
            )
        }

        // Play/Pause (Bottom)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)
                .size(60.dp)
                .clip(CircleShape)
                .clickable { 
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING)
                    onClickPlayPause() 
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "⏯",
                color = textColor,
                fontWeight = textWeight,
                fontSize = 18.sp
            )
        }

        // Prev (Left)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 10.dp)
                .size(60.dp)
                .clip(CircleShape)
                .clickable { 
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING)
                    onClickPrev() 
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "⏮",
                color = textColor,
                fontWeight = textWeight,
                fontSize = 18.sp
            )
        }

        // Next (Right)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp)
                .size(60.dp)
                .clip(CircleShape)
                .clickable { 
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING)
                    onClickNext() 
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "⏭",
                color = textColor,
                fontWeight = textWeight,
                fontSize = 18.sp
            )
        }
    }
}
