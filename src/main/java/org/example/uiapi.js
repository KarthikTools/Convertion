const { chromium } = require('playwright');

async function loginWithUI(retries) {
    const browser = await chromium.launch();
    const page = await browser.newPage();
    let success = false;

    for (let attempt = 1; attempt <= retries; attempt++) {
        try {
            await page.goto('https://example.com/login');
            await page.fill('#username', 'user');
            await page.fill('#password', 'password');
            await page.click('#loginButton');

            // Wait for some element that appears only on successful login
            await page.waitForSelector('#successElement', { timeout: 5000 });
            console.log('Login successful with UI automation');
            success = true;
            break;
        } catch (error) {
            console.log(`UI login attempt ${attempt} failed. Retrying...`);
        }
    }

    await browser.close();
    return success;
}

async function loginWithAPI() {
    const fetch = require('node-fetch');

    const response = await fetch('https://example.com/api/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            username: 'user',
            password: 'password'
        })
    });

    if (response.ok) {
        console.log('Login successful with API');
    } else {
        console.log('API login failed');
    }
}

(async () => {
    const retries = 3;
    const uiLoginSuccess = await loginWithUI(retries);

    if (!uiLoginSuccess) {
        console.log('Switching to API login');
        await loginWithAPI();
    }
})();
