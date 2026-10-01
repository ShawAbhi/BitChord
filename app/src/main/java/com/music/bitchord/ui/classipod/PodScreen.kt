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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.session.MediaController
import com.music.bitchord.playback.PlayerState

@Composable
fun PodScreen(
    controller: MediaController?,
    playerState: PlayerState,
    currentMenu: PodMenuState
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFD3E3F0)) // Base LCD blue
            .drawWithCache {
                onDrawWithContent {
                    drawContent()
                    // LCD Horizontal Scanlines
                    val spacing = 4f // Pixels, not DP, for a high-density grid
                    val numLines = (size.height / spacing).toInt()
                    for (i in 0..numLines) {
                        drawLine(
                            color = Color(0x11000000),
                            start = Offset(0f, i * spacing),
                            end = Offset(size.width, i * spacing),
                            strokeWidth = 1.5f
                        )
                    }
                    // Inner bezel shadow
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.Transparent, Color(0x15000000)),
                            radius = size.width * 0.8f
                        )
                    )
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Status Bar (Light metallic blue)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFFD6E4F0), Color(0xFF90B9DF))
                        )
                    )
                    .border(width = 1.dp, color = Color(0xFF1E3A5F))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                // Play/Pause icon (Left)
                Text(
                    text = if (playerState.isPlaying) "▶" else "||", 
                    fontSize = 11.sp, 
                    color = Color(0xFF1E3A5F), 
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                
                // Title (Center)
                val displayTitle = if (currentMenu.isNowPlaying.value) "Now Playing" else currentMenu.title.value
                Text(
                    text = displayTitle, 
                    fontWeight = FontWeight.ExtraBold, 
                    fontSize = 14.sp, 
                    color = Color(0xFF1E3A5F),
                    modifier = Modifier.align(Alignment.Center)
                )
                
                // Battery icon (Right)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Box(modifier = Modifier.size(18.dp, 10.dp).border(1.dp, Color(0xFF1E3A5F)).padding(1.dp)) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
                            Box(modifier = Modifier.fillMaxWidth(0.8f).fillMaxHeight().background(Color(0xFF1E3A5F)))
                        }
                    }
                    Box(modifier = Modifier.size(2.dp, 4.dp).background(Color(0xFF1E3A5F)))
                }
            }
            
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (currentMenu.isNowPlaying.value) {
                    PodNowPlaying(playerState, currentMenu)
                } else {
                    PodMenuList(currentMenu)
                }
            }
        }
    }
}
