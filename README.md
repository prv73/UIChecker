# UI Checker

A cross-platform desktop application that analyzes UIs for readability, accessibility, color contrast, layout quality, and responsiveness.

Scans URLs via Playwright (browser automation) or analyzes uploaded screenshot images, then scores each dimension out of 100 with detailed breakdowns.

> **Submission for the Open Source Hackathon.**

## Quick start

```shell
# Windows
run.bat

# Linux
./run.sh
```

The first run auto-builds the JAR, then launches the app. If Playwright browsers are missing, follow the prompt to install them.

## Prerequisites

- **Java 25**
- **Playwright browsers** – if not auto-prompted, install manually:
  ```
  playwright install chromium
  ```

## Build & Run (Development)

A Maven distribution is bundled at `mvn/` – no system Maven needed.

```shell
mvn\bin\mvn clean package -DskipTests
java --enable-native-access=ALL-UNNAMED -jar target/ui-checker-1.0.0.jar
```

Or use the convenience scripts:

```shell
# Windows
run.bat

# Linux
./run.sh
```

Output: `target/ui-checker-1.0.0.jar` (~200 MB, includes Playwright driver binaries for Windows and Linux).

## Portable Build (no Java required to run)

Build a standalone Windows installer with a bundled Java runtime using `jpackage` (included in JDK 25):

```shell
# Windows (requires WiX Toolset for single-file EXE installer)
build-portable.bat
```

**Output (with WiX installed):** `dist/UIChecker-1.0.exe` (~216 MB) – a single-file installer EXE. Double-click to install; no Java installation needed.

**Output (without WiX):** `dist/UIChecker/UIChecker.exe` (~270 MB) – a portable folder with the bundled JRE and launcher EXE.

**How it works:** `jpackage` + `jlink` creates a minimal JRE (java.base, java.desktop, java.compiler, java.sql – ~77 MB) and wraps the fat JAR with a native launcher. The app works offline once built.

> **Note:** On first URL analysis, Playwright downloads Chromium (~150 MB) automatically to `~/.cache/ms-playwright/`.
>
> **WiX Toolset** is required for the single-file EXE installer. Install with: `winget install WiXToolset.WiXToolset`

## Features

- **URL Analysis** – launches headless Chromium via Playwright, extracts DOM and styles, then scores readability (font sizes, heading hierarchy), accessibility (alt text, ARIA labels, semantic HTML), color contrast (WCAG relative luminance), layout (content density, structure), and responsiveness (viewport, media queries, resource metrics).
- **Screenshot Analysis** – samples pixels, measures local contrast ratio, counts quantized unique colors, checks brightness balance.
- **Monochrome dark theme** – pure dark-mode Swing UI with animated score meters, hover effects, and a vertical sidebar layout.
- **Cross-platform** – runs on Windows and Linux.

## Notes

- The `mvn/` directory contains a portable Maven 3.9.9 distribution.
- Playwright browser binaries are downloaded separately per platform (not bundled in the JAR).
