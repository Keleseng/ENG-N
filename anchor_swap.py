import re

with open('app/src/main/java/com/example/ui/screens/AnchorCalculationView.kt', 'r') as f:
    content = f.read()

# We know the first item starts at "    // 1. Üst Demirleme & Güvenlik Özeti Şeridi\n    item {\n"
# And the second item starts at "    // 2. Ana Demirleme ve Salma Dairesi Hesaplama Kartı\n    item {\n"

part1_start = content.find("    // 1. Üst Demirleme & Güvenlik Özeti Şeridi")
part2_start = content.find("    // 2. Ana Demirleme ve Salma Dairesi Hesaplama Kartı")
end_index = content.rfind("  }\n}") # End of LazyColumn

if part1_start != -1 and part2_start != -1:
    before = content[:part1_start]
    part1 = content[part1_start:part2_start]
    part2 = content[part2_start:end_index]
    after = content[end_index:]
    
    new_content = before + part2 + part1 + after
    
    with open('app/src/main/java/com/example/ui/screens/AnchorCalculationView.kt', 'w') as f:
        f.write(new_content)
    print("Swapped successfully")
else:
    print("Could not find parts")

