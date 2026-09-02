with open('app/src/main/java/com/example/ui/screens/MainScreen.kt', 'r') as f:
    content = f.read()

if 'import androidx.compose.material.icons.filled.Air' not in content:
    content = content.replace('import androidx.compose.material.icons.filled.*', 'import androidx.compose.material.icons.filled.*\nimport androidx.compose.material.icons.filled.Air\nimport androidx.compose.material.icons.outlined.Air')

with open('app/src/main/java/com/example/ui/screens/MainScreen.kt', 'w') as f:
    f.write(content)
