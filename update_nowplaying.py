import os

now_playing_path = 'app/src/main/java/com/music/bitchord/ui/classipod/PodNowPlaying.kt'
new_now_playing = '''package com.music.bitchord.ui.classipod

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.playback.PlayerState

@Composable
fun PodNowPlaying(playerState: PlayerState) {
    val song = playerState.song
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        if (song != null) {
            // Split view: Left art, right info (Classic 5.5 split look)
            Row(modifier = Modifier.weight(1f).fillMaxWidth().padding(8.dp)) {
                // Mock album art
                Box(modifier = Modifier.weight(1f).aspectRatio(1f).background(Color.DarkGray)) {
                    Text("ART", color = Color.White, modifier = Modifier.align(Alignment.Center))
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                    Text(
                        text = song.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = song.artist ?: "Unknown Artist",
                        fontSize = 12.sp,
                        color = Color.DarkGray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = song.albumName ?: "Unknown Album",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFE0E0E0))
                )
                
                val progress = if (playerState.durationMs > 0) {
                    playerState.position.positionMs.toFloat() / playerState.durationMs.toFloat()
                } else 0f
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(
                            Brush.verticalGradient(listOf(Color(0xFF88CCFF), Color(0xFF007AFF)))
                        )
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Not Playing", fontSize = 16.sp, color = Color.Gray)
            }
        }
    }
}
'''
with open(now_playing_path, 'w', encoding='utf-8') as f:
    f.write(new_now_playing)
