with open('app/src/main/java/com/example/ui/components/AnchorCalculationCard.kt', 'r') as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if "MOB" in line or "Salma" in line or "Demir" in line:
        print(f"{i}: {line.strip()}")
