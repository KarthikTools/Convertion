# Web Accessibility Checker 🎯

## What is this? 🤔

This is a special tool that helps make websites better for everyone! Just like how we make buildings accessible with ramps and elevators, websites need to be accessible too. This tool checks if a website is easy to use for people who:
- Can't see well or are blind
- Can't hear well or are deaf
- Have trouble using a mouse
- Use special tools to browse the web

## How does it work? 🛠️

1. The tool visits a website (like a robot visitor!)
2. It looks at everything on the page (pictures, buttons, text)
3. It checks if everything is easy to use
4. It writes a report about what's good and what needs to be fixed

## What do you need? 📋

- A computer with Python (like a special language for computers)
- Firefox browser (the tool uses this to visit websites)
- An OpenAI API key (this helps the tool understand websites better)

## How to use it? 🚀

1. First, install everything you need:
```bash
pip install -r requirements.txt
```

2. Create a file named `.env` and put your OpenAI API key in it:
```
OPENAI_API_KEY=your_api_key_here
```

3. Run the tool:
```bash
python accessibility_checker.py --url "https://example.com" --output-file "report.txt"
```

## What if the website needs a password? 🔑

If the website needs a password, you can tell the tool:
```bash
python accessibility_checker.py \
    --url "https://example.com/secret-page" \
    --requires-auth \
    --login-url "https://example.com/login" \
    --username "your_username" \
    --password "your_password" \
    --output-file "report.txt"
```

## What will you get? 📝

The tool will create a report that tells you:
1. Critical Issues (things that need to be fixed right away)
2. Important Issues (things that should be fixed soon)
3. Recommendations (ideas to make the website better)
4. A score (like a grade, from 0 to 100)

## Example Report 📊

```
Accessibility Report:
==================================================
Critical Issues:
- Missing alt text for images
- No keyboard navigation support

Important Issues:
- Text contrast could be better
- Form labels are missing

Recommendations:
- Add descriptions to all images
- Make sure all buttons can be clicked with a keyboard
- Use darker text colors for better readability

Overall Accessibility Score: 75/100
```

## Tips for Using the Tool 💡

1. Always check the report carefully
2. Fix the critical issues first
3. Test the website after making changes
4. Run the tool again to see if the score improves

## Need Help? 🆘

If something doesn't work:
1. Make sure Firefox is installed
2. Check if your API key is correct
3. Make sure you have all the required files
4. Try running the tool with a different website

## Remember! 🌟

Making websites accessible is important because:
- Everyone should be able to use the internet
- It's the right thing to do
- It helps more people use your website
- It makes the web better for everyone!

## License 📄

This project is free to use and share (MIT License). 