import os
import re

root_dir = r"c:\Users\venka\AndroidStudioProjects\MyFitnessBuddy\frontend\src\main\java\com\simats\myfitnessbuddy"
old_import = "import com.simats.myfitnessbuddy.data.remote.RetrofitClient"
old_full_path = "com.simats.myfitnessbuddy.data.remote.RetrofitClient"
new_path = "RetrofitClient" # Or com.simats.myfitnessbuddy.RetrofitClient

# We also need to check for FriendRequestItem and UserData if they were moved?
# Wait, are ApiService and other things still there? 
# Yes, only RetrofitClient was moved.

for root, dirs, files in os.walk(root_dir):
    for filename in files:
        if filename.endswith(".kt") or filename.endswith(".java"):
            filepath = os.path.join(root, filename)
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()
            
            modified = False
            if old_import in content:
                print(f"Removing import in {filename}...")
                content = content.replace(old_import, "")
                modified = True
            
            if old_full_path in content:
                print(f"Updating full path in {filename}...")
                content = content.replace(old_full_path, "RetrofitClient")
                modified = True
                
            if modified:
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(content)
