import os

filepath = 'app/src/main/AndroidManifest.xml'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

activity_xml = '''        <activity
            android:name=".ui.classipod.ClassipodActivity"
            android:label="BitChord Pod"
            android:theme="@style/Theme.BitChord"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

    </application>'''

if 'ClassipodActivity' not in content:
    content = content.replace('    </application>', activity_xml)
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)
    print('Added ClassipodActivity to manifest')
else:
    print('Already in manifest')
