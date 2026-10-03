package com.music.bitchord.ui.classipod

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.components.GLASS_EDGE_COLOR
import com.music.bitchord.ui.components.GLASS_EDGE_WIDTH
import com.music.bitchord.ui.components.glassContentColor
import com.music.bitchord.ui.components.lightweightLiquidGlass
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi

/**
 * The round iPod button beside the mini player / glass nav bar that opens the
 * Classipod PIP window.
 *
 * It slides out while the window is open ([FloatingPodService.isRunning]) and
 * back in once the window is closed, so it reads as turning into the bubble.
 *
 * The surface matches whatever bar it sits beside:
 *  - **Liquid glass** ([glass] true): [liquidGlass], the same material as
 *    [GlassNavBar][com.music.bitchord.ui.components.GlassNavBar]'s pills.
 *  - **Reduce dynamic blur**: a solid `surface` fill.
 *  - **Otherwise**: Haze, as [MiniPlayer][com.music.bitchord.ui.components.MiniPlayer] uses.
 *
 * Launching goes through [FloatingPodService.start], which asks for the overlay
 * permission first when it is missing.
 *
 * @param glass whether liquid glass is active and supported — MainActivity's
 *   `glassActive`.
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun RowScope.PodLauncherButton(
    glass: Boolean,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val isPodOpen by FloatingPodService.isRunning
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()

    AnimatedVisibility(
        visible = !isPodOpen,
        modifier = modifier,
        enter = expandHorizontally(expandFrom = Alignment.End) + fadeIn(),
        exit = shrinkHorizontally(shrinkTowards = Alignment.End) + fadeOut(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val surfaceColor = MaterialTheme.colorScheme.surface
            // Performance: no live blur of its own. This button sits beside the
            // bar for as long as the app is open, and as a full liquidGlass
            // surface it added another backdrop capture, blur, lens and shadow
            // pass every frame anything moved (as a Haze surface, another blur
            // pass) — for a 56dp circle. The lightweight glass keeps the tint,
            // highlight and hairline that make it read as glass beside the
            // nav bar; elsewhere it is a near-opaque surface.
            val material = when {
                reduceDynamicBlur -> Modifier
                    .background(surfaceColor, CircleShape)
                    .border(GLASS_EDGE_WIDTH, GLASS_EDGE_COLOR, CircleShape)
                // Under the lightweight glass's own 40% tint, a further fill:
                // with no blur behind it, raw content showing through at 60%
                // would make the glyph hard to read.
                glass -> Modifier
                    .background(surfaceColor.copy(alpha = 0.6f), CircleShape)
                    .lightweightLiquidGlass(CircleShape, fallbackColor = surfaceColor)
                else -> Modifier
                    .background(surfaceColor.copy(alpha = 0.94f), CircleShape)
                    .border(GLASS_EDGE_WIDTH, GLASS_EDGE_COLOR, CircleShape)
            }
            Box(
                modifier = Modifier
                    .size(LAUNCHER_SIZE)
                    .clip(CircleShape)
                    .then(material)
                    .clickable {
                        haptics.play(Haptic.Select)
                        FloatingPodService.start(context)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ipod_icon_white),
                    contentDescription = "Open iPod",
                    modifier = Modifier.size(22.dp),
                    // Glass shows whatever is behind it, so it takes the glass
                    // content colour like the nav bar's glyphs do.
                    tint = if (glass && !reduceDynamicBlur) {
                        glassContentColor()
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
            Spacer(Modifier.width(16.dp))
        }
    }
}

private val LAUNCHER_SIZE = 56.dp
