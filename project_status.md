# ClientGame – Projektstatus & Übergabe-Dokument

Dieses Dokument fasst **präzise und vollständig** den aktuellen Stand des Projekts zusammen.
Es dient als **Single Source of Truth**, um das Projekt jederzeit in einem neuen Chat oder durch andere Entwickler sauber fortzuführen.

---

## 1. Projektüberblick

**ClientGame** ist ein **server-autoritäres Multiplayer‑2D‑Spiel**.

- **Clients**: senden nur Inputs (MOVE, ACTION, BUILD) + rendern
- **Server**: berechnet Weltzustand, Physik & Spielregeln
- **Kommunikation**: asynchron über **RabbitMQ** (Topic Exchanges)
- **Technologien**:
  - Java 17+
  - Gradle (Multi‑Module)
  - RabbitMQ (Docker)
  - Jackson (JSON)
  - LibGDX (Client – noch nicht implementiert)

---

## 2. Projektstruktur

```text
ClientGame/
├── shared/
│   └── src/main/java/com/tim/game/shared/
│       ├── Messaging/
│       ├── DTOs/
│       │   ├── input/
│       │   └── update/
│       └── Modelle/
├── server/
│   └── src/main/java/com/tim/game/server/
│       ├── net/
│       ├── logic/
│       ├── loop/
│       ├── world/
│       └── GameServerMain.java
├── client/ (leer / geplant)
└── build.gradle.kts / settings.gradle.kts
```

---

## 3. Shared‑Modul (gemeinsam für Client & Server)

### 3.1 Messaging

#### `Topics.java`
**Zweck:** Zentrale Definition aller Exchanges & Routing‑Keys

- `EXCHANGE_INPUTS = "game.inputs"`
- `EXCHANGE_UPDATES = "game.updates"`

→ verhindert Magic Strings

---

#### `MessageType.java` (ENUM)
**Zweck:** Typisierung aller Nachrichten

```java
MOVE, ACTION, BUILD, WORLD_SNAPSHOT
```

---

#### `CommandMessage`
**Client → Server**

| Feld | Typ | Beschreibung |
|----|----|----|
| `type` | MessageType | Art des Commands |
| `roomId` | String | Spielraum |
| `clientId` | String | Absender |
| `payloadJson` | String | DTO als JSON |

---

#### `EventMessage`
**Server → Client**

| Feld | Typ | Beschreibung |
|----|----|----|
| `type` | MessageType | z. B. WORLD_SNAPSHOT |
| `roomId` | String | Raum |
| `targetClientId` | String/null | null = Broadcast |
| `payloadJson` | String | DTO als JSON |

---

### 3.2 Modelle

#### `Vector2f`
- `float x, y`
- Reine Datenklasse für Position & Richtungen

#### `EntityId`
- Kapselt eindeutige Entity‑IDs
- Factory: `EntityId.player(clientId)`

#### `EntityType`
```java
PLAYER, MONSTER, BUILDING
```

---

### 3.3 DTOs – Client Inputs

#### `MoveInputDto`
| Feld | Typ |
|----|----|
| `direction` | Vector2f |

#### `ActionInputDto`
| Feld | Typ |
|----|----|
| `actionType` | String |

#### `BuildInputDto`
| Feld | Typ |
|----|----|
| `structureType` | String |
| `position` | Vector2f |

---

### 3.4 DTOs – Server Updates

#### `PlayerStateDto`
| Feld | Typ |
|----|----|
| `entityId` | EntityId |
| `clientId` | String |
| `type` | EntityType |
| `position` | Vector2f |
| `health / maxHealth` | float |
| `stamina / maxStamina` | float |

#### `WorldSnapshotDto`
| Feld | Typ |
|----|----|
| `roomId` | String |
| `tick` | long |
| `players` | List<PlayerStateDto> |

---

## 4. Server‑Modul

### 4.1 Konfiguration

#### `ServerConfig`
| Feld | Beschreibung |
|----|----|
| rabbitHost | z. B. localhost |
| rabbitPort | 5672 |
| username / password | guest/guest |
| virtualHost | / |
| roomId | z. B. "1" |

Factory:
```java
ServerConfig.localDefault()
```

---

### 4.2 Netzwerk (RabbitMQ)

#### `ServerRabbitConnection`
**Aufgaben:**
- Verbindung zu RabbitMQ
- Exchange‑Deklaration
- Queue‑Deklaration

**Exchanges:**
- `game.inputs` (topic)
- `game.updates` (topic)

**Queue:**
- `server.commands.room.<roomId>`

**Binding:**
```
room.<roomId>.client.*.input
```

---

#### `ServerMessageBus`

**Eingang:**
- `startConsumingCommands()`
- deserialisiert `CommandMessage`
- legt sie in eine interne Queue

**Ausgang:**
- `pollCommands(): List<CommandMessage>`
- `broadcastRoomState(EventMessage)`

---

### 4.3 World / Simulation

#### `EntityState` (intern)

Interner Server‑Zustand (nicht übers Netz):
- EntityId
- EntityType
- clientId (nur Player)
- position
- health / stamina

---

#### `WorldState`

**Single Source of Truth** für die Simulation

**Felder:**
- `Map<EntityId, EntityState> entities`
- `Map<String, EntityId> playerEntities`
- `long tick`

**Methoden:**
- `spawnPlayerForClient(clientId, position)`
- `movePlayer(clientId, direction, speed)`
- `incrementTick()`
- `buildSnapshot(): WorldSnapshotDto`

---

### 4.4 Logik

#### `CommandHandler`

**Input:** `List<CommandMessage>`

**Routing:**
- MOVE → `MoveInputDto` → `worldState.movePlayer()`
- ACTION → Stub
- BUILD → Stub

---

### 4.5 GameLoop

#### `GameLoop`

- Tickrate: z. B. 20 TPS

Pro Tick:
1. `pollCommands()`
2. `applyCommands()`
3. `incrementTick()`
4. `buildSnapshot()`
5. Broadcast via `EventMessage`

---

### 4.6 Einstiegspunkt

#### `GameServerMain`

**Ablauf:**
1. `ServerConfig.localDefault()`
2. `ServerRabbitConnection.connect()`
3. `ServerMessageBus.startConsumingCommands()`
4. `WorldState` erstellen
5. `CommandHandler`
6. `GameLoop.start()`

Server läuft lokal, RabbitMQ via Docker.

---

## 5. Infrastruktur

### RabbitMQ (Docker)

```bash
docker run -d \
  --hostname game-rabbit \
  -p 5672:5672 \
  -p 15672:15672 \
  --name game-rabbit \
  rabbitmq:3-management
```

Management UI:
- http://localhost:15672
- guest / guest

---

## 6. Aktueller Status

✅ Server startet
✅ RabbitMQ verbunden
✅ GameLoop läuft
✅ Commands werden konsumiert
⏳ Client fehlt noch

---

## 7. Nächster großer Block: CLIENT (komplett)

### Phase 1 – Client Core
- Gradle‑Modul `client`
- LibGDX Desktop Launcher
- RabbitMQ Client‑Connection

### Phase 2 – Rendering
- Top‑Down‑Kamera
- Player‑Sprite
- WorldSnapshot rendern

### Phase 3 – Input
- WASD / Maus
- MoveInputDto erzeugen
- CommandMessage publishen

### Phase 4 – Sync
- Snapshot‑Interpolation
- Client‑Prediction (optional)

---

## 8. Empfohlener Prompt für neuen Chat

> "Ich habe ein Java‑Multiplayer‑Projekt mit RabbitMQ. Server ist autoritativ, WorldState + GameLoop existieren. Bitte lies PROJECT_STATUS.md und hilf mir beim kompletten Client‑Aufbau mit LibGDX."

---

**Ende des Dokuments**

