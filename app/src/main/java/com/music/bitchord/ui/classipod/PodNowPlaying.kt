package com.music.bitchord.ui.classipod

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.playback.PlayerState

@Composable
fun PodNowPlaying(playerState: PlayerState, menuState: PodMenuState) {
    val song = playerState.song
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent) // relies on screen background
    ) {
        if (song != null) {
            // "3 of 77" top left
            val queuePos = (playerState.queueIndex + 1).toString()
            val queueTotal = playerState.queue.size.coerceAtLeast(1).toString()
            Text(
                text = if (menuState.isScrubbingMode.value) "Scrubbing" else "$queuePos of $queueTotal",
                fontSize = 13.sp,
                color = Color(0xFF1E3A5F),
                fontWeight = if (menuState.isScrubbingMode.value) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.padding(start = 6.dp, top = 4.dp)
            )
            
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = song.title,
                    fontSize = 15.sp,
                    color = Color(0xFF1E3A5F),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = song.artist ?: "Unknown Artist",
                    fontSize = 14.sp,
                    color = Color(0xFF1E3A5F),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = song.albumName ?: "Unknown Album",
                    fontSize = 14.sp,
                    color = Color(0xFF1E3A5F),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Progress Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                val currentRenderPos = menuState.scrubPositionMs.value ?: playerState.position.positionMs
                
                // Pill progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(7.dp))
                        .background(Color.Transparent)
                ) {
                    val progress = if (playerState.durationMs > 0) {
                        currentRenderPos.toFloat() / playerState.durationMs.toFloat()
                    } else 0f
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(if (menuState.isScrubbingMode.value) Color(0xFF7A9BCF) else Color(0xFF1E3A5F))
                    )
                }
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatMs(currentRenderPos), fontSize = 12.sp, color = Color(0xFF1E3A5F))
                    val remaining = playerState.durationMs - currentRenderPos
                    Text(if (remaining > 0) "-" + formatMs(remaining) else "0:00", fontSize = 12.sp, color = Color(0xFF1E3A5F))
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Not Playing", fontSize = 16.sp, color = Color(0xFF1E3A5F))
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return String.format("%d:%02d", m, s)
}
