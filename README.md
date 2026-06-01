# UI Checker

A cross-platform desktop application that analyzes UIs for readability, accessibility, color contrast, layout quality, and responsiveness.

Scans URLs via Playwright (browser automation) or analyzes uploaded screenshot images, then scores each dimension out of 100 with detailed breakdowns.

> **Submission for the Open Source Hackathon.**

## Prerequisites

- **Java 25**
- **Playwright browsers** – run once after building:
  ```
  java -jar target/ui-checker-1.0.0.jar
  ```
  If prompted, or run manually:
  ```
  playwright install chromium
  ```

## Build

A Maven distribution is bundled at `mvn/` – no system Maven needed.

```shell
mvn\bin\mvn clean package -DskipTests
```

Or with a system-wide Maven:

```shell
mvn clean package -DskipTests
```

Output: `target/ui-checker-1.0.0.jar` (~200 MB, includes Playwright driver binaries for Windows and Linux).

## Run

```shell
java --enable-native-access=ALL-UNNAMED -jar target/ui-checker-1.0.0.jar
```

## Features

- **URL Analysis** – launches headless Chromium via Playwright, extracts DOM and styles, then scores readability (font sizes, heading hierarchy), accessibility (alt text, ARIA labels, semantic HTML), color contrast (WCAG relative luminance), layout (content density, structure), and responsiveness (viewport, media queries, resource metrics).
- **Screenshot Analysis** – samples pixels, measures local contrast ratio, counts quantized unique colors, checks brightness balance.
- **Monochrome dark theme** – pure dark-mode Swing UI with animated score meters, hover effects, and a vertical sidebar layout.
- **Cross-platform** – runs on Windows and Linux.

## Notes

- The `mvn/` directory contains a portable Maven 3.9.9 distribution.
- Playwright browser binaries are downloaded separately per platform (not bundled in the JAR).
