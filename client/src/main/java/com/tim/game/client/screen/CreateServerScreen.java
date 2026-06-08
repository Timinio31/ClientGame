package com.tim.game.client.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.tim.game.client.ClientGame;
import com.tim.game.client.config.SavedServerRepository;
import com.tim.game.client.config.ServerProfile;
import com.tim.game.client.config.WorldSettingsFileRepository;
import com.tim.game.client.net.ClientConfig;
import com.tim.game.client.ui.MenuButton;
import com.tim.game.client.ui.TextInputField;
import com.tim.game.shared.config.WorldSettings;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CreateServerScreen extends AbstractMenuScreen {
    private static final String SUPPORTED_FLAGS = "movement,interaction,inventory,building,crafting,craftingMenu,inventoryCrafting,workstationCrafting,toolCrafting,craftingQuality,craftingFailure,combat,teamCall,companions,groups,bibbles,bibbleSpawning,bibbleCapturing,bibbleTrading,bibbleWorkstationAutomation,bibbleCombat,bibbleFriendlyFire,bibblePermanentDeath,bibbleFreeRoam,bibbleTerminal,bibbleAreas,starterBibbles,worldItems,mapEditing,settlements,starterItems";

    private final SavedServerRepository repository = new SavedServerRepository();
    private final WorldSettingsFileRepository worldSettingsFileRepository = new WorldSettingsFileRepository();
    private final List<TextInputField> fields = new ArrayList<>();
    private int activeFieldIndex = 0;

    private TextInputField serverNameField;
    private TextInputField hostField;
    private TextInputField portField;
    private TextInputField roomField;
    private TextInputField roomPasswordField;

    private TextInputField worldNameField;
    private TextInputField gameModeField;
    private TextInputField mapModeField;
    private TextInputField mapIdField;
    private TextInputField mapFileField;
    private TextInputField mapWidthField;
    private TextInputField mapHeightField;
    private TextInputField seedField;
    private TextInputField moveIntervalField;
    private TextInputField moveCooldownTicksField;
    private TextInputField enabledFlagsField;

    public CreateServerScreen(ClientGame game) {
        super(game);
    }

    @Override
    public void show() {
        super.show();
        createFields();

        buttons.clear();
        buttons.add(new MenuButton(80f, 55f, 230f, 44f, "Save Profile", this::saveProfile));
        buttons.add(new MenuButton(330f, 55f, 260f, 44f, "Save + Connect", () -> {
            try {
                ServerProfile profile = saveProfileInternal();
                game.startGame(ClientConfig.fromServerProfile(profile, ClientGame.newClientId()));
            } catch (Exception e) {
                messageColor = new Color(1f, 0.45f, 0.45f, 1f);
                message = "Could not create profile: " + e.getMessage();
            }
        }));
        buttons.add(new MenuButton(610f, 55f, 220f, 44f, "Back", () -> game.setScreen(new MultiplayerMenuScreen(game))));

        setActiveField(0);
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                for (int i = 0; i < fields.size(); i++) {
                    if (fields.get(i).containsScreenPoint(screenX, screenY)) {
                        setActiveField(i);
                        return true;
                    }
                }
                return handleButtonClick(screenX, screenY);
            }

            @Override
            public boolean keyTyped(char character) {
                fields.get(activeFieldIndex).keyTyped(character);
                return true;
            }

            @Override
            public boolean keyDown(int keycode) {
                if (fields.get(activeFieldIndex).keyDown(keycode)) {
                    return true;
                }
                if (keycode == Input.Keys.TAB) {
                    setActiveField((activeFieldIndex + 1) % fields.size());
                    return true;
                }
                if (keycode == Input.Keys.ESCAPE) {
                    game.setScreen(new MultiplayerMenuScreen(game));
                    return true;
                }
                return false;
            }
        });
    }

    private void createFields() {
        fields.clear();
        WorldSettings defaults = WorldSettings.defaults();
        String defaultFlags = flagsToCsv(defaults);

        float leftX = 80f;
        float rightX = 510f;
        float startY = 455f;
        float gapY = 47f;
        float width = 360f;
        float height = 31f;

        serverNameField = new TextInputField("Server/profile name", "My Local Server", leftX, startY, width, height);
        hostField = new TextInputField("Host/IP", "localhost", leftX, startY - gapY, width, height);
        portField = new TextInputField("RabbitMQ port", "5672", leftX, startY - gapY * 2, width, height, false, true);
        roomField = new TextInputField("Room ID", "1", leftX, startY - gapY * 3, width, height);
        roomPasswordField = new TextInputField("Room password (prepared)", "", leftX, startY - gapY * 4, width, height, true, false);
        worldNameField = new TextInputField("World name", defaults.getWorldName(), leftX, startY - gapY * 5, width, height);
        gameModeField = new TextInputField("Game mode", defaults.getGameMode(), leftX, startY - gapY * 6, width, height);
        mapModeField = new TextInputField("Map mode: PROCEDURAL or FILE", defaults.getMapMode(), leftX, startY - gapY * 7, width, height);

        mapIdField = new TextInputField("Map ID", defaults.getMapId(), rightX, startY, width, height);
        mapFileField = new TextInputField("Map file path (for FILE mode)", defaults.getMapFile(), rightX, startY - gapY, width, height);
        mapWidthField = new TextInputField("Map width", String.valueOf(defaults.getMapWidth()), rightX, startY - gapY * 2, width, height, false, true);
        mapHeightField = new TextInputField("Map height", String.valueOf(defaults.getMapHeight()), rightX, startY - gapY * 3, width, height, false, true);
        seedField = new TextInputField("Seed (0 = room based)", String.valueOf(defaults.getSeed()), rightX, startY - gapY * 4, width, height, false, true);
        moveIntervalField = new TextInputField("Client move interval seconds", String.valueOf(defaults.getMovementRepeatIntervalSeconds()), rightX, startY - gapY * 5, width, height);
        moveCooldownTicksField = new TextInputField("Server move cooldown ticks", String.valueOf(defaults.getMovementCooldownTicks()), rightX, startY - gapY * 6, width, height, false, true);
        enabledFlagsField = new TextInputField("Enabled feature flags CSV", defaultFlags, rightX, startY - gapY * 7, width, height);

        fields.add(serverNameField);
        fields.add(hostField);
        fields.add(portField);
        fields.add(roomField);
        fields.add(roomPasswordField);
        fields.add(worldNameField);
        fields.add(gameModeField);
        fields.add(mapModeField);
        fields.add(mapIdField);
        fields.add(mapFileField);
        fields.add(mapWidthField);
        fields.add(mapHeightField);
        fields.add(seedField);
        fields.add(moveIntervalField);
        fields.add(moveCooldownTicksField);
        fields.add(enabledFlagsField);
    }

    private void setActiveField(int index) {
        activeFieldIndex = index;
        for (int i = 0; i < fields.size(); i++) {
            fields.get(i).setActive(i == activeFieldIndex);
        }
    }

    private void saveProfile() {
        try {
            ServerProfile profile = saveProfileInternal();
            messageColor = Color.LIGHT_GRAY;
            message = "Server profile + world settings JSON saved. Settings: "
                    + worldSettingsFileRepository.resolveRoomSettingsPath(profile.getRoomId());
        } catch (Exception e) {
            messageColor = new Color(1f, 0.45f, 0.45f, 1f);
            message = "Could not save profile: " + e.getMessage();
        }
    }

    private ServerProfile saveProfileInternal() {
        WorldSettings settings = buildWorldSettings();
        ServerProfile profile = new ServerProfile(
                serverNameField.getValue(),
                hostField.getValue(),
                parsePort(portField.getValue()),
                "guest",
                "guest",
                "/",
                roomField.getValue(),
                roomPasswordField.getValue(),
                settings
        ).normalizedCopy();
        repository.upsert(profile);
        return profile;
    }

    private WorldSettings buildWorldSettings() {
        WorldSettings settings = WorldSettings.defaults();
        settings.setWorldName(worldNameField.getValue());
        settings.setGameMode(gameModeField.getValue());
        settings.setMapMode(mapModeField.getValue());
        settings.setMapId(mapIdField.getValue());
        settings.setMapFile(mapFileField.getValue());
        settings.setMapWidth(parseInt(mapWidthField.getValue(), settings.getMapWidth()));
        settings.setMapHeight(parseInt(mapHeightField.getValue(), settings.getMapHeight()));
        settings.setSeed(parseLong(seedField.getValue(), settings.getSeed()));
        settings.setMovementRepeatIntervalSeconds(parseFloat(moveIntervalField.getValue(), settings.getMovementRepeatIntervalSeconds()));
        settings.setMovementCooldownTicks(parseInt(moveCooldownTicksField.getValue(), settings.getMovementCooldownTicks()));
        applyFlags(settings, enabledFlagsField.getValue());
        return settings.normalizedCopy();
    }

    private void applyFlags(WorldSettings settings, String csv) {
        Set<String> enabled = csvToSet(csv);
        settings.setMovementEnabled(enabled.contains("movement"));
        settings.setPlayerInteractionEnabled(enabled.contains("interaction"));
        settings.setInventoryEnabled(enabled.contains("inventory"));
        settings.setBuildingEnabled(enabled.contains("building"));
        settings.setCraftingEnabled(enabled.contains("crafting"));
        settings.setCraftingMenuEnabled(enabled.contains("craftingmenu"));
        settings.setInventoryCraftingEnabled(enabled.contains("inventorycrafting"));
        settings.setWorkstationCraftingEnabled(enabled.contains("workstationcrafting"));
        settings.setToolCraftingEnabled(enabled.contains("toolcrafting"));
        settings.setCraftingQualityEnabled(enabled.contains("craftingquality"));
        settings.setCraftingFailureEnabled(enabled.contains("craftingfailure"));
        settings.setCombatEnabled(enabled.contains("combat"));
        settings.setTeamCallEnabled(enabled.contains("teamcall"));
        settings.setCompanionsEnabled(enabled.contains("companions"));
        settings.setGroupsEnabled(enabled.contains("groups"));
        settings.setBibblesEnabled(enabled.contains("bibbles"));
        settings.setBibbleSpawningEnabled(enabled.contains("bibblespawning"));
        settings.setBibbleCapturingEnabled(enabled.contains("bibblecapturing"));
        settings.setBibbleTradingEnabled(enabled.contains("bibbletrading"));
        settings.setBibbleWorkstationAutomationEnabled(enabled.contains("bibbleworkstationautomation"));
        settings.setBibbleCombatEnabled(enabled.contains("bibblecombat"));
        settings.setBibbleFriendlyFireEnabled(enabled.contains("bibblefriendlyfire"));
        settings.setBibblePermanentDeathEnabled(enabled.contains("bibblepermanentdeath"));
        settings.setBibbleFreeRoamEnabled(enabled.contains("bibblefreeroam"));
        settings.setBibbleTerminalEnabled(enabled.contains("bibbleterminal"));
        settings.setBibbleAreasEnabled(enabled.contains("bibbleareas"));
        settings.setStarterBibblesEnabled(enabled.contains("starterbibbles"));
        settings.setWorldItemsEnabled(enabled.contains("worlditems"));
        settings.setMapEditingEnabled(enabled.contains("mapediting"));
        settings.setSettlementsEnabled(enabled.contains("settlements"));
        settings.setStarterItemsEnabled(enabled.contains("starteritems"));
    }

    private String flagsToCsv(WorldSettings settings) {
        List<String> flags = new ArrayList<>();
        if (settings.isMovementEnabled()) flags.add("movement");
        if (settings.isPlayerInteractionEnabled()) flags.add("interaction");
        if (settings.isInventoryEnabled()) flags.add("inventory");
        if (settings.isBuildingEnabled()) flags.add("building");
        if (settings.isCraftingEnabled()) flags.add("crafting");
        if (settings.isCraftingMenuEnabled()) flags.add("craftingMenu");
        if (settings.isInventoryCraftingEnabled()) flags.add("inventoryCrafting");
        if (settings.isWorkstationCraftingEnabled()) flags.add("workstationCrafting");
        if (settings.isToolCraftingEnabled()) flags.add("toolCrafting");
        if (settings.isCraftingQualityEnabled()) flags.add("craftingQuality");
        if (settings.isCraftingFailureEnabled()) flags.add("craftingFailure");
        if (settings.isCombatEnabled()) flags.add("combat");
        if (settings.isTeamCallEnabled()) flags.add("teamCall");
        if (settings.isCompanionsEnabled()) flags.add("companions");
        if (settings.isGroupsEnabled()) flags.add("groups");
        if (settings.isBibblesEnabled()) flags.add("bibbles");
        if (settings.isBibbleSpawningEnabled()) flags.add("bibbleSpawning");
        if (settings.isBibbleCapturingEnabled()) flags.add("bibbleCapturing");
        if (settings.isBibbleTradingEnabled()) flags.add("bibbleTrading");
        if (settings.isBibbleWorkstationAutomationEnabled()) flags.add("bibbleWorkstationAutomation");
        if (settings.isBibbleCombatEnabled()) flags.add("bibbleCombat");
        if (settings.isBibbleFriendlyFireEnabled()) flags.add("bibbleFriendlyFire");
        if (settings.isBibblePermanentDeathEnabled()) flags.add("bibblePermanentDeath");
        if (settings.isBibbleFreeRoamEnabled()) flags.add("bibbleFreeRoam");
        if (settings.isBibbleTerminalEnabled()) flags.add("bibbleTerminal");
        if (settings.isBibbleAreasEnabled()) flags.add("bibbleAreas");
        if (settings.isStarterBibblesEnabled()) flags.add("starterBibbles");
        if (settings.isWorldItemsEnabled()) flags.add("worldItems");
        if (settings.isMapEditingEnabled()) flags.add("mapEditing");
        if (settings.isSettlementsEnabled()) flags.add("settlements");
        if (settings.isStarterItemsEnabled()) flags.add("starterItems");
        return String.join(",", flags);
    }

    private Set<String> csvToSet(String csv) {
        Set<String> result = new LinkedHashSet<>();
        if (csv == null || csv.isBlank()) {
            return result;
        }
        for (String raw : csv.split(",")) {
            String normalized = raw.trim().toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
            if (!normalized.isBlank()) {
                result.add(normalized);
            }
        }
        return result;
    }

    private int parsePort(String value) {
        int parsed = parseInt(value, 5672);
        if (parsed <= 0 || parsed > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
        return parsed;
    }

    private int parseInt(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return Integer.parseInt(value.trim());
    }

    private long parseLong(String value, long fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return Long.parseLong(value.trim());
    }

    private float parseFloat(String value, float fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return Float.parseFloat(value.trim());
    }

    @Override
    public void render(float delta) {
        clearBackground();
        drawTitle("Create Server + Advanced World Settings", "Saves connection profile and authoritative world-settings JSON for the selected room.");
        drawPanel(50f, 45f, Gdx.graphics.getWidth() - 100f, Gdx.graphics.getHeight() - 150f);

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (TextInputField field : fields) {
            field.drawShape(shapes);
        }
        shapes.end();

        batch.begin();
        font.getData().setScale(0.82f);
        for (TextInputField field : fields) {
            field.drawText(batch, font);
        }
        font.setColor(new Color(0.78f, 0.82f, 0.90f, 1f));
        font.draw(batch, "Supported flags: " + SUPPORTED_FLAGS, 80f, 110f);
        font.draw(batch, "Local server reads: ~/.clientgame/world-settings/<roomId>.json. For hosted servers use --worldSettings=<path>.", 80f, 132f);
        batch.end();

        drawButtons();
        drawMessage(850f, 90f);
    }
}
