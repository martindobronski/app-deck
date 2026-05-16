# App Deck

Ein Java-Swing-Programm, das wie ein Stream Deck funktioniert – mit konfigurierbaren Schaltflächen für URLs und Programme.

## Funktionen

- **5 × 3 Raster** mit 15 quadratischen Schaltflächen pro Seite
- **Mehrere Seiten** mit ◀/▶-Navigation (unten links/rechts)
- **URL-Buttons** – öffnen Webseiten im Browser, inkl. Favicon als Icon
- **Programm-Buttons** – starten macOS-Apps oder beliebige Kommandos
- **JSON-Konfiguration** – wird beim Bearbeiten automatisch gespeichert
- **Bearbeiten per Rechtsklick** – Label, Typ (URL/PROGRAM) und Ziel frei konfigurierbar
- **Label-Vorschlag** – wird automatisch aus URL-Domain oder Programmname extrahiert
- **Dateiauswahldialog** mit Tastaturnavigation (Buchstaben springen zum passenden Eintrag)
- **Drag & Drop** – Buttons durch Ziehen neu anordnen
- **Laufende Apps erkennen** – grüner Rahmen, wenn das Programm aktiv ist
- **App per Langdruck beenden** – Button > 800ms gedrückt halten → App wird beendet
- **App-Icons** – werden automatisch aus .app-Bundles extrahiert (macOS)
- **SVG-Favicons** – lokale `favicon.svg` im Verzeichnis wird als Icon verwendet
- **3D-Optik** – abgerundete Ecken, Gradient, Schlagschatten

## Voraussetzungen

- Java 21+
- macOS (für native App-Icons und `sips`-Konvertierung)

## Verwendung

```bash
mvn package -q
java -jar target/streamdeck-1.0-SNAPSHOT.jar [config.json]
```

## Konfiguration

Die Konfiguration liegt im JSON-Format:

```json
[
  [
    { "label": "GitHub", "type": "URL", "target": "https://github.com" },
    { "label": "Terminal", "type": "PROGRAM", "target": "open -a Terminal" }
  ],
  [
    { "label": "Notizen", "type": "PROGRAM", "target": "open -a Notes" }
  ]
]
```

Jede Seite ist ein Array mit bis zu 13 Buttons. Alte flache Formate (einzelnes Array) werden automatisch erkannt und auf Seiten aufgeteilt.
