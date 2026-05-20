# Bedienungsanleitung App Deck

Version 1.4 vom 20.05.2026

---

## 1. Einleitung

**App Deck** ist eine Desktop-Anwendung für macOS, die als frei konfigurierbare Schaltflächen-Leiste fungiert ähnlich einem Stream Deck. Sie ermöglicht das schnelle Öffnen von Webseiten, Programmen, Dateien und Ordnern sowie das Kopieren von Text in die Zwischenablage.

Die Anwendung richtet sich an alle, die häufig die gleichen Programme, Webseiten oder Dateien öffnen und dabei Zeit sparen möchten. Durch die übersichtliche Anordnung in einem Raster mit mehreren Seiten und Ordnern behältst du stets den Überblick.

### Vorteile auf einen Blick

- **Zeitersparnis**: Häufig genutzte Aktionen mit einem Klick erreichbar
- **Übersichtlichkeit**: Beliebig viele Seiten und Ordner zur Organisation
- **Flexibilität**: Fünf verschiedene Schaltflächen-Typen für unterschiedliche Aufgaben
- **Automatisierung**: YouTube-Update-Prüfung informiert über neue Videos
- **Individualisierung**: Frei konfigurierbare Buttons mit eigenen Icons
- **macOS-Integration**: Native Unterstützung für macOS-Apps und -Funktionen

---

## 2. Installation und Start

### 2.1 Voraussetzungen

- macOS (getestet ab macOS 13 Ventura)
- Java 21 oder hoher (bei Verwendung des .app-Bundles nicht erforderlich)

### 2.2 Installation als .app-Bundle (empfohlen)

1. Lade die aktuellste Version von App Deck herunter
2. Entpacke die ZIP-Datei
3. Verschiebe `App Deck.app` in das Programme-Verzeichnis
4. Beim ersten Start wirst du gefragt, ob du eine vorhandene Konfiguration auswählen oder eine leere neu erstellen möchtest

### 2.3 Starten über die Kommandozeile

```bash
java -jar streamdeck-1.4.jar [pfad/zur/config.json]
```

### 2.4 Konfigurationsdatei

Die Konfiguration wird automatisch an folgender Stelle gespeichert (in dieser Reihenfolge):

1. Aktuelles Arbeitsverzeichnis (`config.json`)
2. Neben der `.app`-Datei
3. `~/Library/Application Support/App Deck/config.json`

---

## 3. Die Benutzeroberfläche

### 3.1 Aufbau

Das Hauptfenster zeigt ein **6 x 4 Raster** aus 24 quadratischen Schaltflächen (120 x 120 Pixel). Jede Schaltfläche kann eine Aktion ausführen (Webseite öffnen, Programm starten, Text kopieren usw.).

- **Oben**: Menüleiste mit den Menüs "App Desk" und "Hilfe"
- **Mitte**: Das Schaltflächen-Raster
- **Unten links**: Seitenzahl (z.B. "Seite 1/3")
- **Unten links/rechts**: Navigationspfeile zum Blättern zwischen Seiten

### 3.2 Menüleiste

**App Desk (Alt + A):**

- Über App Desk: Zeigt Versionsinformationen an
- Suche (Cmd + F): Durchsucht alle Seiten und Ordner
- Auf neue YouTube-Videos prüfen (Cmd + Y): Manuelle YouTube-Prüfung
- Konfiguration sichern (Cmd + B): Erstellt ein Backup der Konfiguration
- Fokus-Modus umschalten (Cmd + Shift + F): Dunkler Hintergrundmodus
- Beenden (Cmd + Q): Schließt die Anwendung

**Hilfe (Alt + H):**

- Dokumentation anzeigen (Cmd + D): Öffnet dieses Handbuch

### 3.3 Navigation

- **Pfeiltasten (links/rechts)**: Zwischen Seiten blättern
- **Pfeiltasten (oben/unten/links/rechts)**: Zwischen Schaltflächen navigieren
- **Eingabetaste**: Die fokussierte Schaltfläche ausführen
- **ESC**: Eine Seite zurück, Ordner verlassen oder Fokus-Modus beenden

---

## 4. Schaltflächen-Typen

### 4.1 URL

Öffnet eine Webseite im Standard-Browser. Das Favicon der Webseite wird automatisch als Icon geladen.

**Beispiele:**

- `https://www.google.com`
- `https://github.com`

**Automatische Erkennung:** Wenn du eine URL eingibst, wird der Typ automatisch auf "URL" gesetzt. Der Anzeigename wird aus der Domain abgeleitet (z.B. "Google" für google.com).

**Vorteil:** Kein manuelles Öffnen des Browsers und Eintippen der Adresse mehr.

### 4.2 PROGRAM

Startet ein Programm auf deinem Mac.

**Beispiele:**

- `open -a Terminal`
- `open -a Safari`
- `open "/Applications/Safari.app"`

**Automatische Erkennung:** Wenn der Pfad auf `.app` endet oder mit `open -a` beginnt, wird der Typ auf "PROGRAM" gesetzt. Das Programm-Icon wird automatisch aus dem .app-Bundle extrahiert.

**Laufende Apps erkennen:** Programme mit einem grünen Rahmen sind gerade aktiv. Du kannst sie per Langdruck (>800ms) beenden.

**Vorteil:** Schneller Zugriff auf alle installierten Programme ohne über dass Dock oder Launchpad zu navigieren.

### 4.3 FILE

Öffnet eine Datei oder einen Ordner mit der Standard-Anwendung.

**Beispiele:**

- `/Users/deinname/Dokumente/Notizen.txt`
- `/Users/deinname/Downloads`

**Automatische Erkennung:** Wenn dass eingegebene Ziel eine existierende Datei oder ein Ordner ist, wird der Typ automatisch auf "FILE" gesetzt.

**Vorteil:** Schneller Zugriff auf häufig genutzte Dateien und Ordner.

### 4.4 FOLDER

Erzeugt einen Ordner-Button, der eine eigene Seite mit weiteren Schaltflächen enthält. Dies ermöglicht eine hierarchische Struktur.

**Besonderheiten:**

- Beim Klicken auf einen Ordner erscheint die Unterseite mit eigenen Schaltflächen
- Der Rückwärts-Pfeil links unten fuhrt zurück zur Hauptebene
- Ordner können beliebig viele Unterseiten haben
- Neue Videos innerhalb eines Ordners werden zusammengezählt und als "neu: X" angezeigt

**Vorteil:** Sinnvolle Strukturierung auch bei vielen Schaltflächen, z.B. nach Kategorien wie "Arbeit", "Privat", "Medien".

### 4.5 COPY

Kopiert einen beliebigen Text in die Zwischenablage. Nach dem Klick erscheint kurz "Kopiert!" auf dem Button.

**Beispiele:**

- Passwörter
- E-Mail-Adressen
- Häufig verwendete Textbausteine
- Code-Snippets

**Vorteil:** Kein manuelles Markieren und Kopieren mehr. Ideal für häufig benötigte Textfragmente.

---

## 5. Schaltflächen konfigurieren

### 5.1 Neuen Button erstellen

Klicke auf eine leere Schaltfläche. Es öffnet sich der Konfigurationsdialog.

### 5.2 Bestehenden Button bearbeiten

Rechtsklick auf die Schaltfläche und "Bearbeiten..." auswählen.

### 5.3 Der Konfigurationsdialog

Der Dialog enthält folgende Felder:

**Label:** Der Anzeigename der Schaltfläche (wird mittig angezeigt)

**Typ:** Auswahl zwischen URL, PROGRAM, FILE, FOLDER und COPY

**Ziel:** Der Pfad, die URL oder der Text, der ausgeführt werden soll

- Bei URL: Die vollständige Webadresse
- Bei PROGRAM: Der Befehl zum Starten (z.B. `open -a Terminal`)
- Bei FILE: Der Pfad zur Datei oder zum Ordner
- Bei FOLDER: Nicht benötigt (Ziel bleibt leer)
- Bei COPY: Der Text, der kopiert werden soll (mehrzeilig möglich)

Der **Browse-Button** (...) öffnet einen Dateiauswahl-Dialog. Dieser unterstützt Type-Ahead: Einfach die Anfangsbuchstaben des gesuchten Programms tippen.

**Auf neue Videos prüfen:** Diese Checkbox erscheint nur bei YouTube-URLs. Ist sie aktiviert, prüft App Deck regelmäßig nach neuen Videos und zeigt ein "Neu"-Badge an.

### 5.4 Automatische Typ-Erkennung

App Deck erkennt den Typ automatisch beim Eintippen des Ziels:

| Eingabe                           | Erkannter Typ | Automatischer Label    |
| --------------------------------- | ------------- | ---------------------- |
| `https://www.youtube.com/@Kanal`  | URL           | "Kanal" (aus @-Handle) |
| `open -a Terminal`                | PROGRAM       | "Terminal"             |
| `open "/Applications/Safari.app"` | PROGRAM       | "Safari"               |
| `Pfad/zur/Datei.pdf` (existiert)  | FILE          | "Datei.pdf"            |
| Beliebiger Text                  | COPY          | (manuell eingeben)     |

---

## 6. Schaltflächen verwalten

### 6.1 Verschieben per Drag & Drop

Ziehe eine Schaltfläche mit der Maus an eine ändere Position:

- **Auf eine ändere Schaltfläche ziehen**: Die beiden tauschen die Platze
- **Auf den Pfeil nach rechts ziehen**: Verschiebt den Button auf die nächste Seite
- **Auf den Pfeil nach links ziehen**: Verschiebt den Button auf die vorherige Seite
- **Auf einen Ordner ziehen**: Verschiebt den Button in den Ordner

### 6.2 Kontextmenü (Rechtsklick)

- **Bearbeiten...**: Öffnet den Konfigurationsdialog
- **Ordner anlegen**: Erzeugt einen neuen Ordner-Button an dieser Position
- **Entfernen**: Löscht die Schaltfläche
- **Als neu markieren**: Setzt dass "Neu"-Badge manuell (z.B. um sich an einen noch nicht angesehenen Inhalt zu erinnern)

### 6.3 Programm per Langdruck beenden

Halte einen Programm-Button länger als 800ms gedruckt. Das Programm wird dann beendet. Der grüne Rahmen (laufende App) verschwindet sofort und erscheint für 12 Sekunden nicht wieder, selbst wenn das Programm noch lauft.

---

## 7. Fokus-Modus (Cmd + Shift + F)

Der Fokus-Modus verdunkelt den gesamten Bildschirm hinter dem App-Deck-Fenster. So kannst du dich ganz auf die Schaltflächen konzentrieren, ohne von anderen Fenstern oder dem Desktop-Hintergrund abgelenkt zu werden.

- **Aktivieren/Deaktivieren**: Cmd + Shift + F oder Menüpunkt "App Desk > Fokus-Modus umschalten"
- **Beenden**: Nochmals Cmd + Shift + F oder ESC
- Die Einstellung wird automatisch in der Konfiguration gespeichert und beim nächsten Start wiederhergestellt
- Klicks auf den dunklen Hintergrund haben keine Auswirkung

**Vorteil:** Ungestörtes Arbeiten, besonders geeignet für Präsentationen oder konzentriertes Arbeiten.

---

## 8. Suchfunktion (Cmd + F)

Die Suche durchsucht alle Seiten und Ordner nach dem Label oder Ziel einer Schaltfläche.

**Bedienung:**

1. Cmd + F drücken oder einfach loszutippen (bei nicht-fokussiertem Textfeld)
2. Suchbegriff eingeben - die Ergebnisse werden sofort gefiltert
3. Mit den Pfeiltasten durch die Ergebnisse navigieren
4. Enter oder Doppelklick springt zur Schaltfläche
5. Der gefundene Button blinkt 5 Sekunden lang gelb

**Anzeige:** Das Suchergebnis zeigt den Ort der Schaltfläche an, z.B. "S.2 > YouTube > S.1/5" (Seite 2, Ordner YouTube, Unterseite 1, Position 5).

**Vorteil:** Auch bei vielen Seiten und Ordnern findest du jede Schaltfläche in Sekunden.

---

## 9. YouTube-Update-Prüfung

### 9.1 Automatische Prüfung

App Deck prüft regelmäßig (alle 5 Minuten), ob auf den konfigurierten YouTube-Kanalen neue Videos erschienen sind. Die erste Prüfung erfolgt 10 Sekunden nach dem Start.

### 9.2 Das "Neu"-Badge

Wenn ein neues Video gefunden wird, erscheint auf der Schaltfläche ein goldenes Badge mit der Anzahl neuer Videos, z.B. "neu: 3".

- Bei Ordnern wird die Anzahl aller neuen Videos zusammengezählt
- Das Badge verschwindet, sobald du die Schaltfläche klickst

### 9.3 Manuelle Prüfung

Cmd + Y oder Menüpunkt "App Desk > Auf neue YouTube-Videos prüfen"

### 9.4 Aktivierung

Die YouTube-Prüfung muss für jede Schaltfläche einzeln aktiviert werden:

1. Schaltfläche bearbeiten (Rechtsklick > Bearbeiten...)
2. Die Checkbox "Auf neue Videos prüfen" aktivieren
3. Diese erscheint automatisch, wenn die URL youtube.com oder youtu.be enthält

**Vorteil:** Du verpasst keine neuen Videos deiner Lieblingskanäle mehr, ohne dafür YouTube manuell besuchen zu müssen.

---

## 10. Tastaturkürzel (Übersicht)

| Kurzel          | Aktion                                             |
| --------------- | -------------------------------------------------- |
| Cmd + F         | Suchdialog öffnen                                  |
| Cmd + Y         | YouTube-Prüfung starten                            |
| Cmd + B         | Konfiguration sichern (Backup)                     |
| Cmd + Shift + F | Fokus-Modus umschalten                             |
| Cmd + D         | Dokumentation anzeigen                             |
| Cmd + Q         | Anwendung beenden                                  |
| Pfeiltasten     | Zwischen Buttons navigieren                        |
| Eingabetaste    | Fokussierten Button ausführen                      |
| ESC             | Rückwärts / Ordner verlassen / Fokus-Modus beenden |

---

## 11. Tipps für den Alltag

### 11.1 Arbeitsplatz einrichten

Erstelle für verschiedene Aufgabenbereiche eigene Seiten:

- **Seite 1**: Täglich genutzte Programme (Browser, Mail, Terminal)
- **Seite 2**: Wichtige Webseiten (GitHub, Jira, Slack)
- **Seite 3**: Dateien und Ordner (Projektordner, Notizen)
- **Seite 4**: YouTube-Kanale, die du verfolgen möchtest

### 11.2 Mit Ordnern strukturieren

Lege Ordner für größere Kategorien an:

- Ordner "Entwicklung" mit Unterseiten für verschiedene Projekte
- Ordner "Medien" mit Unterseiten für Musik, Videos, Podcasts
- Ordner "Social Media" mit Links zu allen Plattformen

### 11.3 Backup nicht vergessen

Sichere regelmäßig deine Konfiguration über den Menüpunkt "Konfiguration sichern" (Cmd + B). Die Backups werden im Ordner `bak/` neben der Konfigurationsdatei abgelegt und mit Zeitstempel versehen.

### 11.4 COPY-Buttons clever nutzen

Nutze COPY-Buttons für:

- Deine E-Mail-Adresse
- Telefonnummern
- FAQ-Antworten
- Wiederkehrende Code-Snippets
- Länge URLs, die du teilen möchtest

---

## 12. Fehlerbehebung

### 12.1 App Deck startet nicht

Stelle sicher, dass Java 21 oder hoher installiert ist:

```bash
java -version
```

### 12.2 YouTube-Prüfung funktioniert nicht

Die YouTube-Prüfung kann fehlschlagen, wenn YouTube Consent-Redirects ausliefert. In diesem Fall:

- Prüfe die Internetverbindung
- Stelle sicher, dass die URL korrekt ist (muss auf `/videos` enden)
- Die Prüfung erfolgt automatisch erneut

### 12.3 Icons werden nicht angezeigt

Icons werden asynchron geladen. Bei langsamer Internetverbindung kann es etwas dauern, bis Favicons erscheinen. Die Icons werden gecached und beim nächsten Start sofort angezeigt.

### 12.4 Konfiguration wiederherstellen

Wenn die Konfiguration beschädigt ist:

1. Öffne den Ordner `~/Library/Application Support/App Deck/`
2. Dort findest du einen Unterordner `bak/` mit zeitgestempelten Backups
3. Kopiere dass gewünschte Backup zurück nach `config.json`
4. Starte App Deck neu

---

## 13. Datenschutz und Logging

App Deck erstellt eine Logdatei (`appdeck.log`) im selben Verzeichnis wie die Konfigurationsdatei. Diese enthält:

- Start- und Endzeitpunkte der Anwendung
- Informationen über gefundene neue YouTube-Videos
- Zusammenfassung der YouTube-Prüfung

Die Logdatei wird bei 5 MB automatisch rotiert (die letzten 9 Versionen bleiben erhalten).

Es werden keine personenbezogenen Daten an Dritte übermittelt. Die Verbindung zu YouTube dient ausschließlich der Prüfung auf neue Videos und erfolgt direkt von deinem Rechner.

---

## 14. Versionshistorie

| Version | Datum      | Änderungen                                         |
| ------- | ---------- | -------------------------------------------------- |
| 1.4     | 20.05.2026 | Fokus-Modus, JWindow-Fix, focusMode in config.json |
| 1.3     | 18.05.2026 | Config-Auswahldialog bei Erststart                 |
| 1.2     | 17.05.2026 | Drag & Drop, Ordner, COPY-Typ, YouTube-Prüfung     |
| 1.1     | 17.05.2026 | Icon-Ladung, laufende Apps, Suchdialog             |
| 1.0     | 16.05.2026 | Erste Version mit 5x3 Raster, URL/PROGRAM/FOLDER   |
