import sys

depth = 0
prev_depth = 0
for i, line in enumerate(open('app/src/main/java/com/example/ui/screens/InputParametersView.kt')):
    depth += line.count('{') - line.count('}')
    if depth != prev_depth and depth <= 3:
        print(f"L{i+1}: {prev_depth} -> {depth} : {line.strip()}")
    prev_depth = depth

