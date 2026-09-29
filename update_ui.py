import os

# 1. Update FloatingPodService.kt to fix dimensions and sharp edges
service_path = 'app/src/main/java/com/music/bitchord/ui/classipod/FloatingPodService.kt'
with open(service_path, 'r', encoding='utf-8') as f:
    service_content = f.read()

old_expanded = '''                        // Expanded UI
                        Box(
                            modifier = Modifier
                                .size(width = 320.dp, height = 500.dp)
                                .background(Color(0xFFE0E0E0))
                        ) {
                            ClassipodApp(controller, playerState)
                            // Close button
                            Box(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .align(Alignment.TopEnd)
                                    .size(32.dp)
                                    .background(Color.Red, CircleShape)
                                    .clickable { isExpanded = false },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("X", color = Color.White)
                            }
                        }'''

new_expanded = '''                        // Expanded UI
                        Box(
                            modifier = Modifier
                                .size(width = 340.dp, height = 540.dp)
                                .padding(12.dp) // Space for shadow/close button
                        ) {
                            ClassipodApp(controller, playerState)
                            // Close button floating outside the main body
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(30.dp)
                                    .background(Color(0x99000000), CircleShape)
                                    .clickable { isExpanded = false },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("X", color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            }
                        }'''

if old_expanded in service_content:
    service_content = service_content.replace(old_expanded, new_expanded)
    with open(service_path, 'w', encoding='utf-8') as f:
        f.write(service_content)

# 2. Update ClassipodApp.kt
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
        menuState.pushMenu("BitChord", listOf(
            MenuItem("Now Playing", hasArrow = true) {
                menuState.isNowPlaying.value = true
            },
            MenuItem("Play / Pause", hasArrow = false) {
                if (playerState.isPlaying) controller?.pause() else controller?.play()
            },
            MenuItem("Next Track", hasArrow = false) {
                controller?.seekToNext()
            },
            MenuItem("Exit Classipod", hasArrow = false) {
                (context as? Activity)?.finish()
            }
        ))
    }

    // iPod 5.5 Gen Body
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFF9F9F9), Color(0xFFE5E5E5)) // Glossy white plastic
                )
            )
            .border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(32.dp)) // Subtle edge
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Screen Bezel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.45f) // 45% height for screen
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black)
                .border(2.dp, Color(0xFF333333), RoundedCornerShape(8.dp)) // Inner dark bezel
                .padding(12.dp) // Bezel thickness
        ) {
            // Actual LCD Screen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                PodScreen(controller, playerState, menuState)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Click Wheel area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.55f),
            contentAlignment = Alignment.Center
        ) {
            ClickWheel(
                onScroll = { ticks ->
                    menuState.scroll(ticks)
                },
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
                onClickNext = {
                    controller?.seekToNext()
                },
                onClickPrev = {
                    controller?.seekToPrevious()
                },
                onClickCenter = {
                    if (!menuState.isNowPlaying.value && menuState.items.isNotEmpty()) {
                        menuState.items[menuState.selectedIndex.value].onClick()
                    }
                }
            )
        }
    }
}
'''
with open(app_path, 'w', encoding='utf-8') as f:
    f.write(new_app_content)

# 3. Update ClickWheel.kt
wheel_path = 'app/src/main/java/com/music/bitchord/ui/classipod/ClickWheel.kt'
new_wheel_content = '''package com.music.bitchord.ui.classipod

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
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

    Box(
        modifier = Modifier
            .size(220.dp) // Standard iPod wheel size
            .clip(CircleShape)
            .background(Color(0xFFF2F2F2)) // Very light grey matte finish
            .border(1.dp, Color(0xFFDDDDDD), CircleShape) // Subtle recessed look
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

                        // Trigger scroll tick every 15 degrees
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
                .size(76.dp)
                .clip(CircleShape)
                .background(Color(0xFFC7C7C7)) // Grey center button
                .border(1.dp, Color(0xFFB0B0B0), CircleShape)
                .clickable { 
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    onClickCenter() 
                }
        )

        val textColor = Color(0xFF777777)

        // Menu (Top)
        Text(
            text = "MENU",
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            letterSpacing = 1.sp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 18.dp)
                .clickable { onClickMenu() }
        )

        // Play/Pause (Bottom)
        Text(
            text = "▶ ||",
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp)
                .clickable { onClickPlayPause() }
        )

        // Prev (Left)
        Text(
            text = "|◀",
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 18.dp)
                .clickable { onClickPrev() }
        )

        // Next (Right)
        Text(
            text = "▶|",
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 18.dp)
                .clickable { onClickNext() }
        )
    }
}
'''
with open(wheel_path, 'w', encoding='utf-8') as f:
    f.write(new_wheel_content)
