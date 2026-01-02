# 🗺️ Projekt-Roadmap – Java/LibGDX Multiplayer Game mit RabbitMQ

**Author:** Tim Zeleznik  
**Status:** Initial Version  
**Struktur:** Sehr detailliert für Setup & Architektur, gröber für spätere Features  
**Architektur:** Player-hosted authoritative Server, Clients senden Inputs → Server berechnet → Clients rendern  

---

# Phase 0 – Grundsetup & Projektstruktur

### 🎯 Ziel  
Lauffähiges Multi-Modul-Projekt (`shared`, `server`, `client`) + lokale RabbitMQ-Instanz.

### To‑Dos  
- [x] Java 17+ installieren  
- [x] Git‑Repository `CLIENTGAME` erstellen  
- [x] Projektstruktur anlegen:
  ```
  game-project/
    shared/
    server/
    client/
  ```
- [x] Gradle als Buildsystem einrichten  
- [ ] Lokale RabbitMQ-Instanz starten (Docker)
  ```bash
  docker run -d --hostname game-rabbit     -p 5672:5672 -p 15672:15672     --name game-rabbit rabbitmq:3-management
  ```
- [x] Management-UI öffnen (http://localhost:15672)  
- [x] Benutzer/Passwort konfigurieren (standard: guest/guest)

---

# Phase 1 – `shared` Modul: Protokoll & DTOs

### 🎯 Ziel  
Alle Messages, DTOs, Enums, Routing-Keys → zentral definiert.  
Keine Logik, kein Netzwerkkram, nur Datenstrukturen.

### To‑Dos  

//ich denke mal das mus sin shared rein 
**Messaging:**
- [x] `Topics.java` (Exchanges, RoutingKeys, Helper)
- [x] `MessageType.java` (ENUM)
- [x] `CommandMessage.java`
- [x] `EventMessage.java`

**Modelle:**
- [x] `Vector2f.java`
- [x] `EntityId.java`
- [x] `EntityType.java`

**DTOs – Client Inputs:**
- [x] `MoveInputDto.java`
- [x] `ActionInputDto.java`
- [x] `BuildInputDto.java`

**DTOs – Server Updates:**
- [x] `PlayerStateDto.java`
- [x] `WorldSnapshotDto.java`

### Abnahmekriterium  
`shared` kompiliert, keinerlei Abhängigkeiten zu LibGDX oder RabbitMQ.

---

# Phase 2 – Server Skeleton (Player‑Hosted Game Server)

### 🎯 Ziel  
Server startet, verbindet sich mit RabbitMQ, empfängt Commands, broadcastet Test‑Events.

### To‑Dos  

**Server-Struktur:**
```
server/
  GameServerMain.java
  ServerConfig.java
  /net
  /loop
  /world
  /logic
```

**Netzwerk:**
- [x] `ServerRabbitConnection.java`
- [x] Exchanges deklarieren:
  - `game.inputs`
  - `game.updates`
- [x] Queue registrieren:
  - `server.commands.room.<roomId>`
- [x] `ServerMessageBus.java`:
  - [x] `pollCommands()`
  - [x] `sendEvent()`
  - [x] `broadcastRoomState()`

**Server Kernlogik:**
- [x] `WorldState.java`
- [x] `CommandHandler.java`
- [x] `GameLoop.java` (z. B. 20 Ticks/Sekunde)

### Test  
Server loggt ankommende Commands in der Console.

---

# Phase 3 – Client Skeleton (LibGDX + RabbitMQ)

### 🎯 Ziel  
Client startet LibGDX-Fenster, verbindet sich mit RabbitMQ, sendet Test-Commands → Server loggt sie.

### To‑Dos  

**Client-Struktur:**
```
client/
  GameClientMain.java
  GameApplication.java
  /screen
  /render
  /input
  /net
  /state
```

**Config:**
- [ ] `ClientConfig.java` (roomId, playerId, rabbit host)

**Netzwerk:**
- [ ] `ClientRabbitConnection.java`
  - Queue: `client.<clientId>.events`
- [ ] `ClientMessageBus.java`
  - thread-safe Queue für Events
  - `sendCommand()`
  - `pollEvents()`

**LibGDX:**
- [ ] `GameApplication.java` → `setScreen(new GameScreen())`
- [ ] `GameScreen.java`:
  - `render()`
    - Events aus MessageBus verarbeiten
    - Welt rendern (erstmal nur schwarzer Bildschirm)
- [ ] `GameInputProcessor.java`
  - bei Tastendruck → `sendCommand(new MOVE)`

### Test  
→ Client drückt Taste → Server loggt Command.

---

# Phase 4 – Echtes Feature: Movement & Spieleranzeige

### 🎯 Ziel  
Der Spieler bewegt sich sichtbar – vollständig serverseitig berechnet.

### Server:  
- [ ] `PlayerEntity` in `WorldState`  
- [ ] `CommandHandler.handle(MOVE)`  
- [ ] `MovementSystem.java`  
- [ ] Im `GameLoop`:
  - [ ] Alle paar Ticks → `WorldSnapshotDto` per Broadcast versenden

### Client:  
- [ ] `WorldClientState.java`
  - Map `playerId -> PlayerStateDto`
- [ ] `ClientEventHandler.java`
- [ ] `WorldRenderer.renderPlayers()`
- [ ] Input → Movement Command (`MoveInputDto`)

### Test  
Spieler bewegt sich im Fenster (Sprite/Quadrat).  
Position kommt *vom Server*, Client interpoliert.

---

# Phase 5 – Welt & Tiles (mittlere Details)

### 🎯 Ziel  
Grundlegende Welt-Struktur und Tiles sichtbar machen.

### To‑Dos  
- [ ] Weltstruktur: 2D-Array oder Chunk-System serverseitig  
- [ ] TileTypes: Grass, Dirt, Water, Ore, Oil  
- [ ] Server:
  - [ ] Welt initialisieren
  - [ ] Tiles in Snapshot zu Clients übertragen  
- [ ] Client:
  - [ ] TileMap-rendering
  - [ ] einfacher Kamera-Fokus auf Spieler

---

# Phase 6 – Ressourcen & Mining (mittlere Details)

### 🎯 Ziel  
Player kann Ressourcen abbauen und bekommt Loot.

### Server  
- [ ] `RESOURCE_NODE` Entity  
- [ ] `MineInputDto` → `CommandHandler`  
- [ ] `ResourceSystem`:
  - Node.amount--  
  - Loot an Inventar des Spielers

### Client  
- [ ] Mining-Command per Click  
- [ ] Inventar UI (sehr basic)  
- [ ] Node verschwindet oder ändert Sprite wenn leer

---

# Phase 7 – Bauen & Platzieren

### 🎯 Ziel  
Spieler kann basierend auf Ressourcen Maschinen/Strukturen bauen.

### Server  
- [ ] `BuildInputDto`  
- [ ] `BuildService`  
- [ ] `BuildingEntity`  
- [ ] `BuildingSystem` (Grundvalidierung: Platz frei?)  

### Client  
- [ ] Ghost-Sprite mit rot/grün  
- [ ] Rechtsklick / Tastendruck zum Platzieren  
- [ ] Buildings rendern

---

# Phase 8 – Monster & Kampf (grober)

### 🎯 Ziel  
Monster spawnen, bewegen sich, verfolgen Spieler und können Schaden verursachen.

### To‑Dos  
- [ ] MonsterEntity  
- [ ] MonsterSystem: Simple AI (patrol → chase → attack)  
- [ ] CombatSystem:
  - Trefferlogik
  - HP-Rechnung  
- [ ] Loot  

---

# Phase 9 – Capture & Summon System (grober)

### 🎯 Ziel  
Monster können gefangen & beschworen werden.

### Server  
- [ ] CaptureInputDto  
- [ ] SummonInputDto  
- [ ] Monster-Ownership  
- [ ] Beschwörte Monster folgen Spieler & kämpfen mit ihm

### Client  
- [ ] Capture UI  
- [ ] Monster-Inventar  
- [ ] Sichtbare beschworene Monster

---

# Phase 10 – Player-Hosted UX (grober)

### 🎯 Ziel  
Eine Spielerfreundliche Lösung zum Starten/Joinen einer Session.

### To‑Dos  
- [ ] Host-Menu:
  - „Host Game“ → startet Server
  - „Join Game“ → IP/Room eingeben  
- [ ] Config-Speicherung  
- [ ] UI für Serverstart/Status

---

# Phase 11 – Optional / Nice to Have

### Ideen  
- Reconnect bei Netzwerkverlust  
- Client-Ping-Anzeige  
- Map-Editor (Tile-basierend)  
- Modding-System (Monster/Rezepte per JSON laden)  
- Debug Overlay (FPS, Entities, TickTime)  
- Replay-System (based on Command-Log)

---

# 🧩 Zusammenfassung

Diese Roadmap führt dich:

- von **Setup & Skeleton (sehr detailliert)**  
- hin zu **tile-basierter Welt**  
- bis zu **Mining, Crafting, Bauen, Monster, Kampf**  
- und später optionalen Features.

Sie ist so aufgebaut, dass du **immer funktionsfähige Zwischenversionen** erhältst.

Wenn du möchtest, generiere ich dir aus Phase 1 direkt **Shared‑Modul‑Skeleton‑Code**, komplett kompilierbar.
