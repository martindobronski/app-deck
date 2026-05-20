Wie wird app deck.app über intellij erstellt?

In IntelliJ gibt es zwei Wege:
1. Über das Maven-Toolfenster (rechts):
- 
Lifecycle → package → baut target/streamdeck-1.2.jar
- 
Dann im Terminal: bash build-app.sh (das macht beides: package + jpackage)
2. Als Run Configuration (ein Klick):
- 
Run → Edit Configurations → + → Shell Script
- 
Script path: build-app.sh
- 
Working directory: /Users/martin/dev/java/StreamDeck
- 
Speichern, dann grüner Play-Button ⇒ baut Jar und .app in einem Schritt

---

Nach Neuladen des Projekts in IntelliJ erscheint oben rechts
"Build App Deck" im Run-Menü – ein Klick und es baut Jar + .app in einem Schritt.
Hinweis: .idea/ ist in .gitignore – die Config wird nicht ins Repository
committed. Jeder Entwickler müsste sie sich lokal anlegen
(oder ich nehme sie aus .gitignore raus, wenn du sie teilen willst).