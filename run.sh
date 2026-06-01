#!/usr/bin/env bash
set -e
export MAVEN_OPTS="--enable-native-access=ALL-UNNAMED"
echo "Building..."
mvn/bin/mvn package -q -DskipTests
echo "Starting UI Checker..."
exec java --enable-native-access=ALL-UNNAMED -jar target/ui-checker-1.0.0.jar
