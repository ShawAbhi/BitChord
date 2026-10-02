package com.music.bitchord.ui.classipod

import android.app.Service
import android.content.Intent
import android.content.res.Resources
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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

    override fun onCreate() {
        super.onCreate()
        isRunning.value = true
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        val screenWidth = Resources.getSystem().displayMetrics.widthPixels
        val screenHeight = Resources.getSystem().displayMetrics.heightPixels

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            x = screenWidth / 2 - 200 // Offset slightly
            y = -200 // Slightly above center
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
                    
                    var isExpanded by remember { mutableStateOf(false) }
                    
                    var expandedWidthDp by remember { mutableStateOf(320.dp) }
                    var expandedHeightDp by remember { mutableStateOf(620.dp) }
                    
                    val collapsedSizePx = with(density) { 64.dp.toPx() }
                    
                    val expansionProgress = remember { Animatable(if (isExpanded) 1f else 0f) }

                    // Track absolute center coordinate offsets (since gravity is CENTER)
                    var windowX by remember { mutableStateOf(params.x.toFloat()) }
                    var windowY by remember { mutableStateOf(params.y.toFloat()) }
                    
                    // Allow external toggles
                    SideEffect {
                        this@FloatingPodService.toggleExpansion = { isExpanded = !isExpanded }
                        this@FloatingPodService.closeExpansion = { isExpanded = false }
                    }
                    
                    val scope = rememberCoroutineScope()

                    LaunchedEffect(isExpanded) {
                        if (isExpanded) {
                            // 1. Immediately expand the invisible WindowManager bounds to hold the animation
                            params.width = with(density) { expandedWidthDp.toPx() }.roundToInt()
                            params.height = with(density) { expandedHeightDp.toPx() }.roundToInt()
                            try {
                                windowManager.updateViewLayout(composeView, params)
                            } catch (e: Exception) {}
                            
                            // 2. Run the smooth Compose visual animation
                            expansionProgress.animateTo(
                                targetValue = 1f,
                                animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
                            )
                        } else {
                            // 1. Run the smooth Compose visual animation first
                            expansionProgress.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
                            )
                            
                            // 2. Once animation reaches 0, snap the WindowManager bounds down to small size
                            params.width = collapsedSizePx.roundToInt()
                            params.height = collapsedSizePx.roundToInt()
                            
                            // Optional: snap to edge
                            val screenHalfWidth = screenWidth / 2f
                            val absoluteX = screenHalfWidth + windowX
                            if (absoluteX < screenHalfWidth) {
                                windowX = -screenHalfWidth + collapsedSizePx / 2f
                            } else {
                                windowX = screenHalfWidth - collapsedSizePx / 2f
                            }
                            params.x = windowX.roundToInt()
                            
                            try {
                                windowManager.updateViewLayout(composeView, params)
                            } catch (e: Exception) {}
                        }
                    }

                    // Dragging updates window position synchronously for zero lag
                    val dragModifier = Modifier.pointerInput(Unit) {
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
                            }
                        )
                    }
                    
                    // Resizing updates both size and position synchronously
                    val resizeModifier = Modifier.pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val dragXDp = with(density) { dragAmount.x.toDp() }
                                val dragYDp = with(density) { dragAmount.y.toDp() }
                                
                                val oldWidthPx = with(density) { expandedWidthDp.toPx() }
                                val oldHeightPx = with(density) { expandedHeightDp.toPx() }
                                
                                expandedWidthDp = (expandedWidthDp + dragXDp).coerceIn(200.dp, 600.dp)
                                expandedHeightDp = (expandedHeightDp + dragYDp).coerceIn(400.dp, 1000.dp)
                                
                                val newWidthPx = with(density) { expandedWidthDp.toPx() }
                                val newHeightPx = with(density) { expandedHeightDp.toPx() }
                                
                                // Since window is centered, shift center to keep top-left anchored
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

                    // Root container fits the active WindowManager bounds
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Expanded App UI container (fixed size to avoid expensive relayouts during animation)
                        Box(
                            modifier = Modifier
                                .size(expandedWidthDp, expandedHeightDp)
                                .graphicsLayer {
                                    val currentWidthPx = 64.dp.toPx() + (expandedWidthDp.toPx() - 64.dp.toPx()) * expansionProgress.value
                                    val currentHeightPx = 64.dp.toPx() + (expandedHeightDp.toPx() - 64.dp.toPx()) * expansionProgress.value
                                    
                                    scaleX = currentWidthPx / expandedWidthDp.toPx()
                                    scaleY = currentHeightPx / expandedHeightDp.toPx()
                                    alpha = expansionProgress.value
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

                        // Collapsed Icon container
                        if (expansionProgress.value < 1f) {
                            val isDark = isSystemInDarkTheme()
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .graphicsLayer {
                                        alpha = 1f - expansionProgress.value
                                        val scale = 1f + 0.5f * expansionProgress.value
                                        scaleX = scale
                                        scaleY = scale
                                    }
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
                        }
                    }
                }
            }
        }

        // Initially we start collapsed, so configure WindowManager for small size
        params.width = (64 * Resources.getSystem().displayMetrics.density).roundToInt()
        params.height = (64 * Resources.getSystem().displayMetrics.density).roundToInt()
        
        windowManager.addView(composeView, params)
    }

    private var toggleExpansion: (() -> Unit)? = null
    private var closeExpansion: (() -> Unit)? = null

    override fun onDestroy() {
        super.onDestroy()
        isRunning.value = false
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        if (::composeView.isInitialized) {
            windowManager.removeView(composeView)
        }
    }
}
