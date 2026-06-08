# Chunk Streaming / Height Collision Hotfix

## Zweck

Dieser Hotfix korrigiert zwei Probleme aus dem ersten Layered-Map-Patch:

1. **Unsichtbare Barrieren** auf eigentlich sichtbaren/laufbaren Terrain-Tiles.
2. **Zu spätes Chunk-Laden**, sodass am Rand der Kamera teilweise noch keine Tiles vorhanden waren.

## Änderungen

### Server: `WorldState.java`

Die Bewegung blockiert nicht mehr wegen `heightLevel`-Unterschieden. `heightLevel` bleibt als Map-Metadatum erhalten, wird aber nicht mehr als unsichtbare Kollisionskante verwendet.

Blockierend bleiben weiterhin:

- nicht laufbare Tiles (`walkable=false`), z. B. Wasser, Lava, Klippen, Map-Rand
- belegte Tiles durch Gebäude/Objekte (`occupiedTiles`)
- Ziele außerhalb der Map

Damit sind die Höhen aktuell vorbereitet, erzeugen aber keine unsichtbaren Wände mehr. Echte Rampen/Klippen sollten später mit sichtbaren Übergangstiles eingeführt werden.

### Client: `GameScreen.java`

Das Chunk-Laden wurde von einem groben Bereichsrequest auf einzelne Chunk-Requests mit Statusverwaltung umgestellt:

- `loadedChunks`: Chunks, die wirklich angekommen sind
- `pendingChunks`: Chunks, die angefragt wurden, aber noch nicht angekommen sind
- Preload-Radius: 3 Chunks um den sichtbaren Bereich
- Maximal 24 neue Chunk-Requests pro Frame

Dadurch werden Chunks früher nachgeladen und nicht mehr fälschlich als geladen behandelt, nur weil sie einmal angefragt wurden.

### Client: `WorldRenderer.java`

Die visuelle Höhenverschiebung normaler Tiles wurde deaktiviert. Der Renderer bleibt top-down/flach, solange keine sichtbaren Rampen-/Klippen-Sprites existieren. Dadurch entstehen keine sichtbaren Linien oder scheinbaren Kanten auf Grasflächen.

## Installation

ZIP im Projektwurzelordner entpacken, z. B.:

```text
C:\Users\tim.zeleznik\ClientGame
```

Danach:

```powershell
.\gradlew.bat clean
.\gradlew.bat :server:run
.\gradlew.bat :client:run
```
