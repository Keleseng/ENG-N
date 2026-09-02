with open('app/src/main/java/com/example/ui/screens/MainScreen.kt', 'r') as f:
    content = f.read()

# 1. Remove from Rail
start_rail = content.find('          // 4. DENİZ & HAVA DURUMU\n          NavigationRailItem(')
if start_rail != -1:
    end_rail = content.find('          )', start_rail) + 11
    # Check if there is another closing paren?
    end_rail = content.find('          )\n', start_rail) + 12
    content = content[:start_rail] + content[end_rail:]

# 2. Remove from Bar
start_bar = content.find('            // 4. DENİZ & HAVA DURUMU\n            NavigationBarItem(')
if start_bar != -1:
    end_bar = content.find('            )\n', start_bar) + 14
    content = content[:start_bar] + content[end_bar:]

# 3. Remove from when
start_when = content.find('      3 -> MarineWeatherView(')
if start_when != -1:
    end_when = content.find('\n', start_when) + 1
    content = content[:start_when] + content[end_when:]
    
with open('app/src/main/java/com/example/ui/screens/MainScreen.kt', 'w') as f:
    f.write(content)

