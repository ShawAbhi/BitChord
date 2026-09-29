import os

app_path = 'app/src/main/java/com/music/bitchord/ui/classipod/ClassipodApp.kt'
with open(app_path, 'r', encoding='utf-8') as f:
    app_content = f.read()

old_exit = '''            MenuItem("Exit", hasArrow = false) {
                (context as? Activity)?.finish()
            }'''
new_exit = '''            MenuItem("Exit", hasArrow = false) {
                // If it's an activity, finish it. If it's a service, we'd need a callback. 
                // But typically, they can just use the X button to close the expanded view.
                (context as? Activity)?.finish()
            }'''

if old_exit in app_content:
    app_content = app_content.replace(old_exit, new_exit)
    with open(app_path, 'w', encoding='utf-8') as f:
        f.write(app_content)
