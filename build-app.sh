#!/bin/bash
# Build App Deck macOS .app bundle
# Requires: maven, jpackage (JDK 16+), swiftc (Xcode)

set -e

cd "$(dirname "$0")"

APP_NAME="App Deck"
BUNDLE_ID="com.dobronski.appdeck"
ICON="icons/app-icon.icns"

# Version aus pom.xml auslesen (zentrale Quelle)
ARTIFACT_VERSION=$(grep -m1 '<version>' pom.xml | sed 's/.*<version>\(.*\)<\/version>.*/\1/')

echo "=== Building JAR (Version: $ARTIFACT_VERSION) ==="
mvn clean package -q

echo "=== Building native floating helper ==="
swiftc -o float_helper float_helper.swift -framework Cocoa
chmod +x float_helper

echo "=== Creating .app bundle ==="
rm -rf dist/
cp Dokumentation.pdf Bedienungsanleitung.pdf target/
jpackage \
  --type app-image \
  --name "$APP_NAME" \
  --app-version "$ARTIFACT_VERSION" \
  --icon "$ICON" \
  --input target/ \
  --main-jar "streamdeck-$ARTIFACT_VERSION.jar" \
  --main-class streamdeck.StreamDeckApp \
  --mac-package-identifier "$BUNDLE_ID" \
  --dest dist/

echo "=== Adding resources ==="
cp float_helper "dist/$APP_NAME.app/Contents/Resources/"
cp icons/app-icon.png "dist/$APP_NAME.app/Contents/Resources/" 2>/dev/null || true

echo "=== Done ==="
echo "App Bundle: dist/$APP_NAME.app"
echo ""
echo "Zum Starten: open \"dist/$APP_NAME.app\""
rm -rf "$APP_NAME.app" 2>/dev/null
cp -r "dist/$APP_NAME.app" .
echo "Kopie im Projektroot: $APP_NAME.app"

echo "=== Done ==="
echo "App Bundle: dist/$APP_NAME.app"
echo ""
echo "Zum Starten: open \"dist/$APP_NAME.app\""
rm -rf "$APP_NAME.app" 2>/dev/null
cp -r "dist/$APP_NAME.app" .
echo "Kopie im Projektroot: $APP_NAME.app"
