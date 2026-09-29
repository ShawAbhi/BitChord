package com.music.bitchord.ui.classipod

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf

data class MenuItem(
    val title: String,
    val hasArrow: Boolean = true,
    val onClick: () -> Unit
)

class PodMenuState {
    var isNowPlaying = mutableStateOf(false)
    var title = mutableStateOf("BitChord")
    var items = mutableStateListOf<MenuItem>()
    var selectedIndex = mutableStateOf(0)
    
    val history = mutableListOf<MenuSnapshot>()

    fun pushMenu(newTitle: String, newItems: List<MenuItem>) {
        if (items.isNotEmpty()) {
            history.add(MenuSnapshot(title.value, items.toList(), selectedIndex.value))
        }
        title.value = newTitle
        items.clear()
        items.addAll(newItems)
        selectedIndex.value = 0
    }

    fun popMenu(): Boolean {
        if (history.isNotEmpty()) {
            val snapshot = history.removeLast()
            title.value = snapshot.title
            items.clear()
            items.addAll(snapshot.items)
            selectedIndex.value = snapshot.selectedIndex
            return true
        }
        return false
    }

    fun scroll(ticks: Int) {
        if (items.isEmpty()) return
        val newIndex = selectedIndex.value + ticks
        selectedIndex.value = newIndex.coerceIn(0, items.size - 1)
    }
}

data class MenuSnapshot(
    val title: String,
    val items: List<MenuItem>,
    val selectedIndex: Int
)
