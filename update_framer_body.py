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
            MenuItem("Now Playing", hasArrow = false) {
                menuState.isNowPlaying.value = true
            },
            MenuItem("Play / Pause", hasArrow = false) {
                if (playerState.isPlaying) controller?.pause() else controller?.play()
            },
            MenuItem("Exit", hasArrow = false) {
                (context as? Activity)?.finish()
            }
        ))
    }

    // Outer Body (Pure white minimalist style)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .shadow(16.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFFAFAFA)) // Very light, almost pure white
            .border(1.dp, Color(0xFFE5E5E5), RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // The Screen (No thick black bezel, just a thin grey outline)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.42f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFFFFFF))
                    .border(1.dp, Color(0xFFC0C0C0), RoundedCornerShape(8.dp)) // Thin silver border
            ) {
                PodScreen(controller, playerState, menuState)
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
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
    }
}
'''
with open(app_path, 'w', encoding='utf-8') as f:
    f.write(new_app_content)
