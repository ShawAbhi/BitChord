import os

screen_path = 'app/src/main/java/com/music/bitchord/ui/classipod/PodScreen.kt'
new_screen = '''package com.music.bitchord.ui.classipod

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // Status Bar (Light metallic blue)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFD0DFEE), Color(0xFF95BCE2))
                    )
                )
                .border(width = 0.5.dp, color = Color(0xFF86A5C3))
                .padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = if (playerState.isPlaying) "▶" else "||", fontSize = 11.sp, color = Color(0xFF1E3A5F), fontWeight = FontWeight.Bold)
            Text(text = currentMenu.title.value, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E3A5F))
            
            // Battery icon
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(18.dp, 10.dp).border(1.dp, Color(0xFF1E3A5F)).padding(1.dp)) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
                        Box(modifier = Modifier.fillMaxWidth(0.8f).fillMaxHeight().background(Color(0xFF1E3A5F)))
                    }
                }
                Box(modifier = Modifier.size(2.dp, 4.dp).background(Color(0xFF1E3A5F)))
            }
        }
        
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (currentMenu.isNowPlaying.value) {
                PodNowPlaying(playerState)
            } else {
                PodMenuList(currentMenu)
            }
        }
    }
}
'''
with open(screen_path, 'w', encoding='utf-8') as f:
    f.write(new_screen)

menu_list_path = 'app/src/main/java/com/music/bitchord/ui/classipod/PodMenuList.kt'
new_menu_list = '''package com.music.bitchord.ui.classipod

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PodMenuList(menuState: PodMenuState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        menuState.items.forEachIndexed { index, item ->
            val isSelected = index == menuState.selectedIndex.value
            val bgColor = if (isSelected) {
                Color(0xFF1D548B) // Dark blue selection
            } else if (index % 2 == 1) {
                Color(0xFFE4F0F5) // Alternating light blue stripe
            } else {
                Color.White
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgColor)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title,
                    color = if (isSelected) Color.White else Color(0xFF112233),
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.hasArrow) {
                    Text(
                        text = ">",
                        color = if (isSelected) Color.White else Color.Gray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
'''
with open(menu_list_path, 'w', encoding='utf-8') as f:
    f.write(new_menu_list)
