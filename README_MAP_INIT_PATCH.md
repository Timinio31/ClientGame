# ClientGame MAP_INIT Patch

## Ziel

Die angefangene Umstellung wurde fertig integriert:

- Die statische Map wird einmalig pro Client über `MessageType.MAP_INIT` gesendet.
- `WorldSnapshotDto` enthält nur noch dynamische Daten: `players`, `buildings`, `tick`, `roomId`.
- Der Client speichert die Map getrennt vom dynamischen Snapshot.
- `WorldRenderer` rendert Tiles aus `MapInitDto` und Spieler/Gebäude aus `WorldSnapshotDto`.
- `CommandHandler` merkt sich initialisierte Clients und sendet `MAP_INIT` nur einmal pro Client und Room.

## Dateien ersetzen

Diese Dateien komplett ersetzen:

```text
client/src/main/java/com/tim/game/client/ClientGame.java
client/src/main/java/com/tim/game/client/net/ClientMessageBus.java
client/src/main/java/com/tim/game/client/render/WorldRenderer.java

server/src/main/java/com/tim/game/server/GameServerMain.java
server/src/main/java/com/tim/game/server/logic/CommandHandler.java
server/src/main/java/com/tim/game/server/loop/GameLoop.java
server/src/main/java/com/tim/game/server/net/ServerMessageBus.java
server/src/main/java/com/tim/game/server/world/WorldState.java

shared/src/main/java/com/tim/game/shared/DTOs/update/WorldSnapshotDto.java
shared/src/main/java/com/tim/game/shared/Messaging/MessageType.java
shared/src/main/java/com/tim/game/shared/debug/DebugCategory.java
shared/src/main/java/com/tim/game/shared/debug/DebugConfig.java
```

## Neue Datei erstellen

Falls sie bei dir noch nicht existiert, diese Datei neu erstellen:

```text
shared/src/main/java/com/tim/game/shared/DTOs/update/MapInitDto.java
```

## Ablauf danach

1. RabbitMQ starten.
2. Server starten.
3. Client starten.
4. Der Client sendet beim Start automatisch einen initialen `PING`.
5. Der Server nutzt diesen ersten Command, um einmalig `MAP_INIT` an diesen Client zu senden.
6. Danach sendet der Server pro Tick nur noch `WORLD_SNAPSHOT` ohne Map.

## Prüfung

Die Dateien wurden mit einer lokalen Java-18-Stub-Kompilierung gegen fehlende Imports, falsche Methodennamen und die entfernten `getMap()`/`setMap()`-Aufrufe geprüft.
Ein echter Gradle-Build war in dieser Umgebung nicht möglich, weil der Gradle Wrapper die Distribution aus dem Internet laden wollte und hier kein Internetzugriff verfügbar ist.
