# LobbyEvent 1.1.0 (Paper 1.21.11)

Ein Lobby-/Event-Plugin fuer Paper 1.21.11 und Java 21.

## Ablauf

### Vor dem Eventstart

- PvP ist in allen geladenen Welten deaktiviert.
- Spieler koennen weder Bloecke abbauen noch platzieren (gilt auch fuer OPs).
- Spieler koennen normal chatten.
- Neue Spieler werden zur Lobby teleportiert.
- Der Lobby-Zustand bleibt auch nach einem Server-Neustart aktiv, solange kein Event gestartet wurde.

### Event starten

Ein OP stellt sich an die gewuenschte Event-Position und fuehrt aus:

`/event start <text>`

Beispiel:

`/event start &dDas Event beginnt jetzt!`

Daraufhin:

- wird der Event-Status gespeichert und der Lobby-Schutz aufgehoben;
- wird PvP in allen geladenen Welten aktiviert;
- sind Block-Abbau und Block-Platzieren wieder erlaubt;
- werden alle aktuell Online-Spieler zum OP teleportiert;
- sehen alle Online-Spieler den angegebenen Text als grossen Bildschirm-Titel.

Spieler, die erst nach dem Start joinen, werden nicht mehr zur Lobby teleportiert.

### Lobby erneut aktivieren

`/event reset`

Setzt den Event-Status zurueck, deaktiviert PvP, sperrt Abbau/Platzieren und teleportiert alle Online-Spieler zum gespeicherten Lobby-Punkt.

## Befehle (nur OP)

- `/event setlobby` - speichert deine aktuelle Position als Lobby.
- `/event start <text>` - startet das Event, zeigt den Text allen Spielern, teleportiert alle zu dir und schaltet PvP/Bauen frei.
- `/event reset` - schaltet den Lobby-Schutz wieder ein.
- `/event help` - zeigt die Hilfe.

Ohne gespeicherten Lobby-Punkt wird der Spawn der ersten Welt verwendet.

## Installation

1. Das Projekt in ein GitHub-Repository hochladen.
2. Den GitHub-Workflow **Build LobbyEvent Plugin** ausfuehren lassen.
3. Unter **Actions** das Artefakt `LobbyEvent-1.1.0` herunterladen.
4. Die JAR in den `plugins`-Ordner des Paper-Servers legen.
5. Den Server neu starten.

Alternativ lokal mit Java 21 und Gradle 8.14.3: `gradle build`. Die JAR liegt danach in `build/libs/`.
