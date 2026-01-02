# 📘 Game Architecture – Clientseitiges Java/LibGDX-Spiel mit RabbitMQ

**Status:** Draft  
**Author:** Tim Zeleznik  
**Technologien:** Java 17+, LibGDX, RabbitMQ (AMQP), JSON, gradle(basic, mit Kotlin DSl)
**Perspektive:** 2D Top-Down  
**Spielkonzept:** Crafting, Monster, PvP, Platzieren/Abbauen, Ressourcen-System (Strom, Öl, Wasser), Multiplayer  

---

## 1. Überblick

Dieses Dokument beschreibt die Architektur eines clientseitigen Java-Spiels, das LibGDX für Rendering & Input nutzt und über RabbitMQ mit einem autoritativen Server kommuniziert.

Das Spiel ist ein **2D Top-Down Multiplayer Sandbox Game** mit folgenden Kernfeatures:

- Ressourcen abbauen & Strukturen platzieren  
- Monster bekämpfen, fangen & beschwören  
- Crafting-System mit Strom/Öl/Wasser  
- Spieler-Status (HP, Stamina, Items)  
- PvP & Monster-KI  
- Welt mit Tiles, Nodes und Maschinen  

---

## 2. Hosting-Modell

Das Spiel verwendet ein **Player-Hosted-Server-Modell**:

- Einer der Spieler startet die Session als **Host**.  
- Auf der Host-Maschine läuft der **Game-Server-Prozess** (oder ein separater Java-Server, der auf demselben Rechner läuft).  
- Alle anderen Spieler verbinden sich als **Clients** zu diesem Host-Server.  
- Der Host ist damit:
  - Gateway zu RabbitMQ (oder betreibt RabbitMQ lokal bzw. im gleichen Netzwerk),
  - authoritative Instanz für Physik, Kampf, Crafting und Weltzustand.

**Konsequenzen:**

- Wenn der Host das Spiel schließt, endet die Session (oder es braucht später ein Host-Migration-Konzept).  
- Für echte Public-Matches wäre ein dedizierter Server sinnvoller, aber für dein Projekt ist Player-Hosted ein guter Start.

---

## 3. Technologiestack

### 3.1 Client

- **Java 17+**  
- **LibGDX**  
  - Orthographic Camera  
  - SpriteBatch / TextureAtlases  
  - Tile-basierte Welt  
  - Input Processing  
- Optional: **Box2D** für Physik auf Serverseite (Client rendert nur Ergebnisse)

### 3.2 Kommunikation

- **RabbitMQ** als Message-Broker  
- **Java AMQP Client** (`amqp-client` Library)  
- JSON-basierte Commands & Events

### 3.3 Architekturform

- **Client = Thin Client**
- **Server = Authoritative**
- Client:
  - sammelt nur Input,
  - rendert Welt & UI basierend auf Server-Updates.
- Server:
  - berechnet Physik, Kampf, Monster, Ressourcen und Crafting,
  - validiert jede Aktion (No-Cheat-Prinzip).

---

## 4. Systemübersicht

```text
┌───────────────────────────────────────┐
│             Player-Hosted             │
│          Game Server (Host)           │
│   • Authoritative World State         │
│   • Physics, Combat, Crafting         │
│   • Monster AI, Spawns                │
│   • RabbitMQ Consumer (Commands)      │
│   • RabbitMQ Producer (Events)        │
└───────────────────────────────────────┘
                 ▲           ▼
     Commands    │           │    Events
                 │           │
┌───────────────────────────────────────┐
│                Clients                │
│     Java + LibGDX Renderer            │
│     • Input Processing                │
│     • World Rendering                 │
│     • Animation & Interpolation       │
│     • RabbitMQ Consumer               │
│     • RabbitMQ Producer               │
└───────────────────────────────────────┘
```

---

## 5. Messaging-Konzept (Topics / „Themes“)

Es gibt zwei zentrale Exchanges:

- **`game.inputs`** – für alles, was **vom Client → Server** geht  
- **`game.updates`** – für alles, was **vom Server → Clients** geht  

Die Routing Keys folgen dem Muster:

```text
room.<roomId>.<scope>.<clientId(optional)>.<type>
```

### 5.1 Client → Server (Inputs)

- Exchange: `game.inputs`
- Beispiele:
  - `room.1.client.42.input`       – generischer Input
  - `room.1.client.42.move`        – Bewegungsinput
  - `room.1.client.42.action`      – Aktionen (Attacke, Interaktion)
  - `room.1.client.42.build`       – Bauen / Platzieren

Clients senden **nur Inputs**, der Server entscheidet, was passiert.

### 5.2 Server → Clients (Updates)

- Exchange: `game.updates`
- Beispiele:
  - `room.1.broadcast.state`       – Weltzustand / State-Updates an alle Clients in Raum 1
  - `room.1.client.42.private`     – private Infos, z. B. Inventar, geheime Daten

### 5.3 Topics-Hilfsklasse (shared)

```java
public final class Topics {
    public static final String EXCHANGE_INPUTS = "game.inputs";
    public static final String EXCHANGE_UPDATES = "game.updates";

    public static String clientInput(String roomId, String clientId) {
        return "room." + roomId + ".client." + clientId + ".input";
    }

    public static String roomBroadcast(String roomId) {
        return "room." + roomId + ".broadcast.state";
    }

    public static String clientPrivate(String roomId, String clientId) {
        return "room." + roomId + ".client." + clientId + ".private";
    }
}
```

---

## 6. Projekt- & Modulstruktur

Das Projekt wird als Monorepo mit drei Modulen organisiert:

```text
game-project/
  shared/        -> Gemeinsame Models & Messages
  client/        -> LibGDX-Client
  server/        -> Game-Server (hosted by one player)
```

### 6.1 `shared/` – Protokoll & Datenmodelle

```text
shared/
  src/main/java/com/tim/game/shared/
    messaging/
      Topics.java              // Strings für Exchanges & Routing Keys
      MessageType.java         // ENUM für Command/Event-Typen
      CommandMessage.java      // Basis-Klasse für Client→Server
      EventMessage.java        // Basis-Klasse für Server→Client
    model/
      Vector2f.java
      EntityId.java
      EntityType.java
      PlayerStateDto.java
      WorldSnapshotDto.java
      input/
        MoveInputDto.java
        ActionInputDto.java
        BuildInputDto.java
```

**Regel:**  
In `shared` liegt **niemals** Logik, kein Rendering, keine RabbitMQ-Connection – nur DTOs, Enums und Protokoll-Strukturen.

---

### 6.2 `client/` – LibGDX-Client & RabbitMQ-Client

```text
client/
  src/main/java/com/tim/game/client/
    GameClientMain.java            // DesktopLauncher + Setup
    GameApplication.java           // extends Game (LibGDX)
    screen/
      GameScreen.java              // Hauptspiel
      MainMenuScreen.java
    net/
      ClientRabbitConnection.java  // Stellt Connection/Channel her
      ClientMessageBus.java        // send/receive Commands/Events
      ClientEventHandler.java      // verarbeitet Server-Events → Client-State
    state/
      WorldClientState.java        // Client-Live-View der Welt
      PlayerClientState.java       // lokaler Player-Cache
    input/
      GameInputProcessor.java      // Keyboard+Mouse → Commands
      ActionMapper.java            // Mappt LibGDX-Input → InputDto
    render/
      WorldRenderer.java           // Tiles, Entities
      EntityRenderer.java
      UIRenderer.java              // HUD, HP, Stamina
    config/
      ClientConfig.java            // Broker-URL, RoomId, PlayerId, etc.
```

**Wichtigste Main-Klassen (Client):**

- **`GameClientMain`**  
  Einstiegspunkt (DesktopLauncher), liest Config, startet `GameApplication`.

- **`GameApplication`**  
  - LibGDX `Game`.  
  - Erstellt `ClientRabbitConnection` & `ClientMessageBus`.  
  - Setzt `GameScreen` als aktive Screen.

- **`GameScreen`**  
  - Hält `WorldClientState`, `WorldRenderer`, `GameInputProcessor`.  
  - Im `render()`:
    - Pollt Events von `ClientMessageBus`.  
    - Aktualisiert `WorldClientState`.  
    - Zeichnet Welt & UI.

- **`ClientRabbitConnection`**  
  - Baut RabbitMQ `Connection` & `Channel` auf.  
  - Deklariert Exchanges, Queues, Bindings.

- **`ClientMessageBus`**  
  - API: `sendCommand(...)`, `pollEvents()`.  
  - Intern: Producer & Consumer-Threads/Callback → Thread-sichere Queue.

---

### 6.3 `server/` – Game-Server (auf Host-Spieler)

```text
server/
  src/main/java/com/tim/game/server/
    GameServerMain.java            // Startpunkt (main)
    ServerConfig.java              // Ports, Broker-URL, Ticks, etc.
    net/
      ServerRabbitConnection.java  // Connection/Channel/Queues
      ServerMessageBus.java        // empfängt Commands, verschickt Events
    loop/
      GameLoop.java                // Tickt Welt, Systems etc.
    world/
      WorldState.java              // kompletter Server-Weltzustand
      RoomManager.java             // mehrere Rooms/Instanzen
      entity/
        Entity.java
        PlayerEntity.java
        MonsterEntity.java
        BuildingEntity.java
      systems/
        MovementSystem.java
        CombatSystem.java
        MonsterSystem.java
        BuildingSystem.java
        ResourceSystem.java
    logic/
      CommandHandler.java          // verarbeitet Client-Commands
      PlayerService.java
      MonsterService.java
      BuildService.java
    persistence/ (optional/später)
      WorldRepository.java
      PlayerRepository.java
```

**Wichtigste Main-Klassen (Server):**

- **`GameServerMain`**
  - Lädt `ServerConfig`.  
  - Erstellt `ServerRabbitConnection` & `ServerMessageBus`.  
  - Erstellt `WorldState`/`RoomManager`.  
  - Startet `GameLoop`.

- **`GameLoop`**
  - Z. B. 20 Ticks/Sekunde.  
  - Pro Tick:
    - `messageBus.pollCommands()` → an `CommandHandler` übergeben.  
    - `systems` ausführen: Movement, Combat, Monster, Ressourcen.  
    - relevanten State über `ServerMessageBus` als Events verschicken.

- **`ServerMessageBus`**
  - API:
    - `pollCommands() : List<CommandMessage>`  
    - `sendEvent(EventMessage event)`  
    - `broadcastRoomState(roomId, WorldSnapshotDto snapshot)`.

---

## 7. Spielwelt & Komponenten

### 7.1 Tile-Based World

- Welt besteht aus 32×32 Pixel-Tiles.  
- Tile-Typen:
  - Gras
  - Erde
  - Erzvorkommen
  - Wasser
  - Ölquelle
  - Fels  
- Tiles können abgebaut oder bebaut werden.

### 7.2 Entities (Server-seitig)

```java
class Entity {
    String id;
    EntityType type;
    Vector2f position;
    float rotation;
    float hp;
    float maxHp;
    Map<ComponentType, Component> components;
}
```

Entity-Typen:

- Spieler  
- Monster  
- Gebäude  
- Resource Nodes  
- Projektile  

---

## 8. Gameplay-Features (Server-Logik, Client-Rendering)

### 8.1 Movement

- Client:
  - sendet Bewegungs-Inputs (`MoveInputDto`).  
  - optional: leichte Client-Prediction (lokale Bewegung, bis Server bestätigt).
- Server:
  - verarbeitet Bewegungsinputs im `MovementSystem`.  
  - berechnet neue Positionen.  
  - sendet Positionsupdates an Clients (`PlayerStateDto`, `EntityUpdateEvent`).

### 8.2 Bauen & Platzieren

- Client:
  - zeigt Ghost-Sprite (Platzierungs-Vorschau).  
  - sendet Build-Commands (`BuildInputDto`) an den Server.
- Server:
  - prüft Ressourcen, Blockaden, Regeln.  
  - erzeugt Gebäude-Entity.  
  - broadcastet Update (`BuildingPlacedEvent`).

### 8.3 Mining

- Client:
  - klickt Ressourcen-Node.  
  - sendet „Mine“-Command.
- Server:
  - reduziert `remainingAmount`.  
  - gibt Loot ins Inventar (Server-seitig).  
  - sendet aktualisierte States (Inventar, Node).

### 8.4 Monster-System

- Monster spawnen in Biomen (Server-Seite).  
- Verhalten in `MonsterSystem`.  
- Monster können:
  - gefangen werden (Capture-Command → Server prüft Bedingungen).  
  - beschworen werden (Summon-Command).  
  - im Kampf Schaden nehmen und Loot droppen.

### 8.5 Spieler-Status

- Attribute:
  - HP  
  - Stamina  
  - optional: Hunger / Mana  
- Berechnet auf dem Server.  
- Client zeigt nur Balken/Icons.

---

## 9. Multiplayer-Logik & Threading

### 9.1 Client Prediction (optional)

- Client bewegt den Spieler direkt.  
- Server arbeitet als Korrektiv:
  - schickt echte Position, wenn Abweichung zu groß.  
  - Client interpoliert/korrektiert sanft.

### 9.2 Threading-Regeln

- RabbitMQ-Consumer läuft **nicht** im Render-/GameLoop-Thread.  
- Events/Commands werden in eine thread-sichere Queue geschrieben.  
- Verarbeitung erfolgt:
  - Client: im LibGDX-Render-Thread (`GameScreen.render()`).  
  - Server: im `GameLoop`-Thread.

---

## 10. Sicherheits- & Architektur-Regeln

1. **Alles, was über RabbitMQ geht, ist ein DTO im `shared`-Modul.**  
   - Keine generischen Maps o. Ä.  
   - Saubere Klassen: `MoveInputDto`, `AttackInputDto`, `WorldSnapshotDto`.

2. **Keine Game-Logik in `net/`-Klassen.**  
   - `ClientMessageBus` und `ServerMessageBus` wissen nur von:
     - Commands rein  
     - Events raus  
   - Keine Bewegungs- oder Schadensberechnung.

3. **Logik-Pro-Bereich → eigenes System.**  
   - Bewegung → `MovementSystem`.  
   - Kampf → `CombatSystem`.  
   - Monster → `MonsterSystem`.  
   - Gebäude & Strom → `BuildingSystem` / `ResourceSystem`.

4. **Kommandos gehen immer über `CommandHandler`.**  
   - `CommandHandler` liest `CommandMessage.type` und ruft passende Services auf.

5. **Weltzustand ist zentral im `WorldState`/`RoomState`.**  
   - Keine verteilten HashMaps-Vereinsamung.

6. **Rendering kennt keine RabbitMQ-Klassen.**  
   - Renderer arbeitet nur mit `WorldClientState`.

7. **Der Host-Spieler betreibt den Server-Prozess.**  
   - Andere Spieler verbinden sich per IP/Adresse des Hosts.  
   - Optional: Host kann das Spiel über ein UI starten/stoppen.

---

## 11. Erweiterbarkeit & Modularität

- Neue Monster → JSON- oder Config-Definitionen, neue Einträge in `EntityType`.  
- Neue Rezepte → Crafting-Konfig.  
- Neue Gebäude → Building-Config + Renderer-Mapping.  
- Content ist möglichst datengetrieben, nicht fest in Code einbetoniert.

---

## 12. Nächste Schritte

1. `shared`-Modul mit:
   - `Topics.java`
   - `CommandMessage`, `EventMessage`
   - `MoveInputDto`, `PlayerStateDto`, `WorldSnapshotDto`
2. `server`-Skelett:
   - `GameServerMain`, `ServerRabbitConnection`, `ServerMessageBus`, `GameLoop`.
3. `client`-Skelett:
   - `GameClientMain`, `GameApplication`, `ClientRabbitConnection`, `ClientMessageBus`, `GameScreen`.

Ab hier kannst du die Ordner einfach **per Drag & Drop** in dein Repo übernehmen und Schritt für Schritt implementieren.
