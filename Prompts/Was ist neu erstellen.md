Prompt für OpenCode

--- neu von OpenCode selbst erstellen lassen ---

Erstelle mir aus den aktuellen Quellen ein Dokument im Markdown-
und PDF-Format. Dokumentation.md und Dokumentation.pdf
Wichtig für die PDF-Erzeugung:
- Enthält das Dokument **keine Emojis** (nur Text mit Umlauten),
  dann erzeuge das PDF mit: pandoc --pdf-engine=xelatex -V lang=de
- Enthält das Dokument **Emojis/ Symbole**, dann erzeuge das PDF mit:
  1. pandoc -t html5 --embed-resources --standalone --metadata lang=de
  2. Danach das HTML mit weasyprint in PDF umwandeln
  3. Dabei folgende CSS ins <head> einfügen:
     @page { size: A4; margin: 2.5cm; }
     body { font-family: Helvetica, Arial, sans-serif; font-size: 11pt; line-height: 1.5; }
- Die Markdown-Datei immer als UTF-8 schreiben
- Vorhandene .md/.pdf Dateien überschreiben
- Nach der Erzeugung das PDF mit pdftotext auf korrekte Umlaute prüfen

Und aktualisiere die README.md auf github


--- alt: ---

Lies zuerst die aktuelle Versionsinformationen aus der Index.html  
Erstelle dann eine Datei mit dem Dateinamen
"Was ist neu in Version x vom tt.mm.jjjj.md“.  
Inhalt der Textdatei soll eine Zusammenstellung aller Änderungen des
aktuellen Tagesdatums sein.  
Erstelle zusätzlich eine Datei im PDF Format, die den gleichen Inhalt der
Markdown Datei enthält.
Falls die Datei schon existiert, aktualisiere den Inhalt und überschreibe sie.
Die Umlaute müssen korrekt dargestellt werden!
Es dürfen keine Symbole verloren gehen!

---

Erstelle mir aus den aktuellen Quellen eine Bedienungsanleitung im Markdown
und im pdf Format.
Die Dateien sollen BEDIENUNGSANLEITUNG.md und BEDIENUNGSANLEITUNG.pdf
heissen und falls bereits vorhanden, sollen sie überschrieben werden.
Es sollen alle Vorteile und Funktionalitäten ausführlich erläutert werden.
Die Umlaute müssen korrekt dargestellt werden!
Es dürfen keine Symbole verloren gehen!