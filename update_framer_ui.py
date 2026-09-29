import os

app_path = 'app/src/main/java/com/music/bitchord/ui/classipod/ClassipodApp.kt'
new_app_content = '''package com.music.bitchord.ui.classipod

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.session.MediaController
import com.music.bitchord.playback.PlayerState

@Composable
fun ClassipodApp(
    controller: MediaController?,
    playerState: PlayerState
) {
    val context = LocalContext.current
    val menuState = remember { PodMenuState() }

    LaunchedEffect(Unit) {
        menuState.pushMenu("iPod", listOf(
            MenuItem("Now Playing", hasArrow = true) {
                menuState.isNowPlaying.value = true
            },
            MenuItem("Music", hasArrow = true) {
                // To be implemented
            },
            MenuItem("Play / Pause", hasArrow = false) {
                if (playerState.isPlaying) controller?.pause() else controller?.play()
            },
            MenuItem("Exit", hasArrow = false) {
                (context as? Activity)?.finish()
            }
        ))
    }

    // Outer Body (Glossy White iPod 5.5 Gen)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .shadow(16.dp, RoundedCornerShape(36.dp))
            .clip(RoundedCornerShape(36.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF), // Top-left highlight
                        Color(0xFFE8E8E8), // Base white
                        Color(0xFFD0D0D0)  // Bottom-right shadow
                    )
                )
            )
            .border(2.dp, Color(0xFFC0C0C0), RoundedCornerShape(36.dp)) // Subtle metallic outer edge
    ) {
        // Inner padding for components
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // Screen Outer Bezel (Black Glass)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.42f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF222222), Color(0xFF000000))
                        )
                    )
                    .border(2.dp, Color(0xFF444444), RoundedCornerShape(12.dp)) // Glass reflection edge
                    .padding(14.dp) // Bezel thickness
            ) {
                // LCD Screen itself
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(4.dp)) // LCDs have slightly rounded or sharp corners
                        .background(Color(0xFFF0F0F0)) // LCD base color
                ) {
                    PodScreen(controller, playerState, menuState)
                    
                    // LCD Inner shadow / Glare overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0x15000000), Color.Transparent, Color(0x05000000))
                                )
                            )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(36.dp))
            
            // Click Wheel area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.58f),
                contentAlignment = Alignment.TopCenter
            ) {
                ClickWheel(
                    onScroll = { ticks -> menuState.scroll(ticks) },
                    onClickMenu = {
                        if (menuState.isNowPlaying.value) {
                            menuState.isNowPlaying.value = false
                        } else {
                            menuState.popMenu()
                        }
                    },
                    onClickPlayPause = {
                        if (playerState.isPlaying) controller?.pause() else controller?.play()
                    },
                    onClickNext = { controller?.seekToNext() },
                    onClickPrev = { controller?.seekToPrevious() },
                    onClickCenter = {
                        if (!menuState.isNowPlaying.value && menuState.items.isNotEmpty()) {
                            menuState.items[menuState.selectedIndex.value].onClick()
                        }
                    }
                )
            }
        }
        
        // Optional: High-gloss reflection overlay on the top half of the body
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.4f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0x33FFFFFF), Color(0x00FFFFFF))
                    )
                )
        )
    }
}
'''
with open(app_path, 'w', encoding='utf-8') as f:
    f.write(new_app_content)

# 3. Update ClickWheel.kt for ultra-realistic UI
wheel_path = 'app/src/main/java/com/music/bitchord/ui/classipod/ClickWheel.kt'
new_wheel_content = '''package com.music.bitchord.ui.classipod

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
    onScroll: (Int) -> Unit,
    onClickMenu: () -> Unit,
    onClickPlayPause: () -> Unit,
    onClickNext: () -> Unit,
    onClickPrev: () -> Unit,
    onClickCenter: () -> Unit
) {
    val view = LocalView.current
    var lastAngle by remember { mutableStateOf(0f) }
    var accumulatedAngle by remember { mutableStateOf(0f) }

    // Outer wheel
    Box(
        modifier = Modifier
            .size(240.dp)
            .shadow(4.dp, CircleShape) // Drop shadow for the wheel
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFFFAFAFA), Color(0xFFEBEBEB)) // Matte white finish
                )
            )
            .border(1.dp, Color(0xFFD6D6D6), CircleShape)
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
                            onScroll(ticks)
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        }
                    }
                )
            }
    ) {
        // Center button
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(80.dp)
                .shadow(2.dp, CircleShape, clip = false) // Slight shadow inside the wheel hole
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFFB0B0B0), Color(0xFFD4D4D4)) // Recessed metallic gradient
                    )
                )
                .border(1.dp, Color(0xFF999999), CircleShape)
                .clickable { 
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    onClickCenter() 
                }
        )

        val textColor = Color(0xFF888888)
        val textWeight = FontWeight.ExtraBold

        // Menu (Top)
        Text(
            text = "MENU",
            color = textColor,
            fontWeight = textWeight,
            fontSize = 15.sp,
            letterSpacing = 1.sp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 22.dp)
                .clickable { onClickMenu() }
        )

        // Play/Pause (Bottom)
        Text(
            text = "▶ ||",
            color = textColor,
            fontWeight = textWeight,
            fontSize = 14.sp,
            letterSpacing = 2.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 22.dp)
                .clickable { onClickPlayPause() }
        )

        // Prev (Left)
        Text(
            text = "|◀",
            color = textColor,
            fontWeight = textWeight,
            fontSize = 15.sp,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 22.dp)
                .clickable { onClickPrev() }
        )

        // Next (Right)
        Text(
            text = "▶|",
            color = textColor,
            fontWeight = textWeight,
            fontSize = 15.sp,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 22.dp)
                .clickable { onClickNext() }
        )
    }
}
'''
with open(wheel_path, 'w', encoding='utf-8') as f:
    f.write(new_wheel_content)
