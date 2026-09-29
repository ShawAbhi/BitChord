import os

service_path = 'app/src/main/java/com/music/bitchord/ui/classipod/FloatingPodService.kt'
new_service_content = '''package com.music.bitchord.ui.classipod

import android.animation.ValueAnimator
import android.app.Service
import android.content.Intent
import android.content.res.Resources
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
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
    private val collapsedSizePx = (64 * Resources.getSystem().displayMetrics.density).roundToInt()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
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
            x = screenWidth // Start on the right edge
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

                    SideEffect {
                        if (isExpandedState != isExpanded) {
                            isExpandedState = isExpanded
                            if (isExpanded) {
                                ensureFullyVisible()
                            } else {
                                snapToEdge()
                            }
                        }
                    }

                    if (!isExpanded) {
                        // Collapsed Bubble with iPod icon
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clickable { isExpanded = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ipod_icon),
                                contentDescription = "Pod",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        // Expanded UI
                        Box(
                            modifier = Modifier
                                .size(width = 320.dp, height = 620.dp)
                                .padding(start = 24.dp, top = 32.dp, end = 24.dp, bottom = 56.dp)
                        ) {
                            ClassipodApp(controller, playerState)
                            // Close button floating outside the main body
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
                        }
                    }
                }
            }
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        composeView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isDragging = true
                        params.x = initialX + dx.roundToInt()
                        params.y = initialY + dy.roundToInt()
                        windowManager.updateViewLayout(composeView, params)
                        true
                    } else {
                        false
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (isDragging) {
                        if (!isExpandedState) {
                            snapToEdge()
                        } else {
                            ensureFullyVisible()
                        }
                        true
                    } else {
                        false
                    }
                }
                else -> false
            }
        }

        windowManager.addView(composeView, params)
        snapToEdge()
    }

    private fun snapToEdge() {
        val targetX = if (params.x + (collapsedSizePx / 2) < screenWidth / 2) {
            -collapsedSizePx / 2 // Snap left, half hidden
        } else {
            screenWidth - (collapsedSizePx / 2) // Snap right, half hidden
        }
        
        animateWindowX(targetX)
    }

    private fun ensureFullyVisible() {
        val expandedWidth = (320 * Resources.getSystem().displayMetrics.density).roundToInt()
        var targetX = params.x
        if (targetX < 0) targetX = 0
        if (targetX + expandedWidth > screenWidth) {
            targetX = screenWidth - expandedWidth
        }
        if (targetX != params.x) {
            animateWindowX(targetX)
        }
    }

    private fun animateWindowX(targetX: Int) {
        val animator = ValueAnimator.ofInt(params.x, targetX)
        animator.duration = 250
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { animation ->
            params.x = animation.animatedValue as Int
            try {
                windowManager.updateViewLayout(composeView, params)
            } catch (e: Exception) {}
        }
        animator.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        if (::composeView.isInitialized) {
            windowManager.removeView(composeView)
        }
    }
}
'''
with open(service_path, 'w', encoding='utf-8') as f:
    f.write(new_service_content)
