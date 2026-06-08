# DS RPG Platinum Grafik-Patch

Dieses Patch-Paket ergänzt den vorhandenen `midcentury_platinum` Texture-Pack mit einem Nintendo-DS/DSi-RPG-inspirierten 2.5D-Pixelstil.

Enthalten:
- Terrain-Tiles: Gras, Wasser mit 3 Animationsframes, Wege, Wald, Berg, Spawn, Indoor, Farmland, Bibble-Zonen.
- Items: vollständige Icons für alle aktuell im `ItemCatalog` definierten V1-Items plus zusätzliche Rohstoff-/Crafting-Icons aus dem mitgegebenen Grafikpaket.
- Workstations/Blocks/Gebäude: Workbench, Storage, Terminal, Generator, Pumpen, Furnace-Varianten, Processor, Quarry, Farm, Kabel, Pipe, Walls, Fences und City-Buildings.
- Player: directional idle/walk/attack/flee/use/hurt Frames.
- Bibbles: directional idle/walk/attack/flee/hurt/capture Frames für NORMAL, FIRE, WATER, NATURE, STONE, METAL, ELECTRIC, PSYCHIC, SHADOW, LIGHT, MACHINE, TOXIC und ICE.

Technische Hinweise:
- Die Java-Änderungen erweitern den TexturePackManager um dynamisches Nachladen von Item-, Tile-, Building- und Bibble-Texturen.
- Der WorldRenderer verwendet für Bibbles jetzt Bewegungsrichtung und einfache Aktionsauswahl.
- Nach dem Kopieren den Client neu bauen/starten, damit die Ressourcen aus `src/main/resources` übernommen werden.
