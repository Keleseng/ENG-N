import sys

for i, line in enumerate(open('app/src/main/java/com/example/ui/screens/InputParametersView.kt')):
    if line.strip().startswith('//'):
        print(f"L{i+1}: {line.strip()}")
