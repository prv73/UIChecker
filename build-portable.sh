#!/usr/bin/env bash
set -e
export MAVEN_OPTS="--enable-native-access=ALL-UNNAMED"

echo "============================================"
echo " UI Checker - Portable Build"
echo "============================================"
echo ""

# Find JDK
JAVA_HOME=""
if [ -d /usr/lib/jvm/zulu-25 ]; then
  JAVA_HOME=/usr/lib/jvm/zulu-25
elif [ -d /usr/lib/jvm/java-25-openjdk ]; then
  JAVA_HOME=/usr/lib/jvm/java-25-openjdk
elif [ -d /usr/lib/jvm/jdk-25 ]; then
  JAVA_HOME=/usr/lib/jvm/jdk-25
else
  JAVA_CMD=$(which java 2>/dev/null || true)
  if [ -n "$JAVA_CMD" ]; then
    JAVA_HOME=$(dirname "$(dirname "$JAVA_CMD")")
  else
    echo "ERROR: Java not found. Install Zulu JDK 25 or later."
    exit 1
  fi
fi
echo "Using JDK: $JAVA_HOME"

# Step 1: Build fat JAR
echo "[1/3] Building fat JAR with Maven..."
./mvn/bin/mvn package -q -DskipTests
echo "      Fat JAR created: target/ui-checker-1.0.0.jar"

# Step 2: Create input directory
echo "[2/3] Preparing input for jpackage..."
rm -rf dist-input
mkdir -p dist-input
cp target/ui-checker-1.0.0.jar dist-input/

# Step 3: Build portable EXE with jpackage
echo "[3/3] Building portable launcher with jpackage..."
rm -rf dist

"$JAVA_HOME/bin/jpackage" \
  --type app-image \
  --input dist-input \
  --main-jar ui-checker-1.0.0.jar \
  --main-class uichecker.App \
  --name UIChecker \
  --add-modules java.base,java.compiler,java.desktop,java.sql \
  --dest dist \
  --java-options "--enable-native-access=ALL-UNNAMED" \
  --java-options "-Dawt.useSystemAAFontSettings=on" \
  --java-options "-Dswing.aatext=true" \


rm -rf dist-input

echo ""
echo "============================================"
echo " Portable build complete!"
echo " Location: dist/UIChecker/"
echo " Launcher: dist/UIChecker/bin/UIChecker"
echo " Size:     ~270 MB (includes bundled Java runtime)"
echo "============================================"
echo ""
echo " NOTE: On first URL analysis, Playwright will"
echo " download Chromium (~150 MB) automatically."
echo ""
echo " To distribute, tar/zip the entire dist/UIChecker/ folder."
echo ""
