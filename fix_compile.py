import os

service_path = 'app/src/main/java/com/music/bitchord/ui/classipod/FloatingPodService.kt'
with open(service_path, 'r', encoding='utf-8') as f:
    service_content = f.read()

old_code = '''                            modifier = Modifier
                                .width(320.dp)
                                .height(620.dp)'''

new_code = '''                            modifier = Modifier
                                .size(width = 320.dp, height = 620.dp)'''

if old_code in service_content:
    service_content = service_content.replace(old_code, new_code)
    with open(service_path, 'w', encoding='utf-8') as f:
        f.write(service_content)
    print("Fixed compilation error.")
else:
    print("Code not found. Could not fix.")
