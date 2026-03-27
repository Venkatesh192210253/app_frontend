import os
import re

dir_path = r"c:\Users\venka\AndroidStudioProjects\MyFitnessBuddy\app\src\main\java\com\simats\myfitnessbuddy"

for root, dirs, files in os.walk(dir_path):
    for file in files:
        if file.endswith(".kt"):
            filepath = os.path.join(root, file)
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()

            new_content = re.sub(r'apiService\.([a-zA-Z0-9_]+)\(\s*token\s*\)', r'apiService.\1()', content)
            new_content = re.sub(r'apiService\.([a-zA-Z0-9_]+)\(\s*token\s*,\s*', r'apiService.\1(', new_content)

            if new_content != content:
                print(f"Updated {filepath}")
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(new_content)
