# Layered Map Generation Patch

Dieser Patch ersetzt die einfache Random-Map durch ein erweiterbares Layer-System für eine große, endliche, aber chunkbasiert gestreamte Welt.

## Enthalten

- Seedbasierte prozedurale Map-Generierung.
- Layer für Height, Temperatur, Feuchtigkeit, Biome, Terrain, Ressourcen und Flüsse.
- Neue Biome: Grasland, Wald, Wüste, Sumpf, Schnee, Magma, Gebirge, Strand/Küste, Flüsse und Starter-/City-Zonen.
- Höhenlevel pro Tile. Der Spieler darf natürliche Höhenunterschiede von maximal 1 Level laufen.
- Chunkbasierte Übertragung zum Client über `MAP_CHUNK_REQUEST` und `MAP_CHUNK`.
- Client rendert nur noch sichtbare Tiles plus Rand und cached geladene Chunks.
- Ressourcen können direkt auf Tiles liegen und beim Harvesting verbraucht werden.
- Siedlungen werden aus mehreren Zentren generiert und per A*-ähnlichem Road-Pathfinding verbunden.

## Wichtige Einstellungen in `WorldSettings`

- `mapWidth`, `mapHeight`: jetzt bis 8192 normalisiert.
- `chunkStreamingEnabled`: aktiviert chunkbasierte Map-Übertragung.
- `chunkSize`: Standard 32, erlaubt 8 bis 128.
- `mapInitFullTileLimit`: kleine Maps können weiterhin komplett im MAP_INIT übertragen werden.
- `maxHeightLevel`: vorbereitet für spätere feinere Höhenlogik.

## Hinweis zu sehr großen Welten

Der Client bekommt große Maps nicht mehr vollständig im `MAP_INIT`, sondern lädt sichtbare Chunks nach. Der Server generiert die Map aktuell weiterhin beim Start als vollständigen In-Memory-Zustand. Für echte 8048x8048-Welten sollte als nächster Schritt ein packed/chunk-backed Server-Speicher statt `MapTile[][]` umgesetzt werden.
