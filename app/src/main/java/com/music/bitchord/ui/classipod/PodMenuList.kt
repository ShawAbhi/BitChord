package com.music.bitchord.ui.classipod

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PodMenuList(menuState: PodMenuState) {
    val listState = rememberLazyListState()
    val selectedIndex = menuState.selectedIndex.value
    
    // Ensure the selected item is always visible by scrolling the list when necessary
    LaunchedEffect(selectedIndex) {
        val firstVisible = listState.firstVisibleItemIndex
        val itemsPerPage = 6 // Roughly 6 full items fit on this iPod screen height
        
        if (selectedIndex < firstVisible) {
            listState.scrollToItem(selectedIndex) // Push list down
        } else if (selectedIndex >= firstVisible + itemsPerPage) {
            listState.scrollToItem(selectedIndex - itemsPerPage + 1) // Push list up
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        itemsIndexed(menuState.items) { index, item ->
            val isSelected = index == selectedIndex
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
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (item.hasArrow) {
                    Text(
                        text = ">",
                        color = if (isSelected) Color.White else Color.Gray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}
