import os

now_playing_path = 'app/src/main/java/com/music/bitchord/ui/classipod/PodNowPlaying.kt'
new_now_playing = '''package com.music.bitchord.ui.classipod

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
            .background(Color(0xFFF3F3F3)), // LCD Off-white
    ) {
        if (song != null) {
            // Split view: Left art, right info (Classic 5.5 split look)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Mock album art with a subtle 3D border
                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .aspectRatio(1f)
                        .shadow(4.dp)
                        .border(1.dp, Color(0xFFDDDDDD))
                        .background(Color.DarkGray)
                ) {
                    // Ideally AsyncImage here, but using placeholder
                    Text("ART", color = Color.White, modifier = Modifier.align(Alignment.Center))
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(), 
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = song.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = song.artist ?: "Unknown Artist",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF444444),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = song.albumName ?: "Unknown Album",
                        fontSize = 13.sp,
                        color = Color(0xFF666666),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Progress Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Current time placeholder
                Text("0:00", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(14.dp)
                        .border(1.dp, Color(0xFF999999))
                        .background(Color(0xFFFFFFFF))
                ) {
                    val progress = if (playerState.durationMs > 0) {
                        playerState.position.positionMs.toFloat() / playerState.durationMs.toFloat()
                    } else 0f
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF8AC7FF), Color(0xFF4A90E2))
                                )
                            )
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Duration placeholder
                Text("-0:00", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Not Playing", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            }
        }
    }
}
'''
with open(now_playing_path, 'w', encoding='utf-8') as f:
    f.write(new_now_playing)
