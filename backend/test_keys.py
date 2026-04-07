import os
import requests
import smtplib
from dotenv import load_dotenv

# Load environment variables from the .env file in the current directory
load_dotenv()

def test_fast2sms_key():
    api_key = os.getenv('FAST2SMS_API_KEY')
    if not api_key:
        print("[-] FAST2SMS_API_KEY not found in .env")
        return

    print(f"[*] Testing Fast2SMS Key: {api_key[:10]}...")
    url = "https://www.fast2sms.com/dev/wallet"
    headers = {
        "authorization": api_key,
        "Content-Type": "application/json"
    }
    
    try:
        response = requests.get(url, headers=headers)
        if response.status_code == 200:
            data = response.json()
            if data.get('status'):
                print(f"[+] Fast2SMS Key is WORKING. Wallet Balance: {data.get('wallet')}")
            else:
                print(f"[-] Fast2SMS Key might be invalid. Response: {data}")
        else:
            print(f"[-] Fast2SMS API returned status code {response.status_code}: {response.text}")
    except Exception as e:
        print(f"[-] Error testing Fast2SMS: {e}")

def test_smtp_login():
    email_user = os.getenv('EMAIL_HOST_USER')
    email_pass = os.getenv('EMAIL_HOST_PASSWORD')
    
    if not email_user or not email_pass:
        print("[-] SMTP credentials not found in .env")
        return

    print(f"[*] Testing SMTP Login for: {email_user}")
    try:
        # Connect to Gmail SMTP
        server = smtplib.SMTP('smtp.gmail.com', 587)
        server.starttls()
        server.login(email_user, email_pass)
        print("[+] SMTP Login is WORKING.")
        server.quit()
    except smtplib.SMTPAuthenticationError:
        print("[-] SMTP Authentication FAILED. Check your email and app password.")
    except Exception as e:
        print(f"[-] Error testing SMTP: {e}")

def test_groq_key():
    api_key = os.getenv('GROQ_API_KEY')
    if not api_key:
        print("[-] GROQ_API_KEY not found in .env")
        return

    print(f"[*] Testing Groq Key: {api_key[:10]}...")
    url = "https://api.groq.com/openai/v1/chat/completions"
    headers = {
        "Authorization": f"Bearer {api_key}",
        "Content-Type": "application/json"
    }
    payload = {
        "model": "llama-3.3-70b-versatile",
        "messages": [{"role": "user", "content": "Hello"}],
        "max_tokens": 5
    }
    
    try:
        response = requests.post(url, headers=headers, json=payload)
        if response.status_code == 200:
            print("[+] Groq Key is WORKING.")
        else:
            print(f"[-] Groq API returned status code {response.status_code}: {response.text}")
    except Exception as e:
        print(f"[-] Error testing Groq: {e}")

if __name__ == "__main__":
    print("--- Testing Keys from .env ---")
    test_fast2sms_key()
    print("-" * 30)
    test_smtp_login()
    print("-" * 30)
    test_groq_key()
