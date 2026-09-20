with open('app/src/main/java/com/example/ui/screens/MarineMapView.kt', 'r') as f:
    lines = f.readlines()

open_count = 0
for i, line in enumerate(lines):
    open_count += line.count('{')
    open_count -= line.count('}')
    if open_count < 0:
        print(f"Negative count at line {i+1}: {line.strip()}")
        break
