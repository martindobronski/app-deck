#!/bin/bash
# Build App Deck macOS .app bundle
# Requires: maven, jpackage (JDK 16+)

set -e

cd "$(dirname "$0")"

APP_NAME="App Deck"
BUNDLE_ID="com.dobronski.appdeck"
ICON="icons/app-icon.icns"

echo "=== Building JAR ==="
mvn package -q

echo "=== Creating .app bundle ==="
rm -rf dist/
cp Dokumentation.pdf Bedienungsanleitung.pdf target/
jpackage \
  --type app-image \
  --name "$APP_NAME" \
  --app-version "1.5" \
  --input target/ \
  --main-jar streamdeck-1.5.jar \
  --main-class streamdeck.StreamDeckApp \
  --mac-package-identifier "$BUNDLE_ID" \
  --dest dist/

echo "=== Done ==="
echo "App Bundle: dist/$APP_NAME.app"
echo ""
echo "Zum Starten: open \"dist/$APP_NAME.app\""
rm -rf "$APP_NAME.app" 2>/dev/null
cp -r "dist/$APP_NAME.app" .
echo "Kopie im Projektroot: $APP_NAME.app"
