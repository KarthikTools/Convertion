import os
from selenium import webdriver
from selenium.webdriver.firefox.service import Service
from selenium.webdriver.firefox.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
import geckodriver_autoinstaller
from bs4 import BeautifulSoup
import openai
from dotenv import load_dotenv
import json
import time
import argparse
import logging
import platform

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

class AccessibilityChecker:
    def __init__(self):
        load_dotenv()
        openai.api_key = os.getenv('OPENAI_API_KEY')
        self.setup_driver()
        self.is_authenticated = False

    def setup_driver(self):
        """Set up the headless Firefox driver"""
        # Install geckodriver if it's not installed
        geckodriver_autoinstaller.install()
        
        firefox_options = Options()
        firefox_options.add_argument("--headless")
        firefox_options.add_argument("--width=1920")
        firefox_options.add_argument("--height=1080")
        
        self.driver = webdriver.Firefox(options=firefox_options)

    def authenticate(self, login_url, username, password, username_field_id=None, password_field_id=None, submit_button_id=None):
        """Handle authentication for the webpage"""
        try:
            logger.info("Attempting to authenticate...")
            self.driver.get(login_url)
            
            # Wait for the login form to be present
            WebDriverWait(self.driver, 10).until(
                EC.presence_of_element_located((By.TAG_NAME, "form"))
            )

            # Find username and password fields
            if username_field_id:
                username_field = self.driver.find_element(By.ID, username_field_id)
            else:
                username_field = self.driver.find_element(By.CSS_SELECTOR, "input[type='text'], input[type='email']")
            
            if password_field_id:
                password_field = self.driver.find_element(By.ID, password_field_id)
            else:
                password_field = self.driver.find_element(By.CSS_SELECTOR, "input[type='password']")

            # Enter credentials
            username_field.clear()
            username_field.send_keys(username)
            password_field.clear()
            password_field.send_keys(password)

            # Find and click submit button
            if submit_button_id:
                submit_button = self.driver.find_element(By.ID, submit_button_id)
            else:
                submit_button = self.driver.find_element(By.CSS_SELECTOR, "button[type='submit'], input[type='submit']")
            
            submit_button.click()

            # Wait for login to complete
            time.sleep(3)  # Basic wait for login to complete
            
            # Check if login was successful
            if "login" not in self.driver.current_url.lower():
                self.is_authenticated = True
                logger.info("Authentication successful!")
                return True
            else:
                logger.error("Authentication failed!")
                return False

        except Exception as e:
            logger.error(f"Error during authentication: {str(e)}")
            return False

    def get_page_content(self, url):
        """Fetch and parse the webpage content"""
        try:
            self.driver.get(url)
            # Wait for the page to load completely
            self.driver.implicitly_wait(10)
            
            # Get the page source and parse with BeautifulSoup
            page_source = self.driver.page_source
            soup = BeautifulSoup(page_source, 'html.parser')
            
            # Extract relevant information
            page_info = {
                'title': soup.title.string if soup.title else 'No title found',
                'headings': [h.text for h in soup.find_all(['h1', 'h2', 'h3', 'h4', 'h5', 'h6'])],
                'images': [{'src': img.get('src', ''), 'alt': img.get('alt', '')} 
                          for img in soup.find_all('img')],
                'links': [{'href': a.get('href', ''), 'text': a.text} 
                         for a in soup.find_all('a')],
                'forms': [{'id': form.get('id', ''), 'action': form.get('action', '')} 
                         for form in soup.find_all('form')]
            }
            
            return page_info
        except Exception as e:
            logger.error(f"Error fetching page content: {str(e)}")
            return None

    def analyze_accessibility(self, page_info):
        """Analyze the page content using OpenAI"""
        try:
            prompt = f"""
            Analyze the following webpage content for accessibility issues and provide a detailed report.
            Focus on WCAG 2.1 guidelines and common accessibility problems.
            
            Page Information:
            {json.dumps(page_info, indent=2)}
            
            Please provide a structured report with:
            1. Critical Issues
            2. Important Issues
            3. Recommendations
            4. Overall Accessibility Score (0-100)
            """

            response = openai.ChatCompletion.create(
                model="gpt-4",
                messages=[
                    {"role": "system", "content": "You are an expert web accessibility consultant."},
                    {"role": "user", "content": prompt}
                ],
                temperature=0.7
            )

            return response.choices[0].message['content']
        except Exception as e:
            logger.error(f"Error analyzing accessibility: {str(e)}")
            return None

    def check_accessibility(self, url, requires_auth=False, login_url=None, username=None, password=None, 
                          username_field_id=None, password_field_id=None, submit_button_id=None):
        """Main method to check accessibility of a webpage"""
        logger.info(f"Analyzing accessibility for: {url}")
        
        # Handle authentication if required
        if requires_auth and not self.is_authenticated:
            if not all([login_url, username, password]):
                return "Authentication required but missing credentials"
            
            if not self.authenticate(login_url, username, password, 
                                   username_field_id, password_field_id, submit_button_id):
                return "Authentication failed"
        
        # Get page content
        page_info = self.get_page_content(url)
        if not page_info:
            return "Failed to fetch page content"

        # Analyze accessibility
        report = self.analyze_accessibility(page_info)
        if not report:
            return "Failed to generate accessibility report"

        return report

    def close(self):
        """Close the browser"""
        self.driver.quit()

def parse_arguments():
    """Parse command line arguments"""
    parser = argparse.ArgumentParser(description='Web Accessibility Checker')
    parser.add_argument('--url', required=True, help='URL to check for accessibility')
    parser.add_argument('--requires-auth', action='store_true', help='Whether the page requires authentication')
    parser.add_argument('--login-url', help='Login page URL')
    parser.add_argument('--username', help='Username for authentication')
    parser.add_argument('--password', help='Password for authentication')
    parser.add_argument('--username-field-id', help='ID of username field')
    parser.add_argument('--password-field-id', help='ID of password field')
    parser.add_argument('--submit-button-id', help='ID of submit button')
    parser.add_argument('--output-file', help='File to save the report')
    return parser.parse_args()

def main():
    args = parse_arguments()
    
    checker = AccessibilityChecker()
    try:
        report = checker.check_accessibility(
            url=args.url,
            requires_auth=args.requires_auth,
            login_url=args.login_url,
            username=args.username,
            password=args.password,
            username_field_id=args.username_field_id,
            password_field_id=args.password_field_id,
            submit_button_id=args.submit_button_id
        )
        
        if args.output_file:
            with open(args.output_file, 'w') as f:
                f.write(report)
            logger.info(f"Report saved to {args.output_file}")
        else:
            print("\nAccessibility Report:")
            print("=" * 50)
            print(report)
            
    finally:
        checker.close()

if __name__ == "__main__":
    main() 