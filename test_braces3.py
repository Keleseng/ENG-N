import sys

depth = 0
for i, line in enumerate(open('app/src/main/java/com/example/ui/screens/InputParametersView.kt')):
    depth += line.count('{') - line.count('}')
    print(f"L{i+1} [D:{depth}]")

