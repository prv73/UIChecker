# UI Checker

A cross-platform desktop application that analyzes UIs for readability, accessibility, color contrast, layout quality, and responsiveness.

Scans URLs via Playwright (browser automation) or analyzes uploaded screenshot images, then scores each dimension out of 100 with detailed breakdowns.

> **Submission for the Open Source Hackathon.**

## Quick start

```shell
mvn\bin\mvn package -DskipTests
java --enable-native-access=ALL-UNNAMED -jar target/ui-checker-1.0.0.jar
```

The first run builds the JAR and launches the app. If Playwright browsers are missing, install them with `playwright install chromium`.

## Portable Build (no Java required)

```shell
build-portable.bat
```

Output: `dist/UIChecker/UIChecker.exe` (~270 MB) – portable folder with bundled JRE. Double-click the EXE to launch; no installation or Java needed.

Under the hood, `jpackage` + `jlink` creates a minimal JRE (~77 MB) and wraps the fat JAR with a native launcher. On first URL analysis, Playwright downloads Chromium (~150 MB) automatically.

## Features

- **URL Analysis** – launches headless Chromium via Playwright, extracts DOM and styles, then scores readability (font sizes, heading hierarchy), accessibility (alt text, ARIA labels, semantic HTML), color contrast (WCAG relative luminance), layout (content density, structure), and responsiveness (viewport, media queries, resource metrics).
- **Screenshot Analysis** – samples pixels, measures local contrast ratio, counts quantized unique colors, checks brightness balance.
- **Monochrome dark theme** – pure dark-mode Swing UI with animated score meters, hover effects, and a vertical sidebar layout.
- **Cross-platform** – runs on Windows and Linux.

## Notes

- The `mvn/` directory contains a portable Maven 3.9.9 distribution.
- Playwright browser binaries are downloaded separately per platform (not bundled in the JAR).
