import requests
import json

# Replace with actual user credentials if needed, but we can test logic locally
# Since I'm on the machine, I can just use python to test the model logic

from django.core.management import setup_environ
import os
import sys

# Setup Django environment
sys.path.append('c:/Users/venka/AndroidStudioProjects/MyFitnessBuddy/backend')
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'myfitnessbuddy.settings')
import django
django.setup()

from users.models import DailyStats, User
from django.utils import timezone

try:
    user = User.objects.first()
    if user:
        today = timezone.now().date()
        stats, created = DailyStats.objects.get_or_create(user=user, date=today)
        print(f"Stats found/created: {stats.steps} steps on {stats.date}")
        stats.steps = 5000
        stats.save()
        print(f"Updated stats: {DailyStats.objects.get(id=stats.id).steps} steps")
    else:
        print("No user found")
except Exception as e:
    print(f"Error: {e}")
