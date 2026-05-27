import re

with open('app/src/main/java/com/example/ui/screens/OtherScreen.kt', 'r') as f:
    content = f.read()

# Replace Checkboxes
pattern_checkbox = re.compile(
    r'Row\(verticalAlignment = Alignment\.CenterVertically\)\s*\{\s*Checkbox\(checked = ([\w]+), onCheckedChange = \{ \1 = it \}[^\)]*\)\s*Text\("([^"]+)"(?:,\s*fontWeight = FontWeight\.\w+)?(?:,\s*color = [^\)]+)?(?:,\s*fontSize = [^\)]+)?\)\s*\}'
)

# For those with extra kwargs we can just use the function we defined! But wait, `TimeBlock` extra args might be lost. Wait, Text("⏱️ Bật...", color=..., fontSize=...)
# Our `AccessibleCheckboxRow` only takes `text` string! So it strips styling.
# But for accessibility maybe it's fine. Wait, `AccessibleCheckboxRow` doesn't support color.
