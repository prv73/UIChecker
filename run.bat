@echo off
echo Building...
call mvn\bin\mvn.cmd package -q -DskipTests
if %errorlevel% neq 0 (
    echo Build failed.
    pause
    exit /b %errorlevel%
)
echo Starting UI Checker...
java --enable-native-access=ALL-UNNAMED -jar target\ui-checker-1.0.0.jar
if %errorlevel% neq 0 (
    echo Failed to launch. Make sure Java 25 is installed.
    pause
)
