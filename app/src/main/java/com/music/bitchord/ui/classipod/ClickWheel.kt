package com.music.bitchord.ui.classipod

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }
    
    val hapticTick = {
        val hapticEnabled = Settings.System.getInt(context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) == 1
        if (hapticEnabled) {
            try {
                val audioAttrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val vibAttrs = VibrationAttributes.Builder()
                        .setUsage(VibrationAttributes.USAGE_ALARM)
                        .build()
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK), vibAttrs)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK), audioAttrs)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(10, audioAttrs)
                }
            } catch (e: Exception) {}
        }
    }
    
    val hapticClick = {
        val hapticEnabled = Settings.System.getInt(context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) == 1
        if (hapticEnabled) {
            try {
                val audioAttrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val vibAttrs = VibrationAttributes.Builder()
                        .setUsage(VibrationAttributes.USAGE_ALARM)
                        .build()
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK), vibAttrs)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK), audioAttrs)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(20, audioAttrs)
                }
            } catch (e: Exception) {}
        }
    }
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
                                hapticTick()
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
                    hapticClick()
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
                    hapticClick()
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
                    hapticClick()
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
                    hapticClick()
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
                    hapticClick()
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
