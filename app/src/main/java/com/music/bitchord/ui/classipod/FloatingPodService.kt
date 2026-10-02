package com.music.bitchord.ui.classipod

import android.app.Service
import android.content.Intent
import android.content.res.Resources
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
        val collapsedSizePx = (64 * Resources.getSystem().displayMetrics.density).roundToInt()
        val initialX = (screenWidth - collapsedSizePx) / 2

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            x = initialX
            y = -200
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
                    var isFirstLaunch by remember { mutableStateOf(true) }
                    
                    var expandedWidthDp by remember { mutableStateOf(320.dp) }
                    var expandedHeightDp by remember { mutableStateOf(620.dp) }
                    
                    val collapsedSizePx = with(density) { 64.dp.toPx() }
                    
                    val expansionProgress = remember { Animatable(if (isExpanded) 1f else 0f) }

                    var windowX by remember { mutableStateOf(params.x.toFloat()) }
                    var windowY by remember { mutableStateOf(params.y.toFloat()) }
                    
                    SideEffect {
                        this@FloatingPodService.toggleExpansion = { isExpanded = !isExpanded }
                        this@FloatingPodService.closeExpansion = { isExpanded = false }
                    }

                    var collapsedOffsetX by remember { mutableStateOf(0f) }
                    var collapsedOffsetY by remember { mutableStateOf(0f) }

                    LaunchedEffect(isExpanded) {
                        if (isFirstLaunch) {
                            isFirstLaunch = false
                            return@LaunchedEffect
                        }

                        val newWidthPx = with(density) { expandedWidthDp.toPx() }
                        val newHeightPx = with(density) { expandedHeightDp.toPx() }
                        
                        val maxExpandedX = (screenWidth - newWidthPx) / 2f
                        val maxExpandedY = (screenHeight - newHeightPx) / 2f
                        val maxCollapsedX = (screenWidth - collapsedSizePx) / 2f
                        val maxCollapsedY = (screenHeight - collapsedSizePx) / 2f

                        if (isExpanded) {
                            val startX = windowX
                            val startY = windowY

                            val targetX = windowX.coerceIn(-maxExpandedX, maxExpandedX)
                            val targetY = windowY.coerceIn(-maxExpandedY, maxExpandedY)

                            collapsedOffsetX = startX - targetX
                            collapsedOffsetY = startY - targetY

                            windowX = targetX
                            windowY = targetY
                            params.x = windowX.roundToInt()
                            params.y = windowY.roundToInt()
                            params.width = newWidthPx.roundToInt()
                            params.height = newHeightPx.roundToInt()
                            try {
                                windowManager.updateViewLayout(composeView, params)
                            } catch (e: Exception) {}

                            expansionProgress.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                            )
                            
                            collapsedOffsetX = 0f
                            collapsedOffsetY = 0f
                        } else {
                            // 1. In-place collapse animation (zero jump, zero race conditions)
                            collapsedOffsetX = 0f
                            collapsedOffsetY = 0f

                            expansionProgress.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                            )

                            // 2. Resize physical window to 64x64 at the exact current position
                            params.width = collapsedSizePx.roundToInt()
                            params.height = collapsedSizePx.roundToInt()
                            params.x = windowX.roundToInt()
                            params.y = windowY.roundToInt()
                            try {
                                windowManager.updateViewLayout(composeView, params)
                            } catch (e: Exception) {}

                            // 3. Smoothly slide the collapsed bubble to the nearest screen edge
                            val targetEdgeX = if (windowX < 0) -maxCollapsedX else maxCollapsedX
                            val targetEdgeY = windowY.coerceIn(-maxCollapsedY, maxCollapsedY)

                            if (windowX != targetEdgeX || windowY != targetEdgeY) {
                                val slideAnimX = Animatable(windowX)
                                val slideAnimY = Animatable(windowY)
                                
                                launch {
                                    slideAnimY.animateTo(
                                        targetValue = targetEdgeY,
                                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                                    ) {
                                        windowY = value
                                        params.y = windowY.roundToInt()
                                        try {
                                            windowManager.updateViewLayout(composeView, params)
                                        } catch (e: Exception) {}
                                    }
                                }
                                
                                slideAnimX.animateTo(
                                    targetValue = targetEdgeX,
                                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                                ) {
                                    windowX = value
                                    params.x = windowX.roundToInt()
                                    try {
                                        windowManager.updateViewLayout(composeView, params)
                                    } catch (e: Exception) {}
                                }
                            }
                        }
                    }

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
                            },
                            onDragEnd = {
                                if (!isExpanded) {
                                    val maxCollapsedX = (screenWidth - collapsedSizePx) / 2f
                                    val maxCollapsedY = (screenHeight - collapsedSizePx) / 2f
                                    val targetEdgeX = if (windowX < 0) -maxCollapsedX else maxCollapsedX
                                    val targetEdgeY = windowY.coerceIn(-maxCollapsedY, maxCollapsedY)
                                    
                                    scope.launch {
                                        val animX = Animatable(windowX)
                                        val animY = Animatable(windowY)
                                        launch {
                                            animY.animateTo(targetEdgeY, tween(200, easing = FastOutSlowInEasing)) {
                                                windowY = value
                                                params.y = windowY.roundToInt()
                                                try { windowManager.updateViewLayout(composeView, params) } catch (e: Exception) {}
                                            }
                                        }
                                        animX.animateTo(targetEdgeX, tween(200, easing = FastOutSlowInEasing)) {
                                            windowX = value
                                            params.x = windowX.roundToInt()
                                            try { windowManager.updateViewLayout(composeView, params) } catch (e: Exception) {}
                                        }
                                    }
                                }
                            }
                        )
                    }
                    
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

                    val isDark = isSystemInDarkTheme()
                    val containerColor = if (isDark) Color(0xFF202124) else Color.White
                    val borderColor = if (isDark) Color(0x33FFFFFF) else Color(0x1F000000)
                    val iconTint = if (isDark) Color.White else Color(0xFF1F1F1F)

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Expanded App UI container (rendered on GPU layer with zero recomposition during animation)
                        Box(
                            modifier = Modifier
                                .size(expandedWidthDp, expandedHeightDp)
                                .graphicsLayer {
                                    val p = expansionProgress.value
                                    alpha = ((p - 0.05f) / 0.95f).coerceIn(0f, 1f)
                                    val scale = 0.72f + 0.28f * p
                                    scaleX = scale
                                    scaleY = scale
                                    
                                    translationX = collapsedOffsetX * (1f - p)
                                    translationY = collapsedOffsetY * (1f - p)
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
                                        .clickable(enabled = isExpanded) { isExpanded = false },
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

                        // Collapsed Icon container (fades out gracefully, zero recomposition)
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .graphicsLayer {
                                    val p = expansionProgress.value
                                    alpha = (1f - p * 2.5f).coerceIn(0f, 1f)
                                    val scale = 1f + 0.15f * p
                                    scaleX = scale
                                    scaleY = scale
                                    
                                    translationX = collapsedOffsetX * (1f - p)
                                    translationY = collapsedOffsetY * (1f - p)
                                }
                                .then(if (!isExpanded) dragModifier else Modifier)
                                .shadow(elevation = 6.dp, shape = CircleShape, clip = false)
                                .background(containerColor, CircleShape)
                                .border(1.dp, borderColor, CircleShape)
                                .clickable(enabled = !isExpanded) { isExpanded = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ipod_icon_white),
                                contentDescription = "Pod",
                                modifier = Modifier.size(34.dp),
                                tint = iconTint
                            )
                        }
                    }
                }
            }
        }

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
            try {
                windowManager.removeView(composeView)
            } catch (e: Exception) {}
        }
    }
}
