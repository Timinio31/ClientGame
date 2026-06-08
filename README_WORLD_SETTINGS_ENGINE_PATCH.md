# World Settings / Engine Base Patch

Dieser Patch ergänzt die aktuelle Client-Server-Basis um eine serverautoritativ geladene Weltkonfiguration.

## Ziel

Das Projekt soll nicht nur ein einzelnes Spiel abbilden, sondern als Grund-Engine dienen. Deshalb werden Welt- und Gameplay-Regeln beim Erstellen eines Serverprofils als JSON gespeichert, beim Serverstart geladen und beim ersten Client-Kontakt über `MAP_INIT` an den Client übertragen.

## Speicherorte

Client-Seite beim Speichern eines Serverprofils:

```text
~/.clientgame/saved-servers.json
~/.clientgame/world-settings/<roomId>.json
```

Server-Seite beim Start:

```text
~/.clientgame/world-settings/<roomId>.json
```

Optional kann ein anderer Pfad verwendet werden:

```bash
./gradlew :server:run --args="--room=1 --worldSettings=/path/to/world-settings.json"
```

Alternativ per Umgebungsvariable:

```text
CLIENTGAME_WORLD_SETTINGS=/path/to/world-settings.json
```

## Neue WorldSettings

Die zentrale Klasse ist:

```text
shared/src/main/java/com/tim/game/shared/config/WorldSettings.java
```

Sie enthält u. a.:

- Weltname, GameMode
- Map-Modus: `PROCEDURAL` oder `FILE`
- Map-ID, Map-Dateipfad, Breite, Höhe, TileSize, Seed
- Movement-Intervall für den Client
- Movement-Cooldown in Server-Ticks
- Feature-Flags: Movement, Interaction, Inventory, Building, Crafting, Combat, TeamCall, Companions, Groups, WorldItems, MapEditing, Settlements, StarterItems
- Limits wie MaxPlayers und MaxGroupSize

## Map-Dateien

Der Server kann generierte Maps speichern und später laden. Standardpfad für automatisch gespeicherte generierte Maps:

```text
~/.clientgame/maps/<mapId>.json
```

Wenn in `WorldSettings` Folgendes gesetzt wird:

```json
{
  "mapMode": "FILE",
  "mapFile": "C:/path/to/map.json"
}
```

dann versucht der Server diese Map zu laden. Falls die Datei fehlt oder ungültig ist, fällt er auf prozedurale Generierung zurück.

## Aktuell serverseitig aktiv erzwungen

- Movement kann deaktiviert werden.
- Movement-Speed wird über `movementCooldownTicks` serverseitig begrenzt.
- Inventory-Aktionen können deaktiviert werden.
- Building kann deaktiviert werden.
- Starter-World-Items und WorldItems können deaktiviert werden.
- Settlements können für prozedurale Maps deaktiviert werden.

## Aktuell clientseitig aktiv genutzt

- `MAP_INIT` enthält jetzt `worldSettings`.
- Client übernimmt `movementRepeatIntervalSeconds`.
- Client blockiert lokale Inventory-, Pickup-, Drop-, Use-, Slot- und Build-Eingaben, wenn die entsprechenden Features deaktiviert sind.
- Overlay zeigt Weltname, Map-ID, Map-Modus und wichtige Feature-Flags.

## Wichtig

Das ist die Engine-Basis. Crafting, Combat, TeamCall, Companions, Groups und MapEditing sind als Konfiguration bereits vorbereitet, aber die konkrete Spiellogik dafür muss noch in eigenen Systemen implementiert werden.
