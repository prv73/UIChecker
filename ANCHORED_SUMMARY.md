# UI Checker - Anchored Summary

## Goal
- Build a cross-platform desktop UI checking app in Java that analyzes URLs and screenshots, scores them out of 100, and has a modern animated Swing interface.

## Constraints & Preferences
- Java only (Zulu JDK 25), no Python or Node.js
- Maven for build (bundled locally at `C:\uichecker\mvn\`)
- Playwright for Java (v1.49.0) for URL browser automation
- Gson for JSON (v2.11.0)
- Pure Swing desktop app – no localhost server, no browser dependency
- Cross-platform: Windows + Linux (same fat JAR, font fallback via `FONT` constant)
- Monochrome dark theme (black/dark grays replacing dark blues)
- UI must be modern: gradients, hover animations, animated score meters, rounded cards, vertical sidebar layout
- File selection uses native AWT `FileDialog` (modern Windows dialog)
- Submission for the Open Source Hackathon

## Progress
### Done
- Rewrote entire app in Java 25 with Maven build system, shade plugin for fat JAR
- Built 5 check modules (readability, accessibility, contrast, layout, responsiveness) using Playwright DOM extraction + WCAG color math
- Built screenshot upload analyzer using `BufferedImage` – samples pixels, measures contrast ratio, counts quantized unique colors, checks brightness balance
- Built pure Swing desktop UI with custom-painted components (no HTML served)
- Added: gradient header with animated accent line, sidebar layout with URL + Screenshot sections merged in one card, rounded inputs with gray focus glow, animated score meter, hover-elevated category cards, hover-animated buttons as `JButton` subclasses, detail rows with checkmark/cross icons and suggestion text, performance metrics panel
- Replaced horizontal tab bar with vertical sidebar; separated from main content by a 1px painted border
- Added 16px top padding to body panel to push sidebar down from header
- Added screenshot preview in results for screenshot file analysis (Base64-encoded in `ScreenshotAnalyzer`)
- Changed color palette from dark blue to monochrome (BG `#080808`, SURFACE `#111`, CARD `#1a1a1a`, BORDER `#2e2e2e`, TEXT `#f0f0f0`, gray category colors)
- Added Linux font fallback: `FONT` constant uses `"SansSerif"` on Linux, `"Segoe UI"` on Windows
- Attempted Windows dark title bar via `sun.awt.windows.darkMode` system property
- Initialized Git repo with `.gitignore` (ignores `target/`, `lib/`, `uploads/`, IDE files, `dist/`), `README.md` (build/run/hackathon submission/portable build)
- Added `run.bat` and `run.sh` one-command build+run scripts with error handling
- Updated all 5 check modules to include `suggestion` field in detail maps with actionable improvement advice
- Added "Show Example" button in sidebar that displays a 96/100 perfect-score template
- Added `suggestionsCard` that aggregates all suggestions from failed detail rows into a dedicated card
- Added tooltip on detail row icons showing the suggestion text on hover
- Added `exampleTemplate()` method returning a realistic 96/100 result map hitting all 5 categories
- Added `detail()` helper in App.java for consistent map creation with nullable suggestion
- **Portable build system**: `build-portable.bat` uses JDK's `jpackage` + `jlink` to create a self-contained Windows installer EXE (~216 MB) with bundled JRE (java.base, java.desktop, java.compiler, java.sql – ~77 MB) and all required JVM flags (`--enable-native-access=ALL-UNNAMED`, font AA, OpenGL)
- WiX Toolset 3.14 installed via `winget` for `--type exe` installer generation
- Fallback to `--type app-image` portable folder if WiX is not available

### In Progress
- (none)

### Blocked
- (none)

## Key Decisions
- Used `javax.swing.Timer` explicitly (fully qualified) to avoid ambiguity with `java.util.Timer`
- Replaced `JFileChooser` with AWT `FileDialog` for native Windows modern file picker
- `RoundedButton` changed from `JPanel`+`MouseAdapter` to `JButton`+`ActionListener` for reliable click handling
- URL and Screenshot sections merged into one card with a horizontal divider instead of two separate cards
- Git repo initialized; all changes committed locally; pushed to GitHub (`prv73/UIChecker`)
- Used `GIT_EDITOR=true` to avoid Vim during rebase in this environment
- Suggestions shown inline as tooltips on detail row icons AND aggregated in a dedicated card at the bottom of results
- Portable distribution via `jpackage --type exe` (installer) or `--type app-image` (portable folder)
- `jlink` strips debug, headers, man pages, native commands for minimal runtime size (~77 MB)
- JVM options baked into the EXE launcher: native access, font AA, Swing AA, OpenGL acceleration

## Next Steps
- Test the installer EXE: double-click `dist/UIChecker-1.0.exe`
- Verify Launcher creates Start Menu entry and launches app
- Push changes to GitHub
- For Linux portable: use `jpackage --type app-image --add-modules ...`

## Critical Context
- Playwright Java v1.49.0: `WaitUntilState` enum uses `NETWORKIDLE` (no underscore)
- `Page.NavigateOptions.setTimeout()` expects `double` (seconds), not `int`
- Playwright browsers must be installed once (`playwright install chromium`); downloaded on first `Playwright.create()`
- Check modules return `suggestion` field (nullable string) in each detail map
- `ContrastCheck` detail method signature changed: `detail(boolean pass, String label, String detail, String suggestion)`
- `ScreenshotAnalyzer.analyze()` -> `detail(pass, label, detail, suggestion)` also has suggestion parameter
- `App.java` has its own `detail()` helper for the example template
- **jpackage** available in JDK 25 at `C:\Program Files\Zulu\zulu-25\bin\jpackage`
- **jlink** modules used: `java.base,java.compiler,java.desktop,java.sql`
- **WiX Toolset** required for `--type exe`, installed at `C:\Program Files (x86)\WiX Toolset v3.14\bin`
- The fat JAR is at `C:\uichecker\target\ui-checker-1.0.0.jar`
- JAR excludes `lib/gson-2.11.0.jar` (Maven fetches it) – `lib/` is gitignored

## Relevant Files
- `C:\uichecker\pom.xml` – Maven build, deps: playwright 1.49.0, gson 2.11.0, shade plugin for fat jar
- `C:\uichecker\src\main\java\uichecker\App.java` – Main class: JFrame, custom-painted monochrome UI, sidebar layout, Playwright analysis orchestration
- `C:\uichecker\src\main\java\uichecker\checks\ReadabilityCheck.java` – Font sizes, line heights, heading hierarchy (max 20 pts, now with suggestions)
- `C:\uichecker\src\main\java\uichecker\checks\AccessibilityCheck.java` – Alt text, ARIA labels, semantic HTML, lang, doctype (max 20 pts, now with suggestions)
- `C:\uichecker\src\main\java\uichecker\checks\ContrastCheck.java` – WCAG relative luminance / contrast ratio, palette size (max 20 pts, now with suggestions)
- `C:\uichecker\src\main\java\uichecker\checks\LayoutCheck.java` – Content density, body children, heading count vs section count (max 20 pts, now with suggestions)
- `C:\uichecker\src\main\java\uichecker\checks\ResponsivenessCheck.java` – Viewport meta, media queries, image sizes, load time, page size (max 20 pts, now with suggestions)
- `C:\uichecker\src\main\java\uichecker\screenshot\ScreenshotAnalyzer.java` – Image decoding, pixel sampling, quantized color counting, local contrast averaging, brightness check (now includes Base64-encoded screenshot)
- `C:\uichecker\build-portable.bat` – Builds portable EXE installer or app image (auto-detects WiX)
- `C:\uichecker\build-portable.sh` – Linux portable build script (creates app-image)
- `C:\uichecker\run.bat` / `run.sh` – One-command build+run scripts
- `C:\uichecker\README.md` – Build/run/prerequisites/hackathon submission/portable build
- `C:\uichecker\.gitignore` – Ignores `target/`, `lib/`, `uploads/`, IDE/OS files, `dist/`, `dist-input/`
