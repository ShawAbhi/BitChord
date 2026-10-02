package com.music.bitchord.ui.classipod

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.animateScrollBy
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
    
    // Keep the highlighted row fully on screen. Measured from what the list
    // has actually laid out rather than assuming a fixed number of rows per
    // page: the screen's height changes with the iPod's size, and a guessed
    // count let the highlight slip below the bottom edge.
    LaunchedEffect(selectedIndex) {
        val info = listState.layoutInfo
        val visible = info.visibleItemsInfo
        if (visible.isEmpty()) return@LaunchedEffect
        val viewportTop = info.viewportStartOffset
        val viewportBottom = info.viewportEndOffset - info.afterContentPadding
        val row = visible.firstOrNull { it.index == selectedIndex }
        when {
            // On screen but cut off at the bottom: nudge up just enough.
            row != null && row.offset + row.size > viewportBottom ->
                listState.animateScrollBy((row.offset + row.size - viewportBottom).toFloat())
            // On screen but cut off at the top: nudge down just enough.
            row != null && row.offset < viewportTop ->
                listState.animateScrollBy((row.offset - viewportTop).toFloat())
            row != null -> Unit
            // Off the top: bring it to the top.
            selectedIndex < visible.first().index -> listState.animateScrollToItem(selectedIndex)
            // Off the bottom: scroll so it lands on the bottom edge. Rows are
            // all one height, so the distance is rows × that height.
            else -> {
                val last = visible.last()
                val rowSize = last.size
                val overshoot = (last.offset + last.size - viewportBottom).coerceAtLeast(0)
                listState.animateScrollBy(
                    ((selectedIndex - last.index) * rowSize + overshoot).toFloat(),
                )
            }
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
