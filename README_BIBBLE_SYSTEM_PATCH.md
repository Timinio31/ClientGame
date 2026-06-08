# Bibble System Patch

This patch adds the first server-authoritative Bibble foundation.

## Implemented now

- WorldSettings flags for enabling/freezing Bibbles.
- Shared Bibble model:
  - BibbleType
  - BibblePersonality
  - BibbleLifecycleState
  - BibbleCommandType
  - BibbleStats
  - BibbleAttack
  - BibbleStateDto
  - BibbleInventoryDto
  - BibbleTypeMatrix
  - BibbleCatalog
- Server-side Bibble ownership through BibbleManager and BibbleInventory.
- Each player can have up to three active Bibbles.
- Existing/future stored Bibbles belong to a terminal/global storage list.
- If Bibbles are disabled through settings, existing Bibbles are frozen rather than deleted.
- One starter Bibble is created for a player when Bibbles and starterBibbles are enabled.
- Active Bibbles can follow, stay, patrol, be recalled to terminal storage, or be assigned to a nearby workstation placeholder.
- Bibbles are included in WorldSnapshotDto and rendered by the client.
- A first Bibble inventory/detail menu is available in-game.

## Client controls

- B: open/close Bibble inventory
- 1-3 while Bibble menu is open: toggle active slot between active team and terminal storage
- F: all active Bibbles follow
- S: all active Bibbles stay
- P: all active Bibbles patrol
- R: recall active Bibbles to terminal storage
- W: assign one active Bibble to the nearest workstation placeholder
- ESC: close open Bibble/Crafting menu or return to menu

Keybinding customization is not implemented yet. It should be moved into player settings later.

## New world setting flags

The Create Server screen accepts these additional CSV flags:

```text
bibbles,bibbleSpawning,bibbleCapturing,bibbleTrading,bibbleWorkstationAutomation,bibbleCombat,bibbleFriendlyFire,bibblePermanentDeath,bibbleFreeRoam,bibbleTerminal,bibbleAreas,starterBibbles
```

Default behavior:

- bibbles enabled
- starter Bibble enabled
- max active Bibbles: 3
- terminal storage prepared
- workstation automation prepared
- Bibble combat flags prepared, not fully simulated yet
- permanent death disabled by default

## Important architecture choice

Bibbles are not normal inventory items. They are separate owned entities with their own id, lifecycle state, stats, type, personality, bond values and attacks. The player inventory only references Bibbles through BibbleInventoryDto.

This is required for later capture, trading, workstations, fenced zones, combat, breeding and ownership rules.

