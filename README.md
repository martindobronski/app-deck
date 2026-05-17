# App Deck

Eine in Java Swing entwickelte Desktop-Anwendung fur macOS, die als frei konfigurierbare Schaltflachen-Leiste (a la Stream Deck) fungiert.

## Features

- **6 x 4 Raster** mit 24 quadratischen Schaltflachen pro Seite
- **Mehrere Seiten** mit Navigation (vorwarts/ruckwarts)
- **URL-Buttons** – offnen Webseiten im Browser, inkl. Favicon
- **Programm-Buttons** – starten macOS-Apps oder beliebige Kommandos
- **Ordner-Buttons** – verschachtelte Unterseiten fur Gruppen
- **COPY-Buttons** – kopieren Text in die Zwischenablage
- **Datei-Buttons** – offnen Dateien und Ordner
- **Drag & Drop** – Buttons neu anordnen, in Ordner verschieben oder auf andere Seiten ziehen
- **Hover-Effekt** – blaulicher Highlight beim Uberfahren mit der Maus
- **Laufende Apps erkennen** – gruner Rahmen bei aktiven Programmen
- **App per Langdruck beenden** – >800ms gedruckt halten
- **App-Icons** – automatisch aus .app-Bundles extrahiert (macOS)
- **JSON-Konfiguration** – wird beim Bearbeiten automatisch gespeichert
- **Dunkles Design** – dunkler Hintergrund (35,35,40→50,50,55) mit hellen Buttons (248,248,250→225,225,230)
- **Eigenes App-Icon** – farbiges Raster auf dunklem Hintergrund (PNG + ICNS)
- **YouTube-Update-Prufung** – periodische Suche nach neuen Videos, "neu"-Badge, manueller Check uber Menü
- **Suchdialog** – Cmd+F durchsucht alle Seiten nach Label/Ziel, Navigation mit Pfeiltasten, blinkende Markierung
- **Type-to-Search** – Tastatureingabe bei nicht-fokussierten Textfeldern offnet Suchdialog
- **Pfeiltasten-Navigation** zwischen Buttons, Enter fuhrt Aktion aus
- **Fokus-Ring** – gold/orange gestrichelter Rahmen um fokussierten Button
- **ESC-Rucknavigation** zur vorherigen Seite bzw. Ordner-Ebene
- **Menuleiste** – App Desk (Info, YouTube-Prufung, Beenden) und Hilfe (Dokumentation)
- **Konfigurationsdialog** – linksbundige Felder mit 15px Rand, mittig zentrierte OK/Abbrechen-Buttons
- **YouTube-Erkennung** – Checkbox "Auf neue Videos prufen" erscheint nur bei YouTube-URLs und ist automatisch aktiviert
- **Logging** – `appdeck.log` mit Zeitstempeln, Rotation bei 5 MB

## Voraussetzungen

- Java 21+
- macOS (fur native App-Icons, `sips`, `osascript`)

## Verwendung

```bash
mvn package -q
java -jar target/streamdeck-1.0-SNAPSHOT.jar [config.json]
```

**macOS .app Bundle:**

```bash
./build-app.sh
open "App Deck.app"
```

## Konfiguration

Die Konfiguration liegt im JSON-Format (Mehrseiten-Array):

```json
[
  [
    { "label": "GitHub", "type": "URL", "target": "https://github.com" },
    { "label": "Terminal", "type": "PROGRAM", "target": "open -a Terminal" },
    { "label": "Notizen", "type": "PROGRAM", "target": "open -a Notes" }
  ],
  [
    { "label": "Passwort", "type": "COPY", "target": "meinPasswort123" },
    { "label": "Projekt", "type": "FOLDER", "target": "", "pages": [
      [ { "label": "Build", "type": "URL", "target": "https://jenkins.example.com" } ]
    ]}
  ]
]
```

Jede Seite ist ein Array mit bis zu 22 konfigurierbaren Buttons (+ 2 Navigationsbuttons). Alte flache Formate werden automatisch erkannt und migriert.

## Weitere Informationen

Die ausfuhrliche technische Dokumentation befindet sich in `Dokumentation.md` / `Dokumentation.pdf`.
