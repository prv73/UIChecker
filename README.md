# UI Checker

A cross-platform desktop app that analyzes screenshot images for color contrast, palette diversity, and brightness balance — scoring each dimension out of 100 with a detailed breakdown.

> **Submission for the Open Source Hackathon.**

## Quick start

```shell
mvn\bin\mvn package -DskipTests
java --enable-native-access=ALL-UNNAMED -jar target/ui-checker-1.0.0.jar
```

## Portable Build (no Java required)

```shell
build-portable.bat
```

Output: `dist/UIChecker/UIChecker.exe` (~270 MB) – portable folder with bundled JRE. Double-click the EXE to launch; no installation or Java needed.

Under the hood, `jpackage` + `jlink` creates a minimal JRE (~77 MB) and wraps the fat JAR with a native launcher.

## Features

- **Screenshot Analysis** – samples pixels, measures local contrast ratio, counts quantized unique colors, checks brightness balance.
- **Monochrome dark theme** – pure dark-mode Swing UI with animated score meters, hover effects, and a vertical sidebar layout.
- **Cross-platform** – runs on Windows and Linux.

## Notes

- The `mvn/` directory contains a portable Maven 3.9.9 distribution.
