import os

service_path = 'app/src/main/java/com/music/bitchord/ui/classipod/FloatingPodService.kt'
with open(service_path, 'r', encoding='utf-8') as f:
    service_content = f.read()

# 1. Remove Modifier.clickable
old_box = '''                        // Collapsed Bubble with iPod icon
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clickable { isExpanded = true },
                            contentAlignment = Alignment.Center
                        ) {'''
new_box = '''                        // Collapsed Bubble with iPod icon
                        Box(
                            modifier = Modifier
                                .size(64.dp),
                            contentAlignment = Alignment.Center
                        ) {'''
service_content = service_content.replace(old_box, new_box)


# 2. Update touch listener to handle clicks and inject a callback to toggle state
old_touch = '''        composeView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    false
                }'''

# We need a way to trigger isExpanded from the touch listener. 
# We can declare a mutable state outside setContent or use a ViewModel.
# Since isExpanded is inside setContent, let's use a var callback.

old_set_content = '''            setContent {
                BitChordTheme {
                    val controller = rememberMediaController()
                    val playerState = rememberPlayerState(controller)
                    var isExpanded by remember { mutableStateOf(false) }'''

new_set_content = '''            setContent {
                BitChordTheme {
                    val controller = rememberMediaController()
                    val playerState = rememberPlayerState(controller)
                    var isExpanded by remember { mutableStateOf(false) }
                    
                    // Expose a way for the touch listener to toggle expansion
                    SideEffect {
                        this@FloatingPodService.toggleExpansion = { isExpanded = !isExpanded }
                        this@FloatingPodService.closeExpansion = { isExpanded = false }
                    }'''

service_content = service_content.replace(old_set_content, new_set_content)

# Add properties to the class
old_class_start = '''    private val screenWidth = Resources.getSystem().displayMetrics.widthPixels
    private val collapsedSizePx = (64 * Resources.getSystem().displayMetrics.density).roundToInt()'''

new_class_start = '''    private val screenWidth = Resources.getSystem().displayMetrics.widthPixels
    private val collapsedSizePx = (64 * Resources.getSystem().displayMetrics.density).roundToInt()
    private var toggleExpansion: (() -> Unit)? = null
    private var closeExpansion: (() -> Unit)? = null'''
service_content = service_content.replace(old_class_start, new_class_start)

# Update touch listener
old_touch_listener = '''        composeView.setOnTouchListener { _, event ->
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
                        
                        // Limit dragging so it doesn't go completely off-screen
                        if (!isExpandedState) {
                            if (params.x < -collapsedSizePx / 2) params.x = -collapsedSizePx / 2
                            if (params.x > screenWidth - collapsedSizePx / 2) params.x = screenWidth - collapsedSizePx / 2
                        }
                        
                        windowManager.updateViewLayout(composeView, params)
                        true
                    } else {
                        false
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (isDragging) {
                        if (!isExpandedState) {
                            handleCollapseRelease()
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
        }'''

new_touch_listener = '''        composeView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true // Consume ACTION_DOWN to ensure we get MOVE/UP events
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (!isDragging && (Math.abs(dx) > 10 || Math.abs(dy) > 10)) {
                        isDragging = true
                    }
                    if (isDragging) {
                        params.x = initialX + dx.roundToInt()
                        params.y = initialY + dy.roundToInt()
                        
                        if (!isExpandedState) {
                            if (params.x < -collapsedSizePx / 2) params.x = -collapsedSizePx / 2
                            if (params.x > screenWidth - collapsedSizePx / 2) params.x = screenWidth - collapsedSizePx / 2
                        }
                        
                        windowManager.updateViewLayout(composeView, params)
                        true
                    } else {
                        true
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (isDragging) {
                        if (!isExpandedState) {
                            handleCollapseRelease()
                        } else {
                            ensureFullyVisible()
                        }
                    } else {
                        // It was a click!
                        toggleExpansion?.invoke()
                    }
                    true
                }
                else -> false
            }
        }'''
service_content = service_content.replace(old_touch_listener, new_touch_listener)

# Fix the close button inside expanded view
old_close = '''                            // Close button floating outside the main body
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(30.dp)
                                    .background(Color(0x99000000), CircleShape)
                                    .clickable { isExpanded = false },'''
new_close = '''                            // Close button floating outside the main body
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(30.dp)
                                    .background(Color(0x99000000), CircleShape)
                                    .clickable { closeExpansion?.invoke() },'''
service_content = service_content.replace(old_close, new_close)

with open(service_path, 'w', encoding='utf-8') as f:
    f.write(service_content)
    print("Fixed dragging and clicking.")
