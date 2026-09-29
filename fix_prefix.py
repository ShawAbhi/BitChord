import os

service_path = 'app/src/main/java/com/music/bitchord/ui/classipod/FloatingPodService.kt'
with open(service_path, 'r', encoding='utf-8') as f:
    service_content = f.read()

old_drag = "androidx.compose.foundation.gestures.detectDragGestures("
new_drag = "detectDragGestures("
service_content = service_content.replace(old_drag, new_drag)

with open(service_path, 'w', encoding='utf-8') as f:
    f.write(service_content)
    print("Fixed detectDragGestures prefix.")
