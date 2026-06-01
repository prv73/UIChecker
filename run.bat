@echo off
mvn\bin\mvn package -q -DskipTests 2>nul
java --enable-native-access=ALL-UNNAMED -jar target\ui-checker-1.0.0.jar
