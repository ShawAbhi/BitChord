import os

service_path = 'app/src/main/java/com/music/bitchord/ui/classipod/FloatingPodService.kt'
with open(service_path, 'r', encoding='utf-8') as f:
    service_content = f.read()

# Add missing imports at the top
imports = """import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
"""

# Find where to insert imports
import_index = service_content.find('import androidx.compose.foundation.Image')
service_content = service_content[:import_index] + imports + service_content[import_index:]

# Fix the lambda parameter types so compiler can infer
old_drag = """                            onDrag = { change, dragAmount ->
                                change.consume()"""
new_drag = """                            onDrag = { change: PointerInputChange, dragAmount: Offset ->
                                change.consume()"""
service_content = service_content.replace(old_drag, new_drag)

with open(service_path, 'w', encoding='utf-8') as f:
    f.write(service_content)
    print("Fixed user's pointerInput code.")
