#!/usr/bin/env bash
mvn package -q -DskipTests 2>/dev/null
exec java --enable-native-access=ALL-UNNAMED -jar target/ui-checker-1.0.0.jar
