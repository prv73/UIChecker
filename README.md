# UI Checker

Desktop screenshot analysis app that scores UIs on contrast, color palette, and brightness balance.

> **Submission for the Open Source Hackathon.**

Built with **Python 3.14 + PySide6 6.11.1 + Pillow** — Windows only.

## Quick start

Double-click `dist/UIChecker.exe` — no Python or dependencies required (67 MB standalone EXE).

## Features

- **Color Contrast** — samples pixels, measures local contrast ratios, flags low-contrast areas
- **Color Palette** — counts 8-bit quantized unique colors, evaluates variety and cleanliness
- **Brightness Balance** — checks ratio of light vs dark pixels across the screenshot
- **Animated UI** — gradient header, arc score meter, rounded cards with hover effects, dark theme
- **Single-file EXE** — built with PyInstaller `--onefile --windowed`, no runtime or JVM needed

## From source

```shell
pip install -r requirements.txt
python main.py
```

## Build EXE

```shell
pip install pyinstaller
pyinstaller --onefile --windowed --name UIChecker --distpath dist main.py
```

The standalone EXE will be at `dist/UIChecker.exe`.

## Tech

- Python 3.14, PySide6 6.11.1, Pillow 11.x
- PyInstaller 6.x for packaging
- Fusion style with custom QPainter widgets
- Windows only
