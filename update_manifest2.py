import os

filepath = 'app/src/main/AndroidManifest.xml'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# Add permission
if 'SYSTEM_ALERT_WINDOW' not in content:
    content = content.replace('<application', '<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />\n    <application')

# Add service
service_xml = '''        <service
            android:name=".ui.classipod.FloatingPodService"
            android:exported="false" />'''

if 'FloatingPodService' not in content:
    content = content.replace('    </application>', service_xml + '\n    </application>')

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)
print('Updated manifest')
