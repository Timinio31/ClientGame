# Crafting Menu Patch

Dieser Patch erweitert den World-Settings-Patch um ein erstes servervalidiertes Crafting-Menü.

## Bedienung im Client

- `C` öffnet/schließt das Crafting-Menü.
- Während das Crafting-Menü offen ist, craften `1` bis `8` die sichtbaren Rezepte.
- `ESC` schließt zuerst das Crafting-Menü. Erst wenn es geschlossen ist, geht `ESC` zurück ins Hauptmenü.

## Neue World-Settings-Flags

Diese Flags können im Create-Server-Screen im Feld `Enabled feature flags CSV` ein- oder ausgeschaltet werden:

```text
crafting
craftingMenu
inventoryCrafting
workstationCrafting
toolCrafting
craftingQuality
craftingFailure
```

Wichtig:

- `crafting=false` deaktiviert Crafting vollständig.
- `craftingMenu=false` deaktiviert nur das UI/den Menüzugriff.
- `inventoryCrafting=false` deaktiviert einfache Inventarrezepte.
- `workstationCrafting=false` deaktiviert Rezepte, die Stationen wie Workbench, Schmiede oder Maschine benötigen.
- `toolCrafting=false` ist für spätere Hammer-/Werkzeugrezepte vorbereitet.
- `craftingQuality` und `craftingFailure` sind vorbereitet, aber standardmäßig aus.

## Neue Protokollnachricht

Client -> Server:

```text
MessageType.CRAFTING
```

Payload:

```json
{
  "recipeId": "ROPE_FROM_FIBERS",
  "context": "INVENTORY",
  "targetEntityId": ""
}
```

## Servervalidierung

Der Server prüft vor dem Crafting:

- Ist Inventar aktiv?
- Ist Crafting aktiv?
- Ist das Crafting-Menü aktiv?
- Ist der Rezepttyp laut Settings erlaubt?
- Existiert das Rezept?
- Ist das Rezept freigeschaltet?
- Sind alle Inputs vorhanden?
- Erfüllen Items die geforderten Kategorien?
- Ist bei Workstation-Rezepten eine passende Station in Reichweite?
- Ist genug Platz im Inventar oder dürfen überschüssige Outputs als WorldItems gedroppt werden?

## Aktuelle Beispielrezepte

- `ROPE_FROM_FIBERS`: 3x Fiber Material -> 1x Simple Rope
- `PLANKS_FROM_WOOD`: 2x Wood Material -> 4x Wooden Plank
- `PRIMITIVE_TOOL`: 2x Stone Material + 1x Wood Material -> 1x Primitive Tool
- `WORKBENCH_KIT`: 6x Wooden Plank + 1x Simple Rope -> 1x Workbench Kit
- `GENERATOR_KIT`: 4x Wooden Plank + 4x Stone -> 1x Generator Kit, benötigt Workbench in Reichweite

## Aktueller Scope

Dieser Patch ist bewusst die erste lauffähige Basis:

- Inventar-Crafting funktioniert sofort.
- Workstation-Rezepte sind im Modell und serverseitig vorbereitet.
- Das UI zeigt Workstation-Rezepte bereits an, der Server lehnt sie aber ohne passende Station in Reichweite ab.
- Crafting-Dauer ist im Rezeptmodell enthalten, wird aktuell aber noch sofort ausgeführt.
- Qualitäts- und Fehlschlaglogik sind als Settings und Rezeptfelder vorbereitet, aber noch nicht spielmechanisch umgesetzt.
- Rezeptdaten sind aktuell statisch im `CraftingCatalog`. Später kann diese Klasse durch JSON/DB-Rezepte ersetzt werden.
