import sys

depth = 0
for i, line in enumerate(open('app/src/main/java/com/example/ui/screens/InputParametersView.kt')):
    depth += line.count('{') - line.count('}')
    if i + 1 == 150 or i + 1 == 551 or i + 1 == 1190 or i + 1 == 1195 or i + 1 == 1191 or i + 1 == 1192 or i + 1 == 1193:
        print(f"Line {i+1} depth: {depth}")

