# ClientGame Texture Pack Patch

Dieser Patch ergänzt clientseitig ein Texture-Pack-System.

## Inhalt

- Settings-Menü mit Texture-Pack-Auswahl
- persistierte Client-Settings unter `~/.clientgame/client-settings.json`
- externe Texture-Packs unter `~/.clientgame/texture-packs`
- eingebautes Pack `1950 Civilized Cartoon`
- texturiertes Rendering für Tiles, Spieler und Gebäude
- F5 im Spiel lädt das aktive Texture-Pack neu

## Externes Texture-Pack-Format

Ordnerstruktur:

```text
~/.clientgame/texture-packs/my_pack/
├── manifest.json
├── tiles/
│   ├── grass.png
│   ├── water.png
│   ├── wall.png
│   └── spawn.png
└── entities/
    ├── player_local.png
    ├── player_remote.png
    ├── building_generator.png
    └── building_default.png
```

Manifest:

```json
{
  "id": "my_pack",
  "displayName": "My Pack",
  "description": "Beschreibung des Texture-Packs."
}
```

Empfohlene Größe: 32x32 px, PNG, transparente Hintergründe bei Figuren/Gebäuden.
