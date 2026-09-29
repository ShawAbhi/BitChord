import os

service_path = 'app/src/main/java/com/music/bitchord/ui/classipod/FloatingPodService.kt'
with open(service_path, 'r', encoding='utf-8') as f:
    service_content = f.read()

old_box = '''                        // Expanded UI
                        Box(
                            modifier = Modifier
                                .size(width = 320.dp, height = 580.dp)
                                .padding(24.dp) // Large padding to prevent shadow clipping
                        ) {'''

new_box = '''                        // Expanded UI
                        Box(
                            modifier = Modifier
                                .width(320.dp)
                                .height(620.dp)
                                .padding(start = 24.dp, top = 32.dp, end = 24.dp, bottom = 56.dp) // Extra padding at bottom for shadow
                        ) {'''

if old_box in service_content:
    service_content = service_content.replace(old_box, new_box)
    with open(service_path, 'w', encoding='utf-8') as f:
        f.write(service_content)
    print("Fixed bottom shadow clipping.")
else:
    print("Could not find the old box definition.")
