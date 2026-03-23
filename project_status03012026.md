# PROJECT_HANDOVER.md  
## Klare Umsetzungsliste, Dateien, Funktionen (I/O), Konfigurationen & Next Steps (ClientGame)

Dieses Dokument ist eine **komplette Übergabe** für einen **neuen sauberen Chat** (oder eine andere KI).  
Es enthält:

1) **Was wurde gemacht (chronologisch & klar)**  
2) **Alle relevanten Dateien/Module** (die wir in diesem Stand aktiv genutzt/angepasst haben)  
3) **Alle wichtigen Funktionen inkl. Input/Output** (und Zweck)  
4) **Konfigurationen** (RabbitMQ, Ports, Routing, Run-Kommandos)  
5) **Weiterer Plan: kompletter Client** (Roadmap in klaren Schritten)

---

## 1) Was wir gemacht haben (klar & chronologisch)

### A) RabbitMQ-Verbindung & Topologie geprüft/korrekt gemacht
- RabbitMQ läuft via Docker mit Ports:
  - AMQP: **5672**
  - Management UI: **15672**
- Client/Server laufen **außerhalb** von Docker und verbinden sich per `localhost:5672`.
- **Exchanges** wurden als `topic` deklariert:
  - `game.inputs` (Client → Server)
  - `game.updates` (Server → Client)
- **Server-Queue** pro Room wurde erstellt und gebunden:
  - Queue: `server.commands.room.<roomId>`
  - Binding: `room.<roomId>.client.*.input` an `game.inputs`

### B) Fehlerursache „nichts im RabbitMQ UI sichtbar“ verstanden
- Wenn du im UI **keine Queues siehst**, ist häufig die Topologie nicht deklariert oder du schaust in den falschen vhost.
- In unserem Fall: Topologie wird **beim Server-Start** per `declareTopology()` angelegt (Queues/Bindings existieren dann).

### C) Command-Flow stabil gemacht (Client → Server)
- Client publisht `CommandMessage` (MOVE) auf:
  - Exchange: `game.inputs`
  - Routing Key: `room.<roomId>.client.<clientId>.input`
- Server konsumiert aus `server.commands.room.<roomId>` und deserialisiert `CommandMessage`.

### D) Snapshot-Flow stabil gemacht (Server → Client)
- Server sendet pro Tick `WorldSnapshotDto` als JSON in einem `EventMessage` vom Typ `WORLD_SNAPSHOT` auf:
  - Exchange: `game.updates`
  - Routing Key: Broadcast (`room.<roomId>.broadcast.state`) oder private keys (später)
- Client konsumiert Updates und rendert den Snapshot.

### E) Wichtigster Debug-Fix: Snapshot kam an, aber Render hatte `snap=null`
- Ursache: Updates kamen in einem Thread an, Render läuft in LibGDX Render Thread.
- Fix: **Thread-sichere Übergabe** über `AtomicReference<WorldSnapshotDto>` im Client.
- Ergebnis: `lastSnapshot.get()` liefert im Render zuverlässig den neuesten Snapshot.

### F) JSON-Deserialisierung-Fix: `EntityId` war nicht Jackson-fähig
- Client konnte Snapshot nicht parsen wegen `EntityId` in `PlayerStateDto`.
- Fix: `PlayerStateDto.entityId` auf **String** umgestellt (netzwerkfreundlich).
- Ergebnis: Snapshots parsebar, Player-Rendering funktioniert.

### G) Sichtbarer Proof: Bewegung + Kamera-Follow im Client
- Player bewegt sich serverseitig via `MOVE` und wird im Client gerendert.
- Kamera-Follow implementiert: Kamera zentriert sich auf die Position des lokalen Players (cfg.clientId).

---

## 2) Module & relevante Dateien (IST)

> Hinweis: Pfade sind typisch für euer Multi-Module Setup. Wenn einzelne Klassennamen abweichen, gelten die beschriebenen Rollen/Verträge trotzdem.

### shared (gemeinsam)
- `com.tim.game.shared.messaging.Topics`
- `com.tim.game.shared.messaging.MessageType`
- `com.tim.game.shared.messaging.CommandMessage`
- `com.tim.game.shared.messaging.EventMessage`
- `com.tim.game.shared.DTOs.input.MoveInputDto`
- `com.tim.game.shared.DTOs.update.PlayerStateDto` (**entityId ist String!**)
- `com.tim.game.shared.DTOs.update.WorldSnapshotDto`
- `com.tim.game.shared.model.Vector2f`
- `com.tim.game.shared.model.EntityType`
- (optional/weiter vorhanden) `com.tim.game.shared.model.EntityId` (serverintern)

### server
- `com.tim.game.server.GameServerMain`
- `com.tim.game.server.ServerConfig`
- `com.tim.game.server.net.ServerRabbitConnection`
- `com.tim.game.server.net.ServerMessageBus`
- `com.tim.game.server.loop.GameLoop`
- `com.tim.game.server.logic.CommandHandler`
- `com.tim.game.server.world.WorldState`
- `com.tim.game.server.world.EntityState` (server-intern)

### client
- `com.tim.game.client.ClientGame` (LibGDX ApplicationAdapter)
- `com.tim.game.client.net.ClientRabbitConnection`
- `com.tim.game.client.net.ClientMessageBus`
- `com.tim.game.client.net.SnapshotBuffer` (aktuell optional/teilweise ungenutzt)
- `com.tim.game.client.ClientConfig`
- `com.tim.game.client.ClientDesktopLauncher` (Desktop Start)

---

## 3) Funktionen – Input/Output (wichtigste Stellen)

### 3.1 shared.messaging

#### `Topics`
- **Konstanten**
  - `EXCHANGE_INPUTS = "game.inputs"`
  - `EXCHANGE_UPDATES = "game.updates"`
- **Funktionen**
  - `clientInput(roomId, clientId) -> String`  
    **Input:** `roomId`, `clientId`  
    **Output:** `room.<roomId>.client.<clientId>.input`
  - `roomBroadcast(roomId) -> String`  
    **Output:** `room.<roomId>.broadcast.state`
  - `clientPrivate(roomId, clientId) -> String`  
    **Output:** `room.<roomId>.client.<clientId>.private`

#### `CommandMessage`
- **Felder:** `type, roomId, clientId, payloadJson`
- **Input:** Client erzeugt und sendet
- **Output:** Server deserialisiert und verarbeitet

#### `EventMessage`
- **Felder:** `type, roomId, targetClientId, payloadJson`
- **Input:** Server erzeugt und sendet
- **Output:** Client deserialisiert und rendert

---

### 3.2 server.net

#### `ServerRabbitConnection.connect() -> void`
- **Input:** `ServerConfig` (Host/Port/User/Pass/VHost/Room)
- **Output:** offene RabbitMQ `Connection` & `Channel`
- Ruft intern `declareTopology()`

#### `ServerRabbitConnection.declareTopology() -> void`
- **Input:** `roomId`
- **Output:** Exchanges/Queue/Bindings existieren im Broker
- Deklariert:
  - `exchangeDeclare(game.inputs, topic, durable=true)`
  - `exchangeDeclare(game.updates, topic, durable=true)`
  - `queueDeclare(server.commands.room.<roomId>, durable=true)`
  - `queueBind(queue, game.inputs, room.<roomId>.client.*.input)`

#### `ServerMessageBus.startConsumingCommands() -> void`
- **Input:** `channel`, `commandQueueName`
- **Output:** Consumer aktiv; eingehende Messages werden in interne Queue gepusht

#### `ServerMessageBus.pollCommands() -> List<CommandMessage>`
- **Input:** —
- **Output:** Liste aller seit letztem Tick empfangenen Commands

#### `ServerMessageBus.sendEvent(event) -> void`
- **Input:** `EventMessage`
- **Output:** Publish auf `game.updates` (Routing abhängig: broadcast/private)

---

### 3.3 server.world

#### `WorldState.spawnPlayerForClient(clientId, startPos) -> EntityId`
- **Input:** `clientId`, `Vector2f startPosition`
- **Output:** `EntityId` (serverintern)
- Side effects:
  - legt EntityState in `entities` ab
  - mappt `playerEntities[clientId] = entityId`

#### `WorldState.movePlayer(clientId, direction, speed) -> void`
- **Input:** `clientId`, `Vector2f direction`, `float speed`
- **Output:** —
- Side effects:
  - position += direction * speed

#### `WorldState.incrementTick() -> void`
- **Input:** —
- **Output:** —
- Side effect: tick++

#### `WorldState.buildSnapshot() -> WorldSnapshotDto`
- **Input:** —
- **Output:** Snapshot DTO (roomId, tick, players)
- Wichtig: `PlayerStateDto.entityId` ist String

---

### 3.4 server.logic

#### `CommandHandler.applyCommands(commands) -> void`
- **Input:** List<CommandMessage>
- **Output:** —
- Side effects:
  - ruft `handleMove/handleAction/handleBuild`

#### `CommandHandler.handleMove(cmd) -> void`
- **Input:** CommandMessage mit `payloadJson` (MoveInputDto JSON)
- **Output:** —
- Side effect:
  - `worldState.movePlayer(clientId, direction, MOVE_SPEED_PER_TICK)`
  - (Spawn-Logik: Player wird bei Bedarf erzeugt, falls implementiert/aktiv)

---

### 3.5 server.loop

#### `GameLoop.step() -> void`
- **Input:** —
- **Output:** —
- Ablauf:
  1) `commands = messageBus.pollCommands()`
  2) `commandHandler.applyCommands(commands)`
  3) `worldState.incrementTick()`
  4) `snapshot = worldState.buildSnapshot()`
  5) `payloadJson = ObjectMapper.writeValueAsString(snapshot)`
  6) `messageBus.broadcastRoomState(new EventMessage(WORLD_SNAPSHOT, roomId, null, payloadJson))`

---

### 3.6 client.ClientGame (LibGDX)

#### `create() -> void`
- **Input:** —
- **Output:** —
- Initialisiert:
  - Kamera (`OrthographicCamera`) mit `setToOrtho(false, 32, 18)`
  - ShapeRenderer
  - `ClientConfig.localDefault(clientId)`
  - Rabbit Verbindung + MessageBus + Consumer Start

#### `render() -> void`
- **Input:** — (per Frame)
- **Output:** Render-Ausgabe
- Ablauf:
  1) `while ((ev = bus.pollEvent()) != null) { if WORLD_SNAPSHOT -> parse -> lastSnapshot.set(snap) }`
  2) `handleInput()` (WASD -> MOVE publish)
  3) `renderWorld(lastSnapshot.get())`

#### `handleInput() -> void`
- **Input:** Tastatur (WASD)
- **Output:** Publish `CommandMessage(MOVE)` auf `game.inputs`
- Details:
  - direction normalisiert
  - payloadJson = MoveInputDto JSON

#### `renderWorld(snapshot) -> void`
- **Input:** `WorldSnapshotDto` (oder null)
- **Output:** Zeichnet Spieler-Kreise + Debug
- Kamera-Follow:
  - finde PlayerState mit `clientId == cfg.clientId`
  - setze `camera.position` auf Player-Position

#### `dispose() -> void`
- **Input:** —
- **Output:** —
- schließt Renderer + Rabbit Connection

---

## 4) Konfigurationen (Technologien)

### 4.1 RabbitMQ (Docker)
Run:
```bash
docker run -d   --hostname game-rabbit   -p 5672:5672   -p 15672:15672   --name game-rabbit   rabbitmq:3-management
```
UI:
- http://localhost:15672
- user/pass: guest/guest
- vhost: `/`

### 4.2 Exchanges / Routing / Queues
- Exchanges (topic):
  - `game.inputs`
  - `game.updates`
- Server Queue:
  - `server.commands.room.<roomId>`
- Server Binding:
  - `room.<roomId>.client.*.input` (an `game.inputs`)
- Client publish MOVE:
  - exchange=`game.inputs`
  - routingKey=`room.<roomId>.client.<clientId>.input`
- Server broadcast snapshot:
  - exchange=`game.updates`
  - routingKey=`room.<roomId>.broadcast.state`

### 4.3 Gradle Run
- Server:
```bash
./gradlew :server:run
```
- Client:
```bash
./gradlew :client:run
```

---

## 5) Aktueller Stand (Fakten)
✅ Server startet & läuft stabil (GameLoop aktiv)  
✅ RabbitMQ verbunden  
✅ Client publisht MOVE Commands  
✅ Server konsumiert & verarbeitet MOVE  
✅ Server sendet WORLD_SNAPSHOT pro Tick  
✅ Client empfängt & deserialisiert WORLD_SNAPSHOT  
✅ Player wird gerendert & bewegt sich sichtbar  
✅ Kamera folgt lokalem Spieler  

---

## 6) Weiterer Plan (kompletter Client – nächste Schritte)

### Phase 1 – Rendering-Qualität (kurz & wichtig)
1) **Kamera-Smoothing**
   - lerp zwischen current camera pos und player pos
2) **Zoom**
   - Mausrad verändert `camera.zoom`
3) **Grid/Background**
   - einfacher Tile/Grid Hintergrund, damit Bewegung visuell eindeutig ist

### Phase 2 – Multiplayer-Funktionen
4) **Mehrere Spieler sauber darstellen**
   - Farben pro ClientId
   - Name/ID debug text über Player
5) **JOIN/LEAVE Flow**
   - neue MessageTypes: `JOIN`, `LEAVE`
   - Server erzeugt Player erst nach JOIN
   - Client sendet JOIN beim Start
6) **Disconnect Handling**
   - Server entfernt Player nach Timeout oder LEAVE

### Phase 3 – Networking-Verbesserung (für bessere UX)
7) **Snapshot-Interpolation**
   - Client buffert Snapshots (z.B. 100–200ms Delay) und interpoliert Positionen
8) **Client-Prediction (optional)**
   - Client simuliert lokale Bewegung sofort
   - Server korrigiert bei Abweichung (reconciliation)

### Phase 4 – Grafiken & Spielwelt
9) Sprites statt ShapeRenderer
10) Map-Grenzen + Kollisionen
11) Entities: Interactables / Buildings / Gegner

---

## 7) Prompt für neuen sauberen Chat (Copy/Paste)

> Ich habe ein Java-Multiplayer-2D-Spiel mit RabbitMQ (Docker), server-autoritativer Simulation, Tick-GameLoop und einem LibGDX-Client.  
> Movement (MOVE) funktioniert: Client sendet Commands, Server verarbeitet, sendet WORLD_SNAPSHOT, Client rendert und Kamera folgt lokalem Spieler.  
> Bitte lies PROJECT_HANDOVER.md und führe als Nächstes den **kompletten Client-Ausbau** durch: Kamera-Smoothing, Zoom, Grid/Background, Multiplayer-UI, JOIN/LEAVE, Snapshot-Interpolation.

---

**ENDE**
