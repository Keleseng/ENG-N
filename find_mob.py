with open('app/src/main/java/com/example/ui/screens/MarineMapView.kt', 'r') as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if "LaunchedEffect(" in line or "mobEvent" in line or "anchorEvent" in line or "evaluateJavascript" in line:
        print(f"{i}: {line.strip()}")
