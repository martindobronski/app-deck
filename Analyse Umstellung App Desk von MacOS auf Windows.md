# Analyse macOS → Windows Kompatibilität

## 1. Nicht plattformneutral (muss ersetzt werden)

### 1.1 `osascript` – Prozessliste abfragen

**Datei:** `StreamDeckApp.java` | **Zeilen:** 569–582

```java
Process p = Runtime.getRuntime().exec(new String[]{
    "osascript", "-e",
    "tell application \"System Events\" to get name of every process"
});
```

**Problem:** macOS-only (AppleScript).  
**Windows-Lösung:** `tasklist.exe /NH /FO CSV` oder `ProcessHandle.allProcesses()` (Java 9+).

**Priorität:** HIGH

---

### 1.2 `osascript` – App per Long-Press beenden

**Datei:** `StreamDeckApp.java` | **Zeilen:** 635–641

```java
new ProcessBuilder("osascript", "-e",
    "tell application \"" + name + "\" to quit").start();
```

**Problem:** macOS-only (AppleScript).  
**Windows-Lösung:** `taskkill /IM <name.exe> /F`.

**Priorität:** HIGH

---

### 1.3 `ps -ef` – Prozess-Kommandozeilen abfragen

**Datei:** `StreamDeckApp.java` | **Zeile:** 586

```java
Process p = Runtime.getRuntime().exec(new String[]{"ps", "-ef"});
```

**Problem:** Unix-only.  
**Windows-Lösung:** `ProcessHandle.allProcesses()` (Java 9+) oder `wmic process get CommandLine`.

**Priorität:** HIGH

---

### 1.4 `sips` – .icns → PNG konvertieren

**Datei:** `StreamDeckApp.java` | **Zeilen:** 1467–1489

```java
ProcessBuilder pb = new ProcessBuilder("sips", "-s", "format", "png",
    icnsFile.getAbsolutePath(), "--out", pngFile.getAbsolutePath());
```

**Problem:** macOS-only (Bildverarbeitungs-Tool).  
**Windows-Lösung:** Twelvemonkeys ICNS ImageIO-Plugin (`com.twelvemonkeys.imageio:imageio-icns`) oder ShellFolder-Reflection.

**Priorität:** HIGH

---

### 1.5 `sips` – SVG → PNG konvertieren

**Datei:** `StreamDeckApp.java` | **Zeilen:** 1491–1510

```java
ProcessBuilder pb = new ProcessBuilder("sips", "-s", "format", "png",
    "--resampleWidth", Integer.toString(ICON_SIZE * 2),
    svg.getAbsolutePath(), "--out", png.getAbsolutePath());
```

**Problem:** macOS-only.  
**Windows-Lösung:** Batik oder JFreeSVG als Java-SVG-Renderer.

**Priorität:** HIGH

---

### 1.6 `resolveFile()` – macOS-Pfade für Programme

**Datei:** `StreamDeckApp.java` | **Zeilen:** 1587–1608

```java
f = new File("/Applications/" + appName + ".app");
f = new File("/System/Applications/" + appName + ".app");
```

**Problem:** Nur macOS-Pfade (`/Applications/`, `.app`-Bundle-Struktur).  
**Windows-Lösung:** `%ProgramFiles%`, `%LOCALAPPDATA%\Programs`, `%PATH%`, `.exe`-Extension.

**Priorität:** HIGH

---

### 1.7 `extractAppName()` – `open -a`-Syntax

**Datei:** `StreamDeckApp.java` | **Zeilen:** 621–633

```java
if (target.startsWith("open -a "))
    return target.substring(8).trim().replace("\\ ", " ");
```

**Problem:** Geht von macOS-`open`-Befehl aus.  
**Windows-Lösung:** Direkte `.exe`-Namen oder `start`-Befehl parsen.

**Priorität:** HIGH

---

### 1.8 `pollRunningApps()` – gesamte Methode

**Datei:** `StreamDeckApp.java` | **Zeilen:** 566–598

**Problem:** Nutzt `osascript` + `ps -ef` – beides macOS/Unix-only.  
**Windows-Lösung:** Komplett auf `ProcessHandle.allProcesses()` (Java 9+) umstellen.

**Priorität:** HIGH

---

### 1.9 Config-Pfad – `~/Library/Application Support/`

**Datei:** `StreamDeckApp.java` | **Zeile:** 1799

```java
String homeCfg = System.getProperty("user.home")
    + "/Library/Application Support/App Deck/config.json";
```

**Problem:** macOS-spezifischer Pfad.  
**Windows-Lösung:** `System.getProperty("user.home") + "\\AppData\\Roaming\\App Deck\\config.json"`.

**Priorität:** MEDIUM

---

### 1.10 `JFileChooser("/Applications")`

**Datei:** `StreamDeckApp.java` | **Zeile:** 755

```java
JFileChooser fc = new JFileChooser("/Applications");
```

**Problem:** macOS-Verzeichnis.  
**Windows-Lösung:** `System.getenv("ProgramFiles")` oder `C:\\Program Files`.

**Priorität:** MEDIUM

---

### 1.11 `parseCommand()` – naiver Parser

**Datei:** `StreamDeckApp.java` | **Zeilen:** 1393–1405

**Problem:** Geht von Unix-Kommando-Syntax aus (keine Windows-Pfade mit Laufwerkbuchstaben, keine `.exe`).  
**Windows-Lösung:** Robusten Parser schreiben, der `cmd /c`, `start`, und `\"`-Windows-Zitierung versteht.

**Priorität:** MEDIUM

---

### 1.12 `killApp()` – gesamte Methode

**Datei:** `StreamDeckApp.java` | **Zeilen:** 635–641

**Problem:** Nutzt `osascript`.  
**Windows-Lösung:** `taskkill /IM <name.exe> /F` oder `ProcessHandle.onExit()`.

**Priorität:** HIGH

---

### 1.13 `build-app.sh` – macOS `.app`-Bundle

**Datei:** `build-app.sh` | **Zeilen:** 17, 21, 25, 31–33

```bash
jpackage --type app-image --icon icons/app-icon.icns --mac-package-identifier ...
open "dist/App Deck.app"
```

**Problem:** jpackage mit macOS-Flags, `.icns`-Icon, `open`-Befehl.  
**Windows-Lösung:** Eigenes `build.ps1` mit `--type exe`, `--win-menu`, `--win-shortcut`, `.ico`-Datei.

**Priorität:** HIGH (nur für Build-Prozess)

---

### 1.14 `make-icon.sh` – Icon-Generierung

**Datei:** `icons/make-icon.sh` | **Zeilen:** 73–82

```bash
sips -z 1024 1023 ... && iconutil -c icns ...
```

**Problem:** Nutzt `sips` und `iconutil` (beide macOS-only).  
**Windows-Lösung:** `.ico` statisch bereitstellen oder mit ImageIO + `ico`-Plugin generieren.

**Priorität:** HIGH (nur für Build-Prozess)

---

## 2. Plattformneutral (funktioniert auch auf Windows)

| Code                                | Datei/Zeile                  | Grund                                                                      |
| ----------------------------------- | ---------------------------- | -------------------------------------------------------------------------- |
| `Taskbar.setIconImage()`            | StreamDeckApp.java:81–82     | `isTaskbarSupported()` gibt false zurück → wird ignoriert                  |
| `System.setProperty("apple.awt.*")` | StreamDeckApp.java:32        | Wird auf Windows ignoriert (kein Fehler)                                   |
| `Desktop.getDesktop().open()`       | StreamDeckApp.java:147       | Cross-Plattform API (Java AWT)                                             |
| `Desktop.getDesktop().browse()`     | StreamDeckApp.java:1194      | Cross-Plattform API                                                        |
| `FileSystemView.getSystemIcon()`    | StreamDeckApp.java:1454      | Cross-Plattform (Swing)                                                    |
| `ShellFolder` (Reflection)          | StreamDeckApp.java:1579–1583 | Klasse existiert auch auf Windows (anderer interner Name)                  |
| `parseCommand()` (Grundlogik)       | StreamDeckApp.java:1393–1405 | Naives Splitting funktioniert; nur Kommando-Strings sind plattformabhängig |

---

## 3. Unverändert (keine Anpassung nötig)

| Datei                                                                                                              | Grund                                                   |
| ------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------- |
| `ButtonConfig.java`                                                                                                | Reines Datenmodell (POJO), keine Plattform-Abhängigkeit |
| `ConfigLoader.java`                                                                                                | Reines JSON-I/O mit Gson, keine Plattform-Abhängigkeit  |
| `pom.xml`                                                                                                          | Gson-Dependency ist Multi-Plattform                     |
| `README.md`                                                                                                        | Nur Dokumentation                                       |
| Gesamte GUI (Grid-Layout, Drag & Drop, Menüs, Suchdialog, Fokusring, Badge-Zeichnung, Hover-Effekte, Farbverläufe) | Pure Swing/Java2D – kein einziger OS-Aufruf             |

---

## 4. Priorisierte Umsetzungs-Reihenfolge

### Phase 1 – Grundfunktionalität (Minimal)

1. OS-Erkennung einbauen: `System.getProperty("os.name")`
2. `pollRunningApps()` auf `ProcessHandle.allProcesses()` umstellen
3. `killApp()` auf `taskkill` umstellen (Windows) / `osascript` (macOS)
4. `resolveFile()` um Windows-Pfade erweitern

### Phase 2 – Icons & Programme

5. SVG-Rendering durch Batik ersetzen (`loadSvgFromDir()`)
6. ICNS-Erkennung durch ICO-Erkennung ergänzen (`extractMacAppIcon()` → plattformspezifisch)
7. Config-Pfad plattformabhängig machen
8. `JFileChooser`-Default-Verzeichnis plattformabhängig machen

### Phase 3 – Build & Qualität

9. `build-app.sh` durch `build.ps1` ergänzen
10. `.ico`-Datei für Windows bereitstellen
11. Dokumentation aktualisieren
12. Testen unter Windows

---

## 5. Neue Abhängigkeiten (für Windows)

| Library                | Zweck                                  | Maven-Koordinate                                |
| ---------------------- | -------------------------------------- | ----------------------------------------------- |
| **Batik**              | SVG-Rendering (ersetzt `sips`)         | `org.apache.xmlgraphics:batik-transcoder:1.18`  |
| **Twelvemonkeys ICNS** | ICNS-Dekodierung                       | `com.twelvemonkeys.imageio:imageio-icns:3.12.0` |
| **JNA** (optional)     | Native Windows-Aufrufe (Process, Icon) | `net.java.dev.jna:jna-platform:5.15.0`          |
