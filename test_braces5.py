import sys

depth = 0
prev_depth = 0
for i, line in enumerate(open('app/src/main/java/com/example/ui/screens/InputParametersView.kt')):
    depth += line.count('{') - line.count('}')
    if i + 1 == 149 or i + 1 == 150 or i + 1 == 549 or i + 1 == 550 or i + 1 == 551 or i + 1 == 552:
        print(f"L{i+1}: {line.strip()} (depth: {depth})")

