#!/bin/bash
cd "$(dirname "$0")"
PIDFILE="/tmp/app-desk.pid"

# Version aus pom.xml auslesen (zentrale Quelle)
ARTIFACT_VERSION=$(grep -m1 '<version>' pom.xml | sed 's/.*<version>\(.*\)<\/version>.*/\1/')

if [ -f "$PIDFILE" ] && kill -0 "$(cat "$PIDFILE")" 2>/dev/null; then
    osascript -e '
        tell application "System Events"
            set frontmost of (every process whose unix id is '"$(cat "$PIDFILE")"') to true
        end tell'
else
    pkill -xf "java.*streamdeck-$ARTIFACT_VERSION" 2>/dev/null || true
    java -Dapple.awt.application.name="App Deck" -jar "target/streamdeck-$ARTIFACT_VERSION.jar" &
    echo $! > "$PIDFILE"
fi
