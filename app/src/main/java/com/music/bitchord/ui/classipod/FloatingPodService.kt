package com.music.bitchord.ui.classipod

import android.app.Service
import android.content.Intent
import android.content.res.Resources
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.music.bitchord.R
import com.music.bitchord.playback.rememberMediaController
import com.music.bitchord.playback.rememberPlayerState
import com.music.bitchord.ui.theme.BitChordTheme
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

class FloatingPodService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    companion object {
        val isRunning = androidx.compose.runtime.mutableStateOf(false)
    }

    private lateinit var windowManager: WindowManager
    private lateinit var composeView: ComposeView
    private lateinit var params: WindowManager.LayoutParams
    
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override fun onBind(intent: Intent?): IBinder? = null

    private var toggleExpansion: (() -> Unit)? = null
    private var closeExpansion: (() -> Unit)? = null

    override fun onCreate() {
        super.onCreate()
        isRunning.value = true
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        val displayMetrics = Resources.getSystem().displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val densityVal = displayMetrics.density

        val initialCollapsedPx = (64 * densityVal).roundToInt()
        // Initial position: docked to right edge
        val initialX = ((screenWidth - initialCollapsedPx) / 2f).roundToInt()
        val initialY = -((screenHeight / 4f)).roundToInt()

        params = WindowManager.LayoutParams(
            initialCollapsedPx,
            initialCollapsedPx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            x = initialX
            y = initialY
        }

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingPodService)
            setViewTreeViewModelStoreOwner(this@FloatingPodService)
            setViewTreeSavedStateRegistryOwner(this@FloatingPodService)
            
            setContent {
                BitChordTheme {
                    val controller = rememberMediaController()
                    val playerState = rememberPlayerState(controller)
                    val density = LocalDensity.current
                    val scope = rememberCoroutineScope()

                    var isExpanded by remember { mutableStateOf(false) }
                    var isFullyCollapsed by remember { mutableStateOf(true) }
                    
                    var expandedWidthDp by remember { mutableStateOf(320.dp) }
                    var expandedHeightDp by remember { mutableStateOf(620.dp) }
                    
                    val collapsedSizePx = with(density) { 64.dp.toPx() }
                    
                    val expansionProgress = remember { Animatable(0f) }

                    // Track center coordinate offsets (relative to screen center, since gravity is CENTER)
                    var windowX by remember { mutableStateOf(initialX.toFloat()) }
                    var windowY by remember { mutableStateOf(initialY.toFloat()) }
                    
                    // Offsets for anchoring the animation origin to the icon position
                    var animShiftX by remember { mutableStateOf(0f) }
                    var animShiftY by remember { mutableStateOf(0f) }

                    // Expose external toggles
                    SideEffect {
                        this@FloatingPodService.toggleExpansion = { isExpanded = !isExpanded }
                        this@FloatingPodService.closeExpansion = { isExpanded = false }
                    }

                    // Fluid easing curve for silky smooth, responsive motion
                    val fluidEasing = remember { CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f) }

                    LaunchedEffect(isExpanded) {
                        val currentExpandedWidthPx = with(density) { expandedWidthDp.toPx() }
                        val currentExpandedHeightPx = with(density) { expandedHeightDp.toPx() }

                        if (isExpanded) {
                            isFullyCollapsed = false

                            // 1. Calculate target center for the expanded window so it fits within screen margins
                            val maxTargetX = (screenWidth - currentExpandedWidthPx) / 2f - with(density) { 16.dp.toPx() }
                            val minTargetX = -(screenWidth - currentExpandedWidthPx) / 2f + with(density) { 16.dp.toPx() }
                            val targetWindowX = windowX.coerceIn(minTargetX, maxTargetX)

                            val maxTargetY = (screenHeight - currentExpandedHeightPx) / 2f - with(density) { 32.dp.toPx() }
                            val minTargetY = -(screenHeight - currentExpandedHeightPx) / 2f + with(density) { 32.dp.toPx() }
                            val targetWindowY = windowY.coerceIn(minTargetY, maxTargetY)

                            // 2. The relative shift from current icon center to target expanded window center
                            animShiftX = windowX - targetWindowX
                            animShiftY = windowY - targetWindowY

                            // 3. Immediately size the native window to the expanded dimensions
                            windowX = targetWindowX
                            windowY = targetWindowY
                            params.x = targetWindowX.roundToInt()
                            params.y = targetWindowY.roundToInt()
                            params.width = currentExpandedWidthPx.roundToInt()
                            params.height = currentExpandedHeightPx.roundToInt()
                            try {
                                windowManager.updateViewLayout(composeView, params)
                            } catch (e: Exception) {}

                            // 4. Run GPU animation from 0f -> 1f (no WindowManager layout updates during animation!)
                            expansionProgress.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(durationMillis = 280, easing = fluidEasing)
                            )
                        } else {
                            // Only animate collapse if we aren't already fully collapsed
                            if (expansionProgress.value > 0f) {
                                // 1. Calculate where the collapsed icon should land (snap to nearest screen edge)
                                val snapMargin = with(density) { 8.dp.toPx() }
                                val targetCollapsedX = if (windowX < 0) {
                                    -(screenWidth - collapsedSizePx) / 2f + snapMargin
                                } else {
                                    (screenWidth - collapsedSizePx) / 2f - snapMargin
                                }
                                val maxCollapsedY = (screenHeight - collapsedSizePx) / 2f - with(density) { 64.dp.toPx() }
                                val minCollapsedY = -(screenHeight - collapsedSizePx) / 2f + with(density) { 64.dp.toPx() }
                                val targetCollapsedY = windowY.coerceIn(minCollapsedY, maxCollapsedY)

                                // 2. Relative shift towards the target icon center
                                animShiftX = targetCollapsedX - windowX
                                animShiftY = targetCollapsedY - windowY

                                // 3. Run GPU animation 1f -> 0f (native window stays expanded until animation is done)
                                expansionProgress.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(durationMillis = 250, easing = fluidEasing)
                                )

                                // 4. Once visual animation is complete, snap the native window bounds to the icon
                                windowX = targetCollapsedX
                                windowY = targetCollapsedY
                                params.x = targetCollapsedX.roundToInt()
                                params.y = targetCollapsedY.roundToInt()
                                params.width = collapsedSizePx.roundToInt()
                                params.height = collapsedSizePx.roundToInt()
                                
                                animShiftX = 0f
                                animShiftY = 0f
                                isFullyCollapsed = true

                                try {
                                    windowManager.updateViewLayout(composeView, params)
                                } catch (e: Exception) {}
                            }
                        }
                    }

                    // Dragging updates window position smoothly
                    val dragModifier = Modifier.pointerInput(isExpanded) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                windowX += dragAmount.x
                                windowY += dragAmount.y
                                params.x = windowX.roundToInt()
                                params.y = windowY.roundToInt()
                                try {
                                    windowManager.updateViewLayout(composeView, params)
                                } catch (e: Exception) {}
                            },
                            onDragEnd = {
                                if (!isExpanded) {
                                    // Snap collapsed icon to left or right edge
                                    val snapMargin = with(density) { 8.dp.toPx() }
                                    val targetX = if (windowX < 0) {
                                        -(screenWidth - collapsedSizePx) / 2f + snapMargin
                                    } else {
                                        (screenWidth - collapsedSizePx) / 2f - snapMargin
                                    }
                                    windowX = targetX
                                    params.x = targetX.roundToInt()
                                    try {
                                        windowManager.updateViewLayout(composeView, params)
                                    } catch (e: Exception) {}
                                } else {
                                    // Ensure expanded window stays within screen bounds
                                    val currentWidthPx = with(density) { expandedWidthDp.toPx() }
                                    val currentHeightPx = with(density) { expandedHeightDp.toPx() }
                                    val maxTargetX = (screenWidth - currentWidthPx) / 2f
                                    val minTargetX = -(screenWidth - currentWidthPx) / 2f
                                    val maxTargetY = (screenHeight - currentHeightPx) / 2f
                                    val minTargetY = -(screenHeight - currentHeightPx) / 2f
                                    
                                    val clampedX = windowX.coerceIn(minTargetX, maxTargetX)
                                    val clampedY = windowY.coerceIn(minTargetY, maxTargetY)
                                    if (clampedX != windowX || clampedY != windowY) {
                                        windowX = clampedX
                                        windowY = clampedY
                                        params.x = clampedX.roundToInt()
                                        params.y = clampedY.roundToInt()
                                        try {
                                            windowManager.updateViewLayout(composeView, params)
                                        } catch (e: Exception) {}
                                    }
                                }
                            }
                        )
                    }
                    
                    // Resizing updates both size and position
                    val resizeModifier = Modifier.pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val dragXDp = with(density) { dragAmount.x.toDp() }
                                val dragYDp = with(density) { dragAmount.y.toDp() }
                                
                                val oldWidthPx = with(density) { expandedWidthDp.toPx() }
                                val oldHeightPx = with(density) { expandedHeightDp.toPx() }
                                
                                expandedWidthDp = (expandedWidthDp + dragXDp).coerceIn(240.dp, 500.dp)
                                expandedHeightDp = (expandedHeightDp + dragYDp).coerceIn(460.dp, 850.dp)
                                
                                val newWidthPx = with(density) { expandedWidthDp.toPx() }
                                val newHeightPx = with(density) { expandedHeightDp.toPx() }
                                
                                val diffX = newWidthPx - oldWidthPx
                                val diffY = newHeightPx - oldHeightPx
                                
                                windowX += diffX / 2f
                                windowY += diffY / 2f
                                
                                params.x = windowX.roundToInt()
                                params.y = windowY.roundToInt()
                                params.width = newWidthPx.roundToInt()
                                params.height = newHeightPx.roundToInt()
                                
                                try {
                                    windowManager.updateViewLayout(composeView, params)
                                } catch (e: Exception) {}
                            }
                        )
                    }

                    if (isFullyCollapsed) {
                        // Stationary, resting collapsed icon state (lightweight, zero overhead)
                        val isDark = isSystemInDarkTheme()
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .then(dragModifier)
                                .shadow(elevation = 6.dp, shape = CircleShape, clip = false)
                                .background(if (isDark) Color(0xFF202124) else Color.White, CircleShape)
                                .border(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0x1F000000), CircleShape)
                                .clickable { isExpanded = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ipod_icon_white),
                                contentDescription = "Pod",
                                modifier = Modifier.size(34.dp),
                                tint = if (isDark) Color.White else Color(0xFF1F1F1F)
                            )
                        }
                    } else {
                        // Active expanded / transitioning window container
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            val p = expansionProgress.value
                            val currentShiftX = animShiftX * (1f - p)
                            val currentShiftY = animShiftY * (1f - p)

                            // Expanded iPod UI Layer
                            Box(
                                modifier = Modifier
                                    .size(expandedWidthDp, expandedHeightDp)
                                    .graphicsLayer {
                                        val minScale = (64.dp.toPx() / expandedWidthDp.toPx()).coerceAtLeast(0.15f)
                                        val scale = minScale + (1f - minScale) * p
                                        scaleX = scale
                                        scaleY = scale
                                        alpha = (p * 1.6f - 0.1f).coerceIn(0f, 1f)
                                        translationX = currentShiftX
                                        translationY = currentShiftY
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(start = 24.dp, top = 32.dp, end = 24.dp, bottom = 56.dp)
                                ) {
                                    Box(modifier = dragModifier.fillMaxSize()) {
                                        ClassipodApp(controller, playerState)
                                    }
                                    
                                    // Floating Close Button
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(30.dp)
                                            .background(Color(0x99000000), CircleShape)
                                            .clickable { isExpanded = false },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("X", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    
                                    // Bottom-right Resize Handle
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .offset(x = 12.dp, y = 36.dp)
                                            .size(48.dp)
                                            .then(resizeModifier),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        androidx.compose.foundation.Canvas(modifier = Modifier.size(16.dp)) {
                                            drawLine(Color.Gray, start = Offset(8f, 16f), end = Offset(16f, 8f), strokeWidth = 4f)
                                            drawLine(Color.Gray, start = Offset(0f, 16f), end = Offset(16f, 0f), strokeWidth = 4f)
                                        }
                                    }
                                }
                            }

                            // Collapsed Icon Morphing Layer (fades out as iPod expands)
                            if (p < 0.95f) {
                                val isDark = isSystemInDarkTheme()
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .graphicsLayer {
                                            val iconScale = 1f + 0.2f * p
                                            scaleX = iconScale
                                            scaleY = iconScale
                                            alpha = (1f - p * 2.2f).coerceIn(0f, 1f)
                                            translationX = currentShiftX
                                            translationY = currentShiftY
                                        }
                                        .shadow(elevation = 6.dp, shape = CircleShape, clip = false)
                                        .background(if (isDark) Color(0xFF202124) else Color.White, CircleShape)
                                        .border(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0x1F000000), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ipod_icon_white),
                                        contentDescription = "Pod",
                                        modifier = Modifier.size(34.dp),
                                        tint = if (isDark) Color.White else Color(0xFF1F1F1F)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        windowManager.addView(composeView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning.value = false
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        if (::composeView.isInitialized) {
            try {
                windowManager.removeView(composeView)
            } catch (e: Exception) {}
        }
    }
}
