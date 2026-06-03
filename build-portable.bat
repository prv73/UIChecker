@echo off
setlocal enabledelayedexpansion
set MAVEN_OPTS=--enable-native-access=ALL-UNNAMED

echo ============================================
echo  UI Checker - Portable Build
echo ============================================
echo.

REM Find JDK (check common locations)
set JAVA_HOME=
if exist "C:\Program Files\Zulu\zulu-25" set JAVA_HOME=C:\Program Files\Zulu\zulu-25
if exist "C:\Program Files\Eclipse Adoptium\jdk-25" set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25
if exist "C:\Program Files\Java\jdk-25" set JAVA_HOME=C:\Program Files\Java\jdk-25
if exist "C:\Program Files\Amazon Corretto\jdk1.25" set JAVA_HOME=C:\Program Files\Amazon Corretto\jdk1.25

if not defined JAVA_HOME (
    for /f "tokens=*" %%i in ('where java 2^>nul') do (
        set "JAVA_CMD=%%i"
        goto :found_java
    )
    echo ERROR: Java not found. Install Zulu JDK 25 or later.
    pause
    exit /b 1
)
:found_java
if defined JAVA_CMD (
    for %%i in ("!JAVA_CMD!") do set JAVA_HOME=%%~dpi..
)
echo Using JDK: !JAVA_HOME!

REM Step 1: Build fat JAR
echo [1/3] Building fat JAR with Maven...
call mvn\bin\mvn.cmd package -q -DskipTests
if %errorlevel% neq 0 (
    echo Build failed.
    pause
    exit /b %errorlevel%
)
echo       Fat JAR created: target\ui-checker-1.0.0.jar

REM Step 2: Create input directory
echo [2/3] Preparing input for jpackage...
if exist dist-input rmdir /s /q dist-input
mkdir dist-input
copy target\ui-checker-1.0.0.jar dist-input\ >nul

REM Step 3: Find WiX for installer creation
echo [3/3] Building installer EXE with jpackage...
set WIX_DIR=
if exist "C:\Program Files (x86)\WiX Toolset v3.14\bin" set WIX_DIR=C:\Program Files (x86)\WiX Toolset v3.14\bin
if exist "C:\Program Files (x86)\WiX Toolset v3.11\bin" set WIX_DIR=C:\Program Files (x86)\WiX Toolset v3.11\bin
if exist "C:\Program Files\WiX Toolset v3.14\bin" set WIX_DIR=C:\Program Files\WiX Toolset v3.14\bin

set JPACKAGE_TYPE=app-image

set JPACKAGE="!JAVA_HOME!\bin\jpackage"
if exist dist rmdir /s /q dist

%JPACKAGE% --type %JPACKAGE_TYPE% --input dist-input --main-jar ui-checker-1.0.0.jar --main-class uichecker.App --name UIChecker --add-modules java.base,java.compiler,java.desktop,java.sql --dest dist --java-options "--enable-native-access=ALL-UNNAMED" --java-options "-Dawt.useSystemAAFontSettings=on" --java-options "-Dswing.aatext=true" --java-options "-Dsun.java2d.opengl=true"

if %errorlevel% neq 0 (
    echo jpackage failed. See error above.
    pause
    exit /b %errorlevel%
)

REM Cleanup
if exist dist-input rmdir /s /q dist-input

echo.
echo ============================================
if /I "%JPACKAGE_TYPE%"=="exe" (
    dir /b dist\*.exe 2>nul && for %%f in (dist\*.exe) do (
        echo  Installer EXE: dist\%%~nxf
        echo  Size:           %%~zf bytes (%~zf / 1MB = %%~zf / 1048576 MB)
    )
    echo.
    echo  Double-click UIChecker-1.0.exe to install.
) else (
    echo  Portable folder: dist\UIChecker\
    echo  EXE:             dist\UIChecker\UIChecker.exe
    echo  Size:            ~270 MB (includes bundled Java runtime)
    echo.
    echo  Double-click UIChecker.exe to launch.
)
echo ============================================
echo.
echo  NOTE: On first URL analysis, Playwright will
echo  download Chromium (~150 MB) automatically.
echo.
echo  To distribute, share the entire dist\ folder.
echo.
pause
