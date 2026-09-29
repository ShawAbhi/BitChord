import os

screen_path = 'app/src/main/java/com/music/bitchord/ui/classipod/PodScreen.kt'
new_screen = '''package com.music.bitchord.ui.classipod

import androidx.compose.foundation.background
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
        // Status Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFE0E0E0), Color(0xFFC0C0C0))
                    )
                )
                .padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = if (playerState.isPlaying) "▶" else "||", fontSize = 11.sp, color = Color.Black)
            Text(text = currentMenu.title.value, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = Color.Black)
            // Battery mock
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(16.dp, 8.dp).background(Color.Black).padding(1.dp)) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
                        Box(modifier = Modifier.fillMaxWidth(0.8f).fillMaxHeight().background(Color.Black))
                    }
                }
                Box(modifier = Modifier.size(2.dp, 4.dp).background(Color.Black))
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PodMenuList(menuState: PodMenuState) {
    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color.White)
        ) {
            menuState.items.forEachIndexed { index, item ->
                val isSelected = index == menuState.selectedIndex.value
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSelected) 
                                Brush.verticalGradient(listOf(Color(0xFF4A90E2), Color(0xFF007AFF))) 
                            else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                        )
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        color = if (isSelected) Color.White else Color.Black,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
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
        
        // Right side (split view for menus, commonly empty or showing half art)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color.White)
        ) {
            // Half-screen split for 5.5 Gen Classic
        }
    }
}
'''
with open(menu_list_path, 'w', encoding='utf-8') as f:
    f.write(new_menu_list)
