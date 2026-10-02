package com.music.bitchord.ui.classipod

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowInsets
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The Classipod picture-in-picture: a chat-head bubble and the iPod it opens.
 *
 * It behaves like a Messenger chat head:
 *  - **Tap the bubble** to open the iPod. The bubble springs to the iPod's top
 *    right corner and the iPod grows out of it. Tap again to close; the iPod
 *    shrinks back into the bubble and the bubble returns to its edge.
 *  - **Drag the bubble** anywhere. On release it is flung to the nearest side
 *    with a bouncy spring that keeps the finger's momentum. Dragging it while
 *    the iPod is open folds the iPod back into it first.
 *  - **Drag it to the bottom** to close everything. A close target slides up;
 *    near it the bubble is pulled in magnetically and sticks until the finger
 *    pulls away again. Releasing while stuck, or flinging into the target,
 *    closes the PIP and stops the service.
 *  - **Drag the iPod's body** to move the open iPod and its bubble together.
 *
 * It is three overlay windows rather than one, so each can be sized and moved
 * on its own without the open iPod resizing a shared window mid-animation:
 *  - the **close target**, hidden (zero alpha, untouchable) while nothing is dragged;
 *  - the **iPod**, hidden the same way while closed;
 *  - the **bubble**, added last so it is drawn on top of both.
 * A hidden window gets window alpha 0 rather than just transparent content:
 * from Android 12, an untouchable overlay that is not see-through still
 * blocks touches to the apps under it.
 *
 * Positions are driven by Compose [Animatable]s on [AndroidUiDispatcher.Main],
 * so every move is a spring the next touch can interrupt without a jump.
 *
 * Always start it through [start]: the windows need the "Display over other
 * apps" grant, and [start] asks for it through [ClassipodActivity] when it is
 * missing.
 */
class FloatingPodService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    companion object {
        private const val TAG = "BitChord"

        /** Stops the service when sent as the start intent's action. */
        const val ACTION_STOP = "com.music.bitchord.classipod.STOP"

        /** True while the PIP is on screen; the in-app launcher hides itself on it. */
        val isRunning = mutableStateOf(false)

        /**
         * Opens the PIP, asking for the overlay permission first if it has not
         * been granted. Safe to call from any UI click handler.
         */
        fun start(context: Context) {
            if (Settings.canDrawOverlays(context)) {
                context.startService(Intent(context, FloatingPodService::class.java))
            } else {
                context.startActivity(
                    Intent(context, ClassipodActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }

        /** Closes the PIP and stops the service. */
        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingPodService::class.java))
        }
    }

    // ── Owners for the ComposeViews ──────────────────────────────────────

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    // ── Windows ──────────────────────────────────────────────────────────

    /**
     * The context the windows are created with. From Android 11 a service
     * should add overlay windows through a window context, which carries the
     * right metrics and configuration (rotation, density) for its display.
     */
    private lateinit var uiContext: Context
    private lateinit var windowManager: WindowManager

    private var bubbleView: ComposeView? = null
    private var podView: ComposeView? = null
    private var targetView: ComposeView? = null
    private lateinit var bubbleParams: WindowManager.LayoutParams
    private lateinit var podParams: WindowManager.LayoutParams
    private lateinit var targetParams: WindowManager.LayoutParams

    private val scope = CoroutineScope(SupervisorJob() + AndroidUiDispatcher.Main)

    // ── Motion state ─────────────────────────────────────────────────────

    /** The bubble's top-left corner on screen, in px. The iPod hangs off it. */
    private val bubblePos = Animatable(Offset.Zero, Offset.VectorConverter, POSITION_THRESHOLD)

    /** The bubble's scale: pops in on start, squashes on press, shrinks on dismiss. */
    private val bubbleScale = Animatable(0f)

    /** 0 = iPod closed (shrunk into the bubble), 1 = fully open. */
    private val podProgress = Animatable(0f)

    private val targetShown = mutableStateOf(false)
    private val targetHot = mutableStateOf(false)

    /** How far the close target leans toward the bubble as it gets near. */
    private val targetPull = mutableStateOf(Offset.Zero)

    private var expanded = false
    private var podWindowShown = false
    private var dismissing = false

    /** Where the bubble rests while the iPod is closed. */
    private var restPos = Offset.Zero

    /** Whether that rest is half buried in the screen edge. */
    private var restBuried = false

    /** The bubble position the iPod window currently hangs off. */
    private var podAnchor = Offset.Zero

    /** Whether the iPod window follows the bubble frame by frame (only while dragging the iPod). */
    private var podTracksBubble = false

    private var motionJob: Job? = null
    private var targetHideJob: Job? = null

    // Touch tracking for the bubble, in raw screen coordinates so the window
    // moving under the finger doesn't skew the deltas.
    private var downRaw = Offset.Zero
    private var downPos = Offset.Zero
    private var dragging = false
    private var magnetized = false
    private var catchUpUntil = 0L
    private var velocityTracker: VelocityTracker? = null

    private val touchSlop by lazy { ViewConfiguration.get(uiContext).scaledTouchSlop }
    private val vibrator by lazy { getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) stopSelf()
        // Not worth recreating after the process dies: the PIP would come
        // back with nothing having asked for it.
        return START_NOT_STICKY
    }

    override fun onCreate() {
        super.onCreate()

        // Last line of defence: every launch path is meant to go through
        // [start], but an overlay without the grant throws from addView.
        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "FloatingPodService started without overlay permission; stopping")
            stopSelf()
            return
        }

        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        uiContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val display = getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)
            createWindowContext(display, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null)
        } else {
            this
        }
        windowManager = uiContext.getSystemService(WINDOW_SERVICE) as WindowManager

        try {
            addWindows()
        } catch (e: Exception) {
            // Permission revoked between the check and here, or an OEM refusing
            // the window type. Either way there is nothing to show.
            Log.w(TAG, "Could not add the Classipod windows: ${e.message}")
            stopSelf()
            return
        }
        isRunning.value = true

        // Start on the right edge, a quarter of the way down, and pop in.
        val area = area()
        restPos = clampRest(Offset(area.right, area.top + (area.bottom - area.top) * 0.25f))
        snap(restPos)
        scope.launch {
            bubbleScale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 400f))
        }
    }

    private fun addWindows() {
        // Added in this order so the bubble is drawn over the iPod, and both
        // over the close target.
        targetParams = overlayParams(dpInt(TARGET_WINDOW_DP), dpInt(TARGET_WINDOW_DP), touchable = false)
            .apply { alpha = 0f }
        targetView = composeView {
            DismissTarget(
                shown = targetShown.value,
                hot = targetHot.value,
                pull = targetPull.value,
            )
        }
        windowManager.addView(targetView, targetParams)

        podParams = overlayParams(
            dpInt(POD_WIDTH_DP + 2 * POD_SHADOW_DP),
            dpInt(POD_HEIGHT_DP + 2 * POD_SHADOW_DP),
            touchable = false,
        ).apply { alpha = 0f }
        podView = composeView {
            PodPanel(
                progress = { podProgress.value },
                onDrag = ::onPodDrag,
                onDragEnd = ::onPodDragEnd,
            )
        }
        windowManager.addView(podView, podParams)

        bubbleParams = overlayParams(
            dpInt(BUBBLE_DP + 2 * BUBBLE_MARGIN_DP),
            dpInt(BUBBLE_DP + 2 * BUBBLE_MARGIN_DP),
            touchable = true,
        )
        bubbleView = composeView {
            Bubble(scale = { bubbleScale.value })
        }.apply {
            contentDescription = "iPod"
            setOnTouchListener(::onBubbleTouch)
            // Taps are routed through performClick so TalkBack can toggle too.
            setOnClickListener { toggle() }
        }
        windowManager.addView(bubbleView, bubbleParams)
    }

    // ── Bubble touch handling ────────────────────────────────────────────

    @SuppressLint("ClickableViewAccessibility")
    private fun onBubbleTouch(view: View, event: MotionEvent): Boolean {
        if (dismissing) return true
        val raw = Offset(event.rawX, event.rawY)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                velocityTracker?.recycle()
                velocityTracker = VelocityTracker.obtain()
                track(event)
                downRaw = raw
                // Catch the bubble mid-flight, wherever it is.
                motionJob?.cancel()
                downPos = bubblePos.value
                dragging = false
                press(true)
            }
            MotionEvent.ACTION_MOVE -> {
                track(event)
                val delta = raw - downRaw
                if (!dragging && delta.getDistance() > touchSlop) {
                    dragging = true
                    onDragStart()
                }
                if (dragging) onDragTo(downPos + delta)
            }
            MotionEvent.ACTION_UP -> {
                track(event)
                press(false)
                if (dragging) {
                    val tracker = velocityTracker
                    tracker?.computeCurrentVelocity(1000)
                    onDragEnd(Offset(tracker?.xVelocity ?: 0f, tracker?.yVelocity ?: 0f))
                } else {
                    view.performClick()
                }
                velocityTracker?.recycle()
                velocityTracker = null
            }
            MotionEvent.ACTION_CANCEL -> {
                press(false)
                if (dragging) onDragEnd(Offset.Zero)
                velocityTracker?.recycle()
                velocityTracker = null
            }
        }
        return true
    }

    /** Feeds the velocity tracker raw coordinates, since the window itself moves. */
    private fun track(event: MotionEvent) {
        val copy = MotionEvent.obtain(event)
        copy.setLocation(event.rawX, event.rawY)
        velocityTracker?.addMovement(copy)
        copy.recycle()
    }

    private fun onDragStart() {
        // Dragging the bubble always means "move the bubble", so an open iPod
        // folds back into it and follows it until the drag ends.
        if (expanded) collapse(returnToRest = false)
        magnetized = false
        catchUpUntil = 0L
        showTarget()
    }

    private fun onDragTo(free: Offset) {
        val half = dp(BUBBLE_DP) / 2f
        val center = free + Offset(half, half)
        val targetCenter = targetCenter()
        val toTarget = center - targetCenter
        val distance = toTarget.getDistance()

        // The target leans toward a bubble that is getting close.
        targetPull.value = if (!magnetized && distance < dp(PULL_RANGE_DP)) {
            val limit = dp(MAX_PULL_DP)
            Offset(
                (toTarget.x * PULL_FACTOR).coerceIn(-limit, limit),
                (toTarget.y * PULL_FACTOR).coerceIn(-limit, limit),
            )
        } else {
            Offset.Zero
        }

        if (!magnetized && distance < dp(MAGNET_CAPTURE_DP)) {
            // Caught: snap into the target with a bounce and stay there.
            magnetized = true
            targetHot.value = true
            targetPull.value = Offset.Zero
            tick()
            springTo(magnetPos(), MAGNET_SPRING)
            return
        }
        if (magnetized) {
            if (distance < dp(MAGNET_RELEASE_DP)) return
            // Pulled free: spring back under the finger rather than jumping.
            magnetized = false
            targetHot.value = false
            catchUpUntil = System.currentTimeMillis() + CATCH_UP_MS
        }

        val clamped = clampDrag(free)
        if (System.currentTimeMillis() < catchUpUntil) {
            springTo(clamped, CATCH_UP_SPRING)
        } else {
            motionJob?.cancel()
            snap(clamped)
        }
    }

    private fun onDragEnd(velocity: Offset) {
        if (magnetized) {
            dismiss()
            return
        }
        // A hard fling at the target counts as dropping on it.
        val half = dp(BUBBLE_DP) / 2f
        val projected = bubblePos.value + Offset(half, half) + velocity * FLING_PROJECTION_S
        if (velocity.y > dp(FLING_DISMISS_MIN_DP_S) &&
            (projected - targetCenter()).getDistance() < dp(MAGNET_CAPTURE_DP)
        ) {
            targetHot.value = true
            dismiss()
            return
        }
        hideTarget()
        flingToEdge(velocity)
    }

    private fun press(down: Boolean) {
        if (dismissing) return
        scope.launch {
            if (down) {
                bubbleScale.animateTo(PRESSED_SCALE, spring(stiffness = Spring.StiffnessHigh))
            } else {
                // Released: pop back past full size and settle, like a chat head.
                bubbleScale.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 700f))
            }
        }
    }

    // ── Opening and closing the iPod ─────────────────────────────────────

    private fun toggle() {
        if (dismissing) return
        if (expanded) collapse(returnToRest = true) else expand()
    }

    private fun expand() {
        expanded = true
        // Remembered as it is, buried or not, so closing puts it back there.
        restPos = clampRest(bubblePos.value, restBuried)
        val anchor = clampExpanded(bubblePos.value)
        // The iPod is placed once, at its final spot, and only its content
        // animates. Moving a window this size every frame alongside the bubble
        // is what made opening stutter.
        podTracksBubble = false
        placePod(anchor)
        showPodWindow()
        springTo(anchor, OPEN_SPRING)
        scope.launch {
            podProgress.animateTo(1f, POD_OPEN_SPRING)
        }
    }

    /**
     * Shrinks the iPod back into the bubble.
     *
     * The iPod window stays where it is while its content shrinks into its top
     * right corner; only the small bubble window moves.
     *
     * @param returnToRest whether the bubble springs back to where it rested
     *   before the iPod opened. False when a drag is about to take over.
     */
    private fun collapse(returnToRest: Boolean) {
        expanded = false
        podTracksBubble = false
        setPodTouchable(false)
        if (returnToRest) springTo(clampRest(restPos, restBuried), CLOSE_SPRING)
        scope.launch {
            podProgress.animateTo(0f, POD_CLOSE_SPRING)
            if (!expanded) hidePodWindow()
        }
    }

    private fun onPodDrag(delta: Offset) {
        if (!expanded || dismissing) return
        motionJob?.cancel()
        // From here the iPod and its bubble move as one.
        podTracksBubble = true
        podAnchor += delta
        snap(podAnchor)
    }

    private fun onPodDragEnd() {
        if (!expanded) return
        podTracksBubble = true
        podAnchor = clampExpanded(podAnchor)
        springTo(podAnchor, SETTLE_SPRING)
    }

    /** Puts the iPod window where it hangs off a bubble at [anchor]. */
    private fun placePod(anchor: Offset) {
        podAnchor = anchor
        val shadow = dp(POD_SHADOW_DP)
        podParams.x = (anchor.x + dp(BUBBLE_DP) - dp(POD_WIDTH_DP) - shadow).roundToInt()
        podParams.y = (anchor.y + dp(BUBBLE_DP) + dp(POD_GAP_DP) - shadow).roundToInt()
        update(podView, podParams)
    }

    private fun showPodWindow() {
        podWindowShown = true
        podParams.alpha = 1f
        podParams.flags = podParams.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        update(podView, podParams)
    }

    private fun setPodTouchable(touchable: Boolean) {
        podParams.flags = if (touchable) {
            podParams.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        } else {
            podParams.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
        update(podView, podParams)
    }

    private fun hidePodWindow() {
        podWindowShown = false
        podParams.alpha = 0f
        podParams.flags = podParams.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        update(podView, podParams)
    }

    // ── Close target ─────────────────────────────────────────────────────

    private fun showTarget() {
        targetHideJob?.cancel()
        val center = targetCenter()
        val half = dp(TARGET_WINDOW_DP) / 2f
        targetParams.x = (center.x - half).roundToInt()
        targetParams.y = (center.y - half).roundToInt()
        targetParams.alpha = 1f
        update(targetView, targetParams)
        targetHot.value = false
        targetPull.value = Offset.Zero
        targetShown.value = true
    }

    private fun hideTarget() {
        targetShown.value = false
        targetHot.value = false
        targetPull.value = Offset.Zero
        targetHideJob?.cancel()
        targetHideJob = scope.launch {
            // Let the slide-out finish before the window goes fully transparent.
            delay(TARGET_HIDE_DELAY_MS)
            targetParams.alpha = 0f
            update(targetView, targetParams)
        }
    }

    private fun dismiss() {
        if (dismissing) return
        dismissing = true
        heavyClick()
        if (expanded) collapse(returnToRest = false)
        motionJob?.cancel()
        scope.launch {
            bubblePos.animateTo(magnetPos(), MAGNET_SPRING) { applyBubble(value) }
            bubbleScale.animateTo(0f, tween(durationMillis = 160))
            targetShown.value = false
            delay(TARGET_HIDE_DELAY_MS)
            stopSelf()
        }
    }

    // ── Motion helpers ───────────────────────────────────────────────────

    /**
     * Flings the bubble to the nearest side, carrying the finger's momentum.
     *
     * It lands half buried in that edge when it was let go already pushed
     * partly off it ([BURY_TRIGGER] of its width), or flicked hard toward it
     * from close by — and fully on screen otherwise. Either way it is the same
     * spring, so burying bounces in exactly like a normal snap, and dragging a
     * buried bubble back out needs nothing special.
     */
    private fun flingToEdge(velocity: Offset) {
        val area = area()
        val size = dp(BUBBLE_DP)
        val margin = dp(EDGE_MARGIN_DP)
        val pos = bubblePos.value
        val projected = pos + velocity * FLING_PROJECTION_S
        val toRight = projected.x + size / 2f > (area.left + area.right) / 2f

        // How much of the bubble is past the chosen edge, now and where the
        // fling is heading. 0 = fully on screen, 0.5 = half off.
        fun offscreen(x: Float) = if (toRight) (x + size - area.right) / size else (area.left - x) / size
        val towardEdge = if (toRight) velocity.x > 0f else velocity.x < 0f
        val bury = offscreen(pos.x) >= BURY_TRIGGER ||
            (towardEdge &&
                kotlin.math.abs(velocity.x) > dp(BURY_FLING_MIN_DP_S) &&
                offscreen(projected.x) >= 0.5f)

        restBuried = bury
        val x = restX(toRight, bury, area, size, margin)
        val y = projected.y
            .coerceIn(area.top + margin, max(area.top + margin, area.bottom - margin - size))
        restPos = Offset(x, y)
        springTo(restPos, FLING_SPRING, velocity)
    }

    /** The resting x on a side: half off the edge when buried, [EDGE_MARGIN_DP] in from it otherwise. */
    private fun restX(toRight: Boolean, buried: Boolean, area: Area, size: Float, margin: Float): Float =
        when {
            toRight && buried -> area.right - size / 2f
            toRight -> area.right - margin - size
            buried -> area.left - size / 2f
            else -> area.left + margin
        }

    /** Where the bubble rests while closed: on the nearest side, buried or on screen. */
    private fun clampRest(p: Offset, buried: Boolean = false): Offset {
        val area = area()
        val size = dp(BUBBLE_DP)
        val margin = dp(EDGE_MARGIN_DP)
        val toRight = p.x + size / 2f > (area.left + area.right) / 2f
        val x = restX(toRight, buried, area, size, margin)
        val y = p.y.coerceIn(area.top + margin, max(area.top + margin, area.bottom - margin - size))
        return Offset(x, y)
    }

    /** Where the bubble may be while open, so the iPod hanging off it fits on screen. */
    private fun clampExpanded(p: Offset): Offset {
        val area = area()
        val size = dp(BUBBLE_DP)
        val margin = dp(EDGE_MARGIN_DP)
        val minX = area.left + margin + dp(POD_WIDTH_DP) - size
        val maxX = area.right - margin - size
        val minY = area.top + margin
        val maxY = area.bottom - margin - size - dp(POD_GAP_DP) - dp(POD_HEIGHT_DP)
        return Offset(p.x.coerceIn(minX, max(minX, maxX)), p.y.coerceIn(minY, max(minY, maxY)))
    }

    /** Where the bubble may be dragged: anywhere, up to half off either side. */
    private fun clampDrag(p: Offset): Offset {
        val area = area()
        val size = dp(BUBBLE_DP)
        return Offset(
            p.x.coerceIn(area.left - size * 0.5f, area.right - size * 0.5f),
            p.y.coerceIn(area.top, max(area.top, area.bottom - size)),
        )
    }

    private fun targetCenter(): Offset {
        val area = area()
        return Offset(
            (area.left + area.right) / 2f,
            area.bottom - dp(TARGET_BOTTOM_MARGIN_DP) - dp(TARGET_SIZE_DP) / 2f,
        )
    }

    /** The bubble position that centres it on the close target. */
    private fun magnetPos(): Offset {
        val half = dp(BUBBLE_DP) / 2f
        return targetCenter() - Offset(half, half)
    }

    private fun springTo(
        target: Offset,
        spec: AnimationSpec<Offset>,
        initialVelocity: Offset = bubblePos.velocity,
    ) {
        motionJob?.cancel()
        motionJob = scope.launch {
            bubblePos.animateTo(target, spec, initialVelocity) { applyBubble(value) }
        }
    }

    private fun snap(p: Offset) {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            bubblePos.snapTo(p)
            applyBubble(p)
        }
    }

    /**
     * Moves the bubble window — and the iPod window with it, but only while the
     * two are being moved as one (dragging the iPod's body). Opening and closing
     * leave the iPod window still and animate its content instead.
     */
    private fun applyBubble(p: Offset) {
        val margin = dp(BUBBLE_MARGIN_DP)
        bubbleParams.x = (p.x - margin).roundToInt()
        bubbleParams.y = (p.y - margin).roundToInt()
        update(bubbleView, bubbleParams)
        if (podWindowShown && podTracksBubble) placePod(p)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Rotation or a resize: put everything back on screen once the new
        // metrics are in.
        bubbleView?.post {
            if (dismissing) return@post
            if (expanded) {
                podTracksBubble = true
                podAnchor = clampExpanded(podAnchor)
                springTo(podAnchor, SETTLE_SPRING)
            } else {
                restPos = clampRest(bubblePos.value, restBuried)
                springTo(restPos, SETTLE_SPRING)
            }
        }
    }

    // ── Plumbing ─────────────────────────────────────────────────────────

    private data class Area(val left: Float, val top: Float, val right: Float, val bottom: Float)

    /** The part of the screen clear of the system bars and cutouts, in px. */
    private fun area(): Area {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            val insets = metrics.windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout(),
            )
            val bounds = metrics.bounds
            return Area(
                left = insets.left.toFloat(),
                top = insets.top.toFloat(),
                right = (bounds.width() - insets.right).toFloat(),
                bottom = (bounds.height() - insets.bottom).toFloat(),
            )
        }
        val real = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(real)
        return Area(
            left = 0f,
            top = systemDimen("status_bar_height"),
            right = real.widthPixels.toFloat(),
            bottom = real.heightPixels - systemDimen("navigation_bar_height"),
        )
    }

    @SuppressLint("DiscouragedApi", "InternalInsetResource")
    private fun systemDimen(name: String): Float {
        val id = resources.getIdentifier(name, "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id).toFloat() else 0f
    }

    private fun dp(value: Int): Float = value * uiContext.resources.displayMetrics.density
    private fun dpInt(value: Int): Int = dp(value).roundToInt()

    private fun overlayParams(width: Int, height: Int, touchable: Boolean) =
        WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                // Overlay windows added from a service are software-rendered
                // unless asked otherwise, which is the single biggest cost in
                // scaling the whole iPod every frame.
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                (if (touchable) 0 else WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE),
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

    private fun composeView(content: @Composable () -> Unit) = ComposeView(uiContext).apply {
        setViewTreeLifecycleOwner(this@FloatingPodService)
        setViewTreeViewModelStoreOwner(this@FloatingPodService)
        setViewTreeSavedStateRegistryOwner(this@FloatingPodService)
        setContent { BitChordTheme { content() } }
    }

    /** Applies new layout params. Safe to call before the window's first layout. */
    private fun update(view: View?, params: WindowManager.LayoutParams) {
        if (view == null) return
        try {
            windowManager.updateViewLayout(view, params)
        } catch (_: Exception) {
            // Not (or no longer) added; nothing to move.
        }
    }

    private fun tick() = vibrate(VibrationEffect.EFFECT_TICK, 10)
    private fun heavyClick() = vibrate(VibrationEffect.EFFECT_HEAVY_CLICK, 30)

    private fun vibrate(effect: Int, fallbackMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(effect))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(fallbackMs)
            }
        } catch (_: Exception) {
        }
    }

    override fun onDestroy() {
        isRunning.value = false
        scope.cancel()
        velocityTracker?.recycle()
        velocityTracker = null
        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
        if (::windowManager.isInitialized) {
            listOf(bubbleView, podView, targetView).forEach { view ->
                if (view != null) runCatching { windowManager.removeView(view) }
            }
        }
        bubbleView = null
        podView = null
        targetView = null
        store.clear()
        super.onDestroy()
    }
}

// ── Sizes (dp) ───────────────────────────────────────────────────────────

private const val BUBBLE_DP = 64
/** Room around the bubble inside its window for the shadow and the press bounce. */
private const val BUBBLE_MARGIN_DP = 10
/** How far a resting bubble sits from the screen edge. */
private const val EDGE_MARGIN_DP = 8

/** The iPod body. */
private const val POD_WIDTH_DP = 272
private const val POD_HEIGHT_DP = 532
/** Room around the iPod body inside its window for its shadow and a little overshoot. */
private const val POD_SHADOW_DP = 20
/** Space between the bubble and the top of the iPod beneath it. */
private const val POD_GAP_DP = 8

private const val TARGET_SIZE_DP = 64
/** The close target's window: big enough for the target to grow and lean toward the bubble. */
private const val TARGET_WINDOW_DP = 160
private const val TARGET_BOTTOM_MARGIN_DP = 40

/** Within this distance of the target the bubble is captured… */
private const val MAGNET_CAPTURE_DP = 88
/** …and it has to be pulled this far away to break free again. */
private const val MAGNET_RELEASE_DP = 128
/** The target starts leaning toward the bubble inside this distance. */
private const val PULL_RANGE_DP = 220
private const val PULL_FACTOR = 0.12f
private const val MAX_PULL_DP = 20

/** Released with at least this much of its width past an edge, the bubble buries itself in it. */
private const val BURY_TRIGGER = 0.25f
/** A flick toward a nearby edge faster than this buries it too, in dp per second. */
private const val BURY_FLING_MIN_DP_S = 600

private const val PRESSED_SCALE = 0.88f
/** How far ahead a fling is projected when choosing where it lands, in seconds. */
private const val FLING_PROJECTION_S = 0.15f
/** A downward fling faster than this toward the target dismisses, in dp per second. */
private const val FLING_DISMISS_MIN_DP_S = 900
/** How long the bubble springs to catch up with the finger after leaving the magnet. */
private const val CATCH_UP_MS = 220L
private const val TARGET_HIDE_DELAY_MS = 260L

private val POSITION_THRESHOLD = Offset(0.5f, 0.5f)

// Springs. A damping ratio below 1 overshoots and settles back — the bounce.
private val FLING_SPRING = spring(dampingRatio = 0.55f, stiffness = 220f, visibilityThreshold = POSITION_THRESHOLD)
private val OPEN_SPRING = spring(dampingRatio = 0.62f, stiffness = 320f, visibilityThreshold = POSITION_THRESHOLD)
private val CLOSE_SPRING = spring(dampingRatio = 0.6f, stiffness = 260f, visibilityThreshold = POSITION_THRESHOLD)
private val SETTLE_SPRING = spring(dampingRatio = 0.6f, stiffness = 300f, visibilityThreshold = POSITION_THRESHOLD)
private val MAGNET_SPRING = spring(dampingRatio = 0.5f, stiffness = 650f, visibilityThreshold = POSITION_THRESHOLD)
private val POD_OPEN_SPRING = spring(dampingRatio = 0.75f, stiffness = 450f, visibilityThreshold = 0.002f)
private val POD_CLOSE_SPRING = spring(dampingRatio = 1f, stiffness = 700f, visibilityThreshold = 0.004f)
private val CATCH_UP_SPRING = spring(dampingRatio = 0.8f, stiffness = 1400f, visibilityThreshold = POSITION_THRESHOLD)

// ── Content ──────────────────────────────────────────────────────────────

/** The chat-head bubble, styled like an adaptive app icon. Purely visual; the window handles touch. */
@Composable
private fun Bubble(scale: () -> Float) {
    val isDark = isSystemInDarkTheme()
    val containerColor = if (isDark) Color(0xFF202124) else Color.White
    val borderColor = if (isDark) Color(0x33FFFFFF) else Color(0x1F000000)

    Box(
        modifier = Modifier
            .size((BUBBLE_DP + 2 * BUBBLE_MARGIN_DP).dp)
            .padding(BUBBLE_MARGIN_DP.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(BUBBLE_DP.dp)
                .graphicsLayer {
                    val s = scale()
                    scaleX = s
                    scaleY = s
                }
                .shadow(elevation = 6.dp, shape = CircleShape, clip = false)
                .border(width = 1.dp, color = borderColor, shape = CircleShape)
                .background(containerColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ipod_icon_white),
                contentDescription = null,
                modifier = Modifier.size(34.dp),
                tint = if (isDark) Color.White else Color(0xFF1F1F1F),
            )
        }
    }
}

/**
 * The iPod, growing out of and shrinking back into the bubble at its top right.
 *
 * [progress] is only read in the draw phase, so opening and closing redraw
 * the iPod without recomposing it.
 */
@Composable
private fun PodPanel(
    progress: () -> Float,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
) {
    val controller = rememberMediaController()
    val playerState = rememberPlayerState(controller)

    // The bubble's centre, as a fraction of the iPod body: the point the iPod
    // scales from, so it looks like it comes out of the bubble.
    val origin = TransformOrigin(
        pivotFractionX = (POD_WIDTH_DP - BUBBLE_DP / 2f) / POD_WIDTH_DP,
        pivotFractionY = -(BUBBLE_DP / 2f + POD_GAP_DP) / POD_HEIGHT_DP,
    )

    Box(
        modifier = Modifier
            .size((POD_WIDTH_DP + 2 * POD_SHADOW_DP).dp, (POD_HEIGHT_DP + 2 * POD_SHADOW_DP).dp)
            .padding(POD_SHADOW_DP.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = progress()
                    val s = MIN_POD_SCALE + (1f - MIN_POD_SCALE) * p
                    scaleX = s
                    scaleY = s
                    // Faded only over the first stretch. Below full opacity
                    // the whole iPod is drawn into an offscreen buffer every
                    // frame; at full opacity a scale is just a matrix change
                    // on an already-recorded layer.
                    alpha = (p * 4f).coerceIn(0f, 1f)
                    transformOrigin = origin
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onDrag(dragAmount)
                        },
                        onDragEnd = onDragEnd,
                        onDragCancel = onDragEnd,
                    )
                },
        ) {
            ClassipodApp(controller, playerState)
        }
    }
}

private const val MIN_POD_SCALE = 0.1f

/** The close target at the bottom of the screen. Grows and turns red when the bubble is caught. */
@Composable
private fun DismissTarget(shown: Boolean, hot: Boolean, pull: Offset) {
    val scale by animateFloatAsState(
        targetValue = if (hot) 1.3f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 500f),
        label = "targetScale",
    )
    val lean by animateOffsetAsState(
        targetValue = pull,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 600f),
        label = "targetLean",
    )
    val color by animateColorAsState(
        targetValue = if (hot) Color(0xE6D93025) else Color(0x99000000),
        label = "targetColor",
    )

    Box(
        modifier = Modifier.size(TARGET_WINDOW_DP.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = shown,
            enter = fadeIn() + scaleIn(initialScale = 0.6f) + slideInVertically { it },
            exit = fadeOut() + scaleOut(targetScale = 0.6f) + slideOutVertically { it },
        ) {
            Box(
                modifier = Modifier
                    .size(TARGET_SIZE_DP.dp)
                    .graphicsLayer {
                        translationX = lean.x
                        translationY = lean.y
                        scaleX = scale
                        scaleY = scale
                    }
                    .background(color, CircleShape)
                    .border(1.dp, Color(0x33FFFFFF), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Drop here to close",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}
