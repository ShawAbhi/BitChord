import os

filepath = 'app/build.gradle.kts'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

bad = 'signingConfig = signingConfigs.findByName("release")'
good_str = 'signingConfig = signingConfigs.getByName("debug")'

if bad in content:
    content = content.replace(bad, good_str)
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)
    print('Fixed signing config')
else:
    print('Not found')
