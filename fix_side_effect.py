import os

service_path = 'app/src/main/java/com/music/bitchord/ui/classipod/FloatingPodService.kt'
with open(service_path, 'r', encoding='utf-8') as f:
    service_content = f.read()

old_side_effect = '''                    SideEffect {
                        if (isExpandedState != isExpanded) {
                            isExpandedState = isExpanded
                            if (isExpanded) {
                                ensureFullyVisible()
                            } else {
                                snapToEdge()
                            }
                        }
                    }'''

new_side_effect = '''                    SideEffect {
                        if (isExpandedState != isExpanded) {
                            isExpandedState = isExpanded
                            if (isExpanded) {
                                ensureFullyVisible()
                            } else {
                                handleCollapseRelease()
                            }
                        }
                    }'''

if old_side_effect in service_content:
    service_content = service_content.replace(old_side_effect, new_side_effect)
    with open(service_path, 'w', encoding='utf-8') as f:
        f.write(service_content)
    print("Fixed SideEffect.")
else:
    print("Could not find SideEffect.")
