with open('app/src/main/java/com/example/ui/screens/MarineMapView.kt', 'r') as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if "═══════════ GPS MEVKİİ GÖRÜNÜMÜ ═══════════" in line:
        # Check if previous line is `if (activeSourceTab == 0) {`
        if "if (activeSourceTab == 0) {" not in lines[i-1]:
            # It's probably the extra `}`
            lines[i-1] = "          if (activeSourceTab == 0) {\n"
        break

with open('app/src/main/java/com/example/ui/screens/MarineMapView.kt', 'w') as f:
    f.writelines(lines)
