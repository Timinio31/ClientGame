# Bibble TODO / Future Systems

## Capture items and capture score

Implement capture items as normal items with capture modifiers. The server should calculate a capture score from:

- base Bibble difficulty
- current health
- status effects
- trap quality
- capture item quality
- approach direction, for example back-attack bonus
- Bibble personality
- fear/love scalar
- connection after partial domestication
- region/biome modifiers
- special skills or mutations

Each Bibble should stay catchable, but stronger Bibbles should require much better conditions.

## Capture failure behavior

On failed capture, Bibbles can:

- flee using a flee attack/effect
- become aggressive
- receive attack/counter bonus
- call other Bibbles later
- keep moving on the map instead of despawning immediately

## Death, incapacitation and loot

Default should be incapacitation. Permanent death should depend on WorldSettings.

If killed, a Bibble should be lootable. Loot tables should be species/type/biome based.

## Breeding

Bibbles should be breedable later. Breeding should consider:

- species
- type
- personality
- inherited stats
- mutations
- fear/love consequences
- zone/workstation requirements

## Training and relationship items

Create items that influence:

- fear/love scalar
- connection
- obedience
- rebellion
- work speed
- attack learning
- mutation
- temporary buffs/debuffs

Items may have side effects such as stat decreases, fear increase, love loss, addiction-like debuffs or temporary obedience.

## Workstation automation

Workstation menu should become part of the crafting/workstation UI.

Required later:

- assign one Bibble per workstation
- choose recipe
- choose route/network input chest
- choose output chest
- choose fuel/water/resource rules
- simulate or abstract route movement depending on server setting
- special outcomes depending on Bibble type/skills

Examples:

- Fire Bibble removes fuel requirement for some stations.
- Water Bibble removes water bucket requirement.
- Electric Bibble powers machines.
- Nature Bibble improves plant/fiber production.

## Route network

User chose route/network option C. Add a logistics network for linked chests/workstations.

## Fenced Bibble areas

Build a zone tool and Bibble area objects.

Supported zone types later:

- free roam
- work
- train
- breed
- guard
- sleep/rest
- resource gather

If a fence breaks, Bibble reaction depends on connection, fear/love, personality, rebellion and containment quality.

## Trading and stealing

Trading should support:

- player-to-player trade
- terminal trade
- Bibbles against items
- Bibbles against money
- both players confirm
- workstation Bibbles must be recalled before trade

Later mechanics:

- terminal hacking
- stealing from players
- protection items/buildings
- emotional consequences for traded Bibbles

## Combat

Implement:

- active Bibble targeting hotkey
- manual attack trigger
- cooldowns
- autonomous dodging
- player/Bibble friendly fire settings
- Bibble-vs-Bibble combat
- wild Bibble aggression
- type matrix from database
- attack learning
- max four normal attacks, with risky item/mutation exceptions

## Spawn system

Spawning should be biome/segment based.

Need later:

- bigger maps
- map segments
- connected biomes
- cave/indoor segments with short loading transitions if needed
- no wild Bibble spawn inside buildings
- owned/NPC Bibbles may appear inside buildings
- spawn limits per biome/area size

## Database migration

Move these to database or data files later:

- Bibble species
- attacks
- type matrix
- spawn tables
- capture items
- training items
- workstation effects
- personalities and trait modifiers
