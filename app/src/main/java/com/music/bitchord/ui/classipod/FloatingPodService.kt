package com.music.bitchord.ui.classipod

import android.animation.ValueAnimator
import android.app.Service
import android.content.Intent
import android.content.res.Resources
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
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

    private var isExpandedState = false
    private val screenWidth = Resources.getSystem().displayMetrics.widthPixels
    private val screenHeight = Resources.getSystem().displayMetrics.heightPixels
    private val collapsedSizePx = (64 * Resources.getSystem().displayMetrics.density).roundToInt()
    private var toggleExpansion: (() -> Unit)? = null
    private var closeExpansion: (() -> Unit)? = null

    private var windowAnimatorX: ValueAnimator? = null
    private var windowAnimatorY: ValueAnimator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning.value = true
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenWidth - collapsedSizePx / 2 // Start half-buried on right edge
            y = 200
        }

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingPodService)
            setViewTreeViewModelStoreOwner(this@FloatingPodService)
            setViewTreeSavedStateRegistryOwner(this@FloatingPodService)
            
            setContent {
                BitChordTheme {
                    val controller = rememberMediaController()
                    val playerState = rememberPlayerState(controller)
                    var isExpanded by remember { mutableStateOf(false) }
                    
                    var savedCollapsedX by remember { mutableStateOf<Int?>(null) }
                    var savedCollapsedY by remember { mutableStateOf<Int?>(null) }
                    var wasDraggedWhileExpanded by remember { mutableStateOf(false) }

                    SideEffect {
                        this@FloatingPodService.toggleExpansion = { isExpanded = !isExpanded }
                        this@FloatingPodService.closeExpansion = { isExpanded = false }
                    }

                    LaunchedEffect(isExpanded) {
                        if (isExpandedState != isExpanded) {
                            isExpandedState = isExpanded
                            if (isExpanded) {
                                savedCollapsedX = params.x
                                savedCollapsedY = params.y
                                wasDraggedWhileExpanded = false
                                ensureFullyVisible()
                            } else {
                                if (!wasDraggedWhileExpanded && savedCollapsedX != null) {
                                    animateWindowTo(savedCollapsedX!!, savedCollapsedY)
                                } else {
                                    handleCollapseRelease()
                                }
                            }
                        }
                    }

                    val dragModifier = Modifier.pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = {
                                windowAnimatorX?.cancel()
                                windowAnimatorY?.cancel()
                            },
                            onDrag = { change: PointerInputChange, dragAmount: Offset ->
                                change.consume()
                                params.x += dragAmount.x.roundToInt()
                                params.y += dragAmount.y.roundToInt()
                                
                                if (!isExpandedState) {
                                    if (params.x < -collapsedSizePx / 2) params.x = -collapsedSizePx / 2
                                    if (params.x > screenWidth - collapsedSizePx / 2) params.x = screenWidth - collapsedSizePx / 2
                                } else {
                                    wasDraggedWhileExpanded = true
                                }
                                
                                try {
                                    windowManager.updateViewLayout(composeView, params)
                                } catch (e: Exception) {}
                            },
                            onDragEnd = {
                                if (!isExpandedState) {
                                    handleCollapseRelease()
                                } else {
                                    ensureFullyVisible()
                                }
                            }
                        )
                    }

                    val isDark = isSystemInDarkTheme()
                    val containerColor = if (isDark) Color(0xFF202124) else Color.White
                    val borderColor = if (isDark) Color(0x33FFFFFF) else Color(0x1F000000)

                    Box {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = !isExpanded,
                            enter = fadeIn(tween(180, delayMillis = 40, easing = FastOutSlowInEasing)) + 
                                    scaleIn(initialScale = 0.85f, animationSpec = tween(180, delayMillis = 40, easing = FastOutSlowInEasing)),
                            exit = fadeOut(tween(140, easing = FastOutSlowInEasing)) + 
                                   scaleOut(targetScale = 0.85f, animationSpec = tween(140, easing = FastOutSlowInEasing))
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .then(dragModifier)
                                    .shadow(elevation = 6.dp, shape = CircleShape, clip = false)
                                    .border(width = 1.dp, color = borderColor, shape = CircleShape)
                                    .background(containerColor, CircleShape)
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

                        androidx.compose.animation.AnimatedVisibility(
                            visible = isExpanded,
                            enter = fadeIn(tween(220, easing = FastOutSlowInEasing)) + 
                                    scaleIn(initialScale = 0.9f, animationSpec = tween(220, easing = FastOutSlowInEasing)),
                            exit = fadeOut(tween(180, easing = FastOutSlowInEasing)) + 
                                   scaleOut(targetScale = 0.9f, animationSpec = tween(180, easing = FastOutSlowInEasing))
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 320.dp, height = 620.dp)
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
                                        .clickable { closeExpansion?.invoke() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("X", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        windowManager.addView(composeView, params)
        handleCollapseRelease()
    }

    private fun handleCollapseRelease() {
        if (params.x < 0) {
            animateWindowTo(-collapsedSizePx / 2)
        } else if (params.x > screenWidth - collapsedSizePx) {
            animateWindowTo(screenWidth - collapsedSizePx / 2)
        }
    }

    private fun ensureFullyVisible() {
        val expandedWidth = (320 * Resources.getSystem().displayMetrics.density).roundToInt()
        var targetX = params.x
        if (targetX < 0) targetX = 0
        if (targetX + expandedWidth > screenWidth) {
            targetX = screenWidth - expandedWidth
        }
        if (targetX != params.x) {
            animateWindowTo(targetX)
        }
    }

    private fun animateWindowTo(targetX: Int, targetY: Int? = null) {
        windowAnimatorX?.cancel()
        val animX = ValueAnimator.ofInt(params.x, targetX).apply {
            duration = 250
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                params.x = animation.animatedValue as Int
                try {
                    windowManager.updateViewLayout(composeView, params)
                } catch (e: Exception) {}
            }
        }
        windowAnimatorX = animX
        animX.start()

        if (targetY != null && targetY != params.y) {
            windowAnimatorY?.cancel()
            val animY = ValueAnimator.ofInt(params.y, targetY).apply {
                duration = 250
                interpolator = DecelerateInterpolator()
                addUpdateListener { animation ->
                    params.y = animation.animatedValue as Int
                    try {
                        windowManager.updateViewLayout(composeView, params)
                    } catch (e: Exception) {}
                }
            }
            windowAnimatorY = animY
            animY.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning.value = false
        windowAnimatorX?.cancel()
        windowAnimatorY?.cancel()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        if (::composeView.isInitialized) {
            try {
                windowManager.removeView(composeView)
            } catch (e: Exception) {}
        }
    }
}
