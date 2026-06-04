# UI Checker

Desktop app that analyzes screenshot images for color contrast, palette diversity, and brightness balance.

> **Submission for the Open Source Hackathon.**

## Quick start

Double-click `dist/UIChecker.exe` — no install, no Python needed.

## From source

```shell
pip install -r requirements.txt
python main.py
```

## Features

- **Screenshot Analysis** – samples pixels, measures local contrast ratio, counts quantized unique colors, checks brightness balance.
- **Monochrome dark theme** – dark-mode PySide6 UI with animated score meters, hover effects, and sidebar layout.
- **Windows only** – built and tested on Windows.
