# Umstellungsplan – Single Code-Base (macOS + Windows)

## Ziel

Eine Codebasis, die auf macOS und Windows läuft. Plattformspezifische Teile werden durch reine Java-APIs oder kleine `os.name`-Zweige ersetzt.

---

## Phase 1: Prozess-Erkennung (Prozessliste + App killen)

**Status:** macOS-only (osascript, ps -ef)  
**Ziel:** `java.lang.ProcessHandle` (Java 9+)

### 1.1 `pollRunningApps()` ersetzen

**Aktuell (Zeilen 566–598):**
```java
// osascript → runningApps
Process p = Runtime.getRuntime().exec(new String[]{"osascript", "-e", ...});
// ps -ef → runningCmdLines
Process p = Runtime.getRuntime().exec(new String[]{"ps", "-ef"});
```

**Neu:**
```java
private void pollRunningApps() {
    Set<String> apps = new HashSet<>();
    Set<String> cmdLines = new HashSet<>();
    ProcessHandle.allProcesses().forEach(ph -> {
        ph.info().command().ifPresent(cmd -> {
            String name = cmd.contains(File.separator)
                ? cmd.substring(cmd.lastIndexOf(File.separator) + 1)
                : cmd;
            if (name.endsWith(".exe")) name = name.substring(0, name.length() - 4);
            apps.add(name.toLowerCase());
        });
        ph.info().commandLine().ifPresent(cl -> cmdLines.add(cl.toLowerCase()));
    });
    runningApps = apps;
    runningCmdLines = cmdLines;
}
```

**Vorteil:** `ProcessHandle` ist plattformunabhängig – kein `os.name`-Zweig nötig.  
**Aufwand macOS:** 2h  
**Testbar auf macOS:** Ja (ProcessHandle liefert auch dort Prozesse)  
**Zu testen auf Windows:** Prozessnamen ohne `.exe`-Extension, Kommandozeilen-Format

### 1.2 `killApp()` ersetzen

**Aktuell (Zeilen 635–641):**
```java
new ProcessBuilder("osascript", "-e",
    "tell application \"" + name + "\" to quit").start();
```

**Neu:**
```java
private void killApp(String target) {
    String appKey = extractAppName(target).toLowerCase();
    ProcessHandle.allProcesses().forEach(ph -> {
        ph.info().command().ifPresent(cmd -> {
            String procName = cmd.contains(File.separator)
                ? cmd.substring(cmd.lastIndexOf(File.separator) + 1)
                : cmd;
            if (procName.endsWith(".exe")) procName = procName.substring(0, procName.length() - 4);
            if (procName.toLowerCase().equals(appKey)) {
                ph.destroy();
            }
        });
    });
}
```

**Vorteil:** `ProcessHandle.destroy()` ist plattformunabhängig.  
**Aufwand macOS:** 1h  
**Testbar auf macOS:** Ja  
**Zu testen auf Windows:** Berechtigungen für `destroy()`

---

## Phase 2: SVG-Rendering (Batik)

**Status:** macOS-only (sips)  
**Ziel:** `org.apache.xmlgraphics:batik-transcoder:1.18`

### 2.1 `pom.xml` – Dependency hinzufügen

```xml
<dependency>
    <groupId>org.apache.xmlgraphics</groupId>
    <artifactId>batik-transcoder</artifactId>
    <version>1.18</version>
</dependency>
```

### 2.2 `loadSvgFromDir()` ersetzen

**Aktuell (Zeilen 1491–1510):**
```java
ProcessBuilder pb = new ProcessBuilder("sips", "-s", "format", "png",
    "--resampleWidth", Integer.toString(ICON_SIZE * 2),
    svg.getAbsolutePath(), "--out", png.getAbsolutePath());
```

**Neu:**
```java
private Image loadSvgFromDir(File dir) {
    if (dir == null || !dir.isDirectory()) return null;
    File svg = new File(dir, "favicon.svg");
    if (!svg.isFile()) return null;
    try {
        String svgUri = svg.toURI().toURL().toString();
        TranscoderInput input = new TranscoderInput(svgUri);
        BufferedImageTranscoder transcoder = new BufferedImageTranscoder();
        transcoder.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH, (float)(ICON_SIZE * 2));
        transcoder.transcode(input, null);
        BufferedImage img = transcoder.getBufferedImage();
        // zwischengespeichertes PNG im tmp-Verzeichnis (optional, für schnelleren Wiederaufruf)
        return img;
    } catch (Exception e) { return null; }
}
```

**Aufwand macOS:** 3h  
**Testbar auf macOS:** Ja (Batik ist reines Java)  
**Zu testen auf Windows:** Gleicher Code, keine Änderung

---

## Phase 3: ICNS/ICO-Extraktion (Twelvemonkeys + ShellFolder)

**Status:** macOS-only (sips + .app-Bundle-Struktur)  
**Ziel:** `com.twelvemonkeys.imageio:imageio-icns:3.12.0` + bedingte ShellFolder-Nutzung

### 3.1 `pom.xml` – Dependency

```xml
<dependency>
    <groupId>com.twelvemonkeys.imageio</groupId>
    <artifactId>imageio-icns</artifactId>
    <version>3.12.0</version>
</dependency>
```

### 3.2 `extractMacAppIcon()` – ICNS via ImageIO laden

**Aktuell (Zeilen 1467–1489):** sips-Kommando  
**Neu:** `ImageIO.read(icnsFile)` – Twelvemonkeys registriert sich automatisch via ServiceLoader

```java
private Image extractMacAppIcon(File appBundle) {
    try {
        File resources = new File(appBundle, "Contents/Resources");
        if (!resources.isDirectory()) return null;
        File[] icnsFiles = resources.listFiles((dir, name) -> name.endsWith(".icns"));
        if (icnsFiles == null || icnsFiles.length == 0) return null;
        BufferedImage img = ImageIO.read(icnsFiles[0]);
        if (img != null) return img;
    } catch (Exception ignored) {}
    return null;
}
```

### 3.3 Windows: ICO aus .exe extrahieren

**Neu – nur auf Windows aktiv:**
```java
private Image extractWindowsAppIcon(File exeFile) {
    try {
        // ShellFolder-Reflection (existiert auch auf Windows)
        Class<?> sfClass = Class.forName("sun.awt.shell.ShellFolder");
        java.lang.reflect.Method getSF = sfClass.getMethod("getShellFolder", File.class);
        Object sf = getSF.invoke(null, exeFile);
        java.lang.reflect.Method getIcon = sfClass.getMethod("getIcon", boolean.class);
        return (Image) getIcon.invoke(sf, Boolean.TRUE);
    } catch (Exception ignored) {}
    // Fallback: FileSystemView
    Icon icon = FileSystemView.getFileSystemView().getSystemIcon(exeFile);
    if (icon != null) {
        BufferedImage bi = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();
        icon.paintIcon(null, g, 0, 0);
        g.dispose();
        return bi;
    }
    return null;
}
```

**Aufwand macOS:** 2h  
**Testbar auf macOS:** ICNS-Teil ja, ICO-Teil nein (wird nur auf Windows aktiv)  
**Zu testen auf Windows:** ICO-Extraktion aus .exe-Dateien

---

## Phase 4: Programm-Pfade (resolveFile)

**Status:** macOS-only (/Applications/, .app-Bundles)  
**Ziel:** `os.name`-Zweig

### 4.1 `resolveFile()` – Windows-Äste einbauen

**Aktuell (Zeilen 1587–1608):**
```java
private File resolveFile(String target) {
    // ... open -a, open "...", /Applications/..., /System/Applications/...
}
```

**Neu – Struktur:**

```java
private File resolveFile(String target) {
    // 1. Direkter Pfad – plattformneutral
    File f = new File(target);
    if (f.exists()) return f;

    // 2. open -a / open "..." – macOS-only
    if (target.startsWith("open -a ") || target.startsWith("open \"")) {
        if (!isWindows()) {
            // bestehende macOS-Logik
        }
        return null;
    }

    // 3. .app-Suffix entfernen, Programm suchen – plattformspezifisch
    String appName = ...;
    if (isWindows()) {
        return resolveWindowsProgram(appName);
    } else {
        return resolveMacProgram(appName);
    }
}

private boolean isWindows() {
    return System.getProperty("os.name").toLowerCase().contains("win");
}
```

### 4.2 `resolveWindowsProgram()`

```java
private File resolveWindowsProgram(String name) {
    // 1. %ProgramFiles% / %ProgramW6432%
    for (String pf : new String[]{
        System.getenv("ProgramFiles"),
        System.getenv("ProgramFiles(x86)"),
        System.getenv("LOCALAPPDATA") + "\\Programs"
    }) {
        if (pf == null) continue;
        File f = new File(pf, name + ".exe");
        if (f.exists()) return f;
    }
    // 2. PATH durchsuchen
    String path = System.getenv("PATH");
    if (path != null) {
        for (String dir : path.split(Pattern.quote(File.pathSeparator))) {
            File f = new File(dir, name + ".exe");
            if (f.exists()) return f;
        }
    }
    return null;
}
```

**Aufwand macOS:** 2h  
**Testbar auf macOS:** Nein (Windows-Zweig wird nicht aktiv)  
**Zu testen auf Windows:** Vollständig

---

## Phase 5: Config-Pfad + JFileChooser

**Status:** macOS-Pfade hartcodiert  
**Ziel:** `os.name`-Zweig

### 5.1 Config-Pfad

**Aktuell (Zeile 1799):**
```java
String homeCfg = System.getProperty("user.home") + "/Library/Application Support/App Deck/config.json";
```

**Neu:**
```java
String homeCfg;
if (System.getProperty("os.name").toLowerCase().contains("win")) {
    homeCfg = System.getProperty("user.home") + "\\AppData\\Roaming\\App Deck\\config.json";
} else {
    homeCfg = System.getProperty("user.home") + "/Library/Application Support/App Deck/config.json";
}
```

### 5.2 `JFileChooser`-Default

**Aktuell (Zeile 755):**
```java
JFileChooser fc = new JFileChooser("/Applications");
```

**Neu:**
```java
String defaultDir = isWindows()
    ? System.getenv("ProgramFiles")
    : "/Applications";
JFileChooser fc = new JFileChooser(defaultDir != null ? defaultDir : "/");
```

**Aufwand macOS:** 1h  
**Testbar auf macOS:** Nein  
**Zu testen auf Windows:** Vollständig

---

## Phase 6: `extractAppName()` + `parseCommand()` – Kommando-Parser

**Status:** macOS-Syntax (open -a, open "...")  
**Ziel:** Windows-Kommando-Syntax unterstützen

### 6.1 `extractAppName()` erweitern

**Aktuell (Zeilen 621–633):** Erkennt `open -a AppName` und `.app`-Endungen.

**Neu:**
```java
private String extractAppName(String target) {
    if (target.startsWith("open -a "))
        return target.substring(8).trim().replace("\\ ", " ");
    if (target.startsWith("open \"")) {
        int end = target.indexOf("\"", 6);
        if (end > 0) return new File(target.substring(6, end)).getName().replaceAll("\\.app$", "");
    }
    if (target.endsWith(".app"))
        return new File(target.replace("\\ ", " ")).getName().replaceAll("\\.app$", "");
    if (target.endsWith(".exe"))
        return new File(target).getName().replaceAll("\\.exe$", "");
    // Fallback: letzten Pfad-Bestandteil ohne Extension
    String name = target.contains(File.separator)
        ? target.substring(target.lastIndexOf(File.separator) + 1)
        : target;
    int dot = name.lastIndexOf('.');
    return dot > 0 ? name.substring(0, dot) : name;
}
```

**Aufwand macOS:** 0,5h  
**Testbar auf macOS:** Ja (Logik-Test)  
**Zu testen auf Windows:** Mit echten Windows-Pfaden

---

## Phase 7: Build-Skripte

**Status:** `build-app.sh` (macOS-only)  
**Ziel:** `build.ps1` für Windows

### 7.1 `build.ps1`

```powershell
# Build App Deck for Windows
mvn package -q
jpackage --type exe --input target --main-jar streamdeck-1.0-SNAPSHOT.jar `
    --main-class streamdeck.StreamDeckApp `
    --name "App Deck" `
    --win-menu `
    --win-shortcut `
    --win-dir-chooser `
    --icon icons/app-icon.ico
```

### 7.2 `app-icon.ico` bereitstellen

Ein 256x256 `.ico`-File (via `make-icon.sh` erzeugen, dann mit `sips` + ImageMagick oder einem Online-Konverter in `.ico` umwandeln).

**Aufwand macOS:** 1h (PowerShell-Syntax, `.ico`-Datei)  
**Testbar auf macOS:** Nein  
**Zu testen auf Windows:** Vollständig

---

## Phase 8: macOS-spezifischen Code beibehalten (keine Änderung)

Folgende Teile bleiben **unverändert** und werden durch `os.name`-Guards geschützt:

| Code | Grund |
|---|---|
| `System.setProperty("apple.awt.application.name", "App Deck")` | Wird auf Windows ignoriert |
| `Taskbar.setIconImage()` | `isTaskbarSupported()` gibt false zurück |
| `ROUNDED_BORDER` und `ROUNDED_BORDER_RUNNING` | Swing, kein OS-Code |
| Gesamte GUI (Grid, DnD, Menü, Dialoge, Suchdialog, Badges) | Reines Java2D/Swing |
| `ButtonConfig.java`, `ConfigLoader.java` | Keine Plattformabhängigkeit |

---

## Zusammenfassung der Änderungen

| Phase | Datei(en) | Aufwand | Testbar auf macOS |
|---|---|---|---|
| 1.1 pollRunningApps | StreamDeckApp.java | 2h | **Ja** |
| 1.2 killApp | StreamDeckApp.java | 1h | **Ja** |
| 2 SVG-Rendering (Batik) | pom.xml, StreamDeckApp.java | 3h | **Ja** |
| 3 ICNS/ICO-Extraktion | pom.xml, StreamDeckApp.java | 2h | **Teils** |
| 4 Programm-Pfade | StreamDeckApp.java | 2h | Nein |
| 5 Config-Pfad + JFileChooser | StreamDeckApp.java | 1h | Nein |
| 6 extractAppName + parseCommand | StreamDeckApp.java | 0,5h | **Ja** |
| 7 build.ps1 | build.ps1, app-icon.ico | 1h | Nein |
| **Gesamt** | **~8 Dateien** | **~12,5h** | **~8,5h testbar** |

## Git-Workflow

```
main: immer stabil, beide OS

feature/windows-compat
  ├── 01-process-api       (ProcessHandle umstellen)
  ├── 02-batik-svg          (Batik einbinden)
  ├── 03-icon-extraction     (Twelvemonkeys + ShellFolder)
  ├── 04-program-paths       (resolveFile Windows)
  ├── 05-config-paths        (Config + JFileChooser)
  ├── 06-command-parser      (extractAppName Windows)
  └── 07-build-scripts       (build.ps1)
```

## Testplan für Windows

1. `mvn package` → JAR baut fehlerfrei
2. App startet ohne Fehler
3. Grid, Navigation, DnD funktionieren
4. Rechtsklick-Menü (Bearbeiten, Ordner anlegen, Entfernen)
5. URL-Button öffnet Browser
6. PROGRAM-Button (direkter .exe-Pfad)
7. FILE-Button öffnet Datei
8. COPY-Button kopiert Text
9. Konfigurationsdialog öffnet und speichert
10. Laufende Programme werden erkannt (grüner Rahmen)
11. Suchdialog (Cmd+ → Strg+F auf Windows)
12. Config-Backup
