package com.music.bitchord.ui.classipod

import android.app.Activity
import android.media.AudioManager
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

    // Ensure we start with Playlist and update immediately
    LaunchedEffect(playerState.queue) {
        menuState.title.value = "Playlist"
        
        val queueItems = playerState.queue.mapIndexed { index, song ->
            MenuItem(song.title.take(30) + if(song.title.length > 30) "..." else "", hasArrow = false) {
                controller?.seekToDefaultPosition(index)
                controller?.play()
                menuState.isNowPlaying.value = true
            }
        }
        val newItems = queueItems.ifEmpty { listOf(MenuItem("Empty", false) {}) }
        
        // We are no longer using nested menus, so just set the items directly
        menuState.items.clear()
        menuState.items.addAll(newItems)
        
        // Keep selection within bounds
        menuState.selectedIndex.value = menuState.selectedIndex.value.coerceIn(0, (newItems.size - 1).coerceAtLeast(0))
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
                    onScroll = { ticks -> 
                        if (menuState.isNowPlaying.value) {
                            val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager
                            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                            val newVol = (currentVol + ticks).coerceIn(0, maxVol)
                            if (currentVol != newVol) {
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                true
                            } else {
                                false
                            }
                        } else {
                            menuState.scroll(ticks)
                        }
                    },
                    onClickMenu = {
                        // Toggle between Now Playing and Playlist
                        menuState.isNowPlaying.value = !menuState.isNowPlaying.value
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
