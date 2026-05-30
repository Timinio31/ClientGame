# ClientGame Menu + Direct Connect Patch

Dieser Patch baut den Client auf ein LibGDX-Screen-System um und ergänzt:

- Hauptmenü mit `Play Local`, `Play Multiplayer`, `Settings`, `Exit`
- Multiplayer-Menü mit `Direct Connect`, `Saved Servers`, `Create / Save Server Profile`, `Back`
- manuelle IP-/Host-Verbindung über RabbitMQ-Host, Port, User, Passwort, VirtualHost und Room-ID
- lokal gespeicherte Serverprofile unter `~/.clientgame/saved-servers.json`
- `Save + Connect` in der Direct-Connect-Ansicht
- einfache Create-Server-Profile-Ansicht zum Speichern lokaler Server-/Room-Profile
- aktuelle Spiellogik aus `ClientGame` wurde in `GameScreen` verschoben
- beim Betreten des GameScreens wird automatisch ein initialer `PING` gesendet, damit der Server `MAP_INIT` verschickt

## Austauschen

Diese Dateien komplett ersetzen:

```text
client/src/main/java/com/tim/game/client/ClientGame.java
client/src/main/java/com/tim/game/client/net/ClientConfig.java
```

## Neu erstellen

```text
client/src/main/java/com/tim/game/client/config/ServerProfile.java
client/src/main/java/com/tim/game/client/config/SavedServerRepository.java
client/src/main/java/com/tim/game/client/ui/MenuButton.java
client/src/main/java/com/tim/game/client/ui/TextInputField.java
client/src/main/java/com/tim/game/client/screen/AbstractMenuScreen.java
client/src/main/java/com/tim/game/client/screen/MainMenuScreen.java
client/src/main/java/com/tim/game/client/screen/MultiplayerMenuScreen.java
client/src/main/java/com/tim/game/client/screen/DirectConnectScreen.java
client/src/main/java/com/tim/game/client/screen/SavedServersScreen.java
client/src/main/java/com/tim/game/client/screen/CreateServerScreen.java
client/src/main/java/com/tim/game/client/screen/SettingsScreen.java
client/src/main/java/com/tim/game/client/screen/GameScreen.java
```

## Wichtig

Ein echter Server-Browser bzw. automatisches Finden öffentlicher Server ist noch nicht enthalten. Dafür wird später ein Master-/Lobby-Service benötigt. Die jetzige Version unterstützt:

- Variante A: Direct Connect per Host/IP
- Variante B: lokal gespeicherte Serverprofile

Das Room-Passwort ist als Feld vorbereitet, wird serverseitig aber noch nicht geprüft. Dafür sollte später `JOIN_ROOM`, `JOIN_ACCEPTED` und `JOIN_REJECTED` ergänzt werden.
