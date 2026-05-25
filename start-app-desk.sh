#!/bin/bash
cd "$(dirname "$0")"
PIDFILE="/tmp/app-desk.pid"

if [ -f "$PIDFILE" ] && kill -0 "$(cat "$PIDFILE")" 2>/dev/null; then
    osascript -e '
        tell application "System Events"
            set frontmost of (every process whose unix id is '"$(cat "$PIDFILE")"') to true
        end tell'
else
    java -jar target/streamdeck-1.7.jar &
    echo $! > "$PIDFILE"
fi