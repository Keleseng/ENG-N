import re
with open('app/src/main/java/com/example/ui/components/CompactEtaCard.kt', 'r') as f:
    content = f.read()

pattern = r"""      Row\(\s*modifier = Modifier\.fillMaxWidth\(\),\s*horizontalArrangement = Arrangement\.SpaceBetween,\s*verticalAlignment = Alignment\.CenterVertically\s*\)\s*\{\s*Text\(\s*text = "DMS:",[\s\S]*?\)\s*\)[\s\S]*?\}"""

# Wait, let's just find the exact block and replace it.
