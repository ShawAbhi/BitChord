import os

service_path = 'app/src/main/java/com/music/bitchord/ui/classipod/FloatingPodService.kt'
with open(service_path, 'r', encoding='utf-8') as f:
    service_content = f.read()

old_logic = '''        var initialX = 0
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
    }'''

new_logic = '''        var initialX = 0
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
        }

        windowManager.addView(composeView, params)
        // Start fully visible by default
        ensureFullyVisible()
    }

    private fun handleCollapseRelease() {
        // If user drags it off the left edge, bury it half-way on the left
        if (params.x < 0) {
            animateWindowX(-collapsedSizePx / 2)
        } 
        // If user drags it off the right edge, bury it half-way on the right
        else if (params.x > screenWidth - collapsedSizePx) {
            animateWindowX(screenWidth - collapsedSizePx / 2)
        }
        // Otherwise, leave it exactly where they dropped it!
    }'''

if old_logic in service_content:
    service_content = service_content.replace(old_logic, new_logic)
    with open(service_path, 'w', encoding='utf-8') as f:
        f.write(service_content)
    print("Logic updated.")
else:
    print("Could not find old logic.")
