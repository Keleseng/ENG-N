with open('app/src/main/java/com/example/ui/screens/InputParametersView.kt', 'r') as f:
    content = f.read()

content = content.replace('isDark = isDark,', 'isDarkMode = isDark,')

with open('app/src/main/java/com/example/ui/screens/InputParametersView.kt', 'w') as f:
    f.write(content)
