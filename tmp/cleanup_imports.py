import os
import re

root_dir = r"c:\Users\venka\AndroidStudioProjects\MyFitnessBuddy\frontend\src\main\java\com\simats\myfitnessbuddy"
old_import = "import com.simats.myfitnessbuddy.data.remote.RetrofitClient"

for filename in os.listdir(root_dir):
    if filename.endswith(".kt") or filename.endswith(".java"):
        filepath = os.path.join(root_dir, filename)
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        if old_import in content:
            print(f"Updating {filename}...")
            # We remove the import since it's now in the same package
            new_content = content.replace(old_import, "")
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(new_content)
