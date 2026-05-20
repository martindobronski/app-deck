# App Deck

A Java Swing desktop application for macOS that serves as a freely configurable button launcher (a la Stream Deck).

## Features

- **6 x 4 grid** with 24 square buttons (120x120 px) per page
- **Multiple pages** with forward/backward navigation
- **URL buttons** - open websites in browser, including favicons
- **Program buttons** - launch macOS apps or any commands
- **Folder buttons** - nested sub-pages for grouping
- **COPY buttons** - copy text to clipboard
- **File buttons** - open files and folders
- **Drag & Drop** - rearrange buttons, move into folders or to other pages
- **Hover effect** - blue highlight on mouse-over
- **Running app detection** - green border for active programs
- **Long-press to quit** - hold >800ms to terminate an app
- **App icons** - auto-extracted from .app bundles (macOS)
- **JSON configuration** - auto-saved on edit
- **Dark design** - dark gradient background with bright buttons
- **YouTube update check** - periodic new video detection with badge
- **Search dialog** - Cmd+F to search all pages, arrow key navigation, blinking highlight
- **Type-to-search** - keyboard input in non-focused areas opens search
- **Arrow key navigation** between buttons, Enter to execute
- **Focus ring** - gold/orange dashed border on focused button
- **ESC back-navigation** to previous page or folder
- **Menu bar** - App Desk (About, YouTube check, Save config, Toggle focus mode, Quit) and Help (Documentation)
- **Focus mode** - Cmd+Shift+F to cover screen with dark background
- **Backup config** - timestamped copies in `bak/` directory
- **Context menu** (right-click) - Edit, Create folder, Remove, Mark as new
- **Logging** - `appdeck.log` with timestamps, rotation at 5 MB

## Requirements

- Java 21+
- macOS (native features: `osascript`, `sips`, `open`, `jpackage`)

## Quick Start

```bash
git clone git@github.com:martindobronski/app-deck.git
cd app-deck
./build-app.sh
open "App Deck.app"
```

Or manually:

```bash
mvn package -q
java -jar target/streamdeck-1.4.jar [config.json]
```

## Configuration

JSON array-of-arrays (multi-page format):

```json
[
  [
    { "label": "GitHub", "type": "URL", "target": "https://github.com" },
    { "label": "Terminal", "type": "PROGRAM", "target": "open -a Terminal" },
    { "label": "Password", "type": "COPY", "target": "myPassword123" },
    { "label": "Project", "type": "FOLDER", "target": "", "pages": [
      [ { "label": "Build", "type": "URL", "target": "https://jenkins.example.com" } ]
    ]}
  ],
  [
    { "label": "Notes", "type": "FILE", "target": "/path/to/notes.txt" }
  ]
]
```

Config path fallback: current directory -> next to .app bundle -> `~/Library/Application Support/App Deck/config.json`

## Build .app Bundle

```bash
./build-app.sh
```

Uses Maven (compile + shade) and jpackage to create a native macOS `.app` bundle.

## Documentation

Full technical documentation (German):
- `Dokumentation.md` - Markdown
- `Dokumentation.pdf` - PDF

## Project Status

Version 1.4 - May 20, 2026

- macOS-only (planned: cross-platform single codebase)
- Built with Java 21 + Swing + Gson 2.11.0
- MIT-style proprietary license
