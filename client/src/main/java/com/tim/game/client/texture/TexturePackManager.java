package com.tim.game.client.texture;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tim.game.client.config.ClientSettings;
import com.tim.game.client.config.ClientSettingsRepository;
import com.tim.game.shared.inventory.ItemCatalog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TexturePackManager {
    public static final String CLASSIC_PACK_ID = "classic_debug";
    public static final String BUILT_IN_1950_PACK_ID = "1950_civilized_cartoon";
    public static final String BUILT_IN_PLATINUM_PACK_ID = "midcentury_platinum";

    private static final TexturePackManager INSTANCE = new TexturePackManager();

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final ClientSettingsRepository settingsRepository = new ClientSettingsRepository();
    private final Map<String, TexturePackDefinition> availablePacks = new LinkedHashMap<>();
    private final Map<String, Texture> textures = new HashMap<>();
    private final Map<String, List<Texture>> animations = new HashMap<>();
    private final List<Texture> ownedTextures = new ArrayList<>();

    private TexturePackDefinition activePack;
    private boolean initialized;

    private TexturePackManager() {
    }

    public static TexturePackManager getInstance() {
        return INSTANCE;
    }

    public void initialize(String preferredPackId) {
        refreshAvailablePacks();
        initialized = true;
        setActivePack(preferredPackId == null || preferredPackId.isBlank()
                ? ClientSettings.DEFAULT_TEXTURE_PACK_ID
                : preferredPackId);
    }

    public void refreshAvailablePacks() {
        availablePacks.clear();
        registerBuiltInPacks();
        registerExternalPacks();
    }

    private void registerBuiltInPacks() {
        availablePacks.put(CLASSIC_PACK_ID, new TexturePackDefinition(
                CLASSIC_PACK_ID,
                "Classic Debug Shapes",
                "Simple generated colors close to the first prototype renderer.",
                false,
                true,
                null
        ));

        availablePacks.put(BUILT_IN_1950_PACK_ID, new TexturePackDefinition(
                BUILT_IN_1950_PACK_ID,
                "1950 Civilized Cartoon",
                "Warm 1950s town colors, slightly cartoon/Pokemon inspired, not high-modern.",
                true,
                false,
                null
        ));

        availablePacks.put(BUILT_IN_PLATINUM_PACK_ID, new TexturePackDefinition(
                BUILT_IN_PLATINUM_PACK_ID,
                "Midcentury Platinum",
                "Polished top-down cartoon pack inspired by Pokemon Platinum, with a pink-dressed heroine, richer terrain and varied town buildings.",
                true,
                false,
                null
        ));
    }

    private void registerExternalPacks() {
        Path root = getExternalTexturePackFolder();
        try {
            Files.createDirectories(root);
            try (var stream = Files.list(root)) {
                stream.filter(Files::isDirectory).forEach(this::registerExternalPackFolder);
            }
        } catch (IOException exception) {
            System.err.println("[TexturePackManager] Could not scan external texture packs: " + exception.getMessage());
        }
    }

    private void registerExternalPackFolder(Path folder) {
        Path manifestPath = folder.resolve("manifest.json");
        if (!Files.exists(manifestPath)) {
            return;
        }

        try {
            TexturePackManifest manifest = mapper.readValue(manifestPath.toFile(), TexturePackManifest.class);
            String id = defaultIfBlank(manifest.getId(), folder.getFileName().toString());
            String displayName = defaultIfBlank(manifest.getDisplayName(), id);
            String description = defaultIfBlank(manifest.getDescription(), "External texture pack from " + folder);

            availablePacks.put(id, new TexturePackDefinition(
                    id,
                    displayName,
                    description,
                    false,
                    false,
                    folder
            ));
        } catch (IOException exception) {
            System.err.println("[TexturePackManager] Invalid texture pack manifest in " + folder + ": " + exception.getMessage());
        }
    }

    public void setActivePack(String packId) {
        if (!initialized) {
            initialize(packId);
            return;
        }

        TexturePackDefinition selected = availablePacks.get(packId);
        if (selected == null) {
            selected = availablePacks.get(ClientSettings.DEFAULT_TEXTURE_PACK_ID);
        }
        if (selected == null) {
            selected = availablePacks.get(CLASSIC_PACK_ID);
        }

        disposeTextures();
        activePack = selected;
        loadTexturesForActivePack();
        System.out.println("[TexturePackManager] Active texture pack: " + activePack.getDisplayName());
    }

    public void reloadActivePack() {
        String activeId = activePack == null ? ClientSettings.DEFAULT_TEXTURE_PACK_ID : activePack.getId();
        refreshAvailablePacks();
        setActivePack(activeId);
    }

    public Texture getTexture(String key) {
        Texture texture = textures.get(key);
        if (texture == null) {
            texture = createFallbackTexture(key);
            registerOwnedTexture(key, texture);
        }
        return texture;
    }

    public Texture getTileTexture(String tileType, float stateTime) {
        String normalizedType = tileType == null ? "GRASS" : tileType.trim();
        return switch (normalizedType) {
            case "WATER" -> getAnimationFrame(TextureKeys.TILE_WATER, stateTime, 0.28f);
            case "WALL" -> getTexture(TextureKeys.TILE_WALL);
            case "SPAWN" -> getTexture(TextureKeys.TILE_SPAWN);
            case "FOREST" -> getTexture(TextureKeys.TILE_FOREST);
            case "MOUNTAIN" -> getTexture(TextureKeys.TILE_MOUNTAIN);
            case "ROAD" -> getTexture(TextureKeys.TILE_ROAD);
            case "GRASS" -> getTexture(TextureKeys.TILE_GRASS);
            default -> getTexture(TextureKeys.TILE_GRASS);
        };
    }

    public Texture getPlayerTexture(boolean localPlayer, String direction, boolean moving, float stateTime) {
        String dir = normalizeDirection(direction);
        String base = (localPlayer ? TextureKeys.PLAYER_LOCAL : TextureKeys.PLAYER_REMOTE)
                + "_" + (moving ? "walk" : "idle") + "_" + dir;

        if (moving) {
            return getAnimationFrame(base, stateTime, 0.20f);
        }
        Texture idle = textures.get(base);
        if (idle != null) {
            return idle;
        }
        return getAnimationFrame(base, stateTime, 0.50f);
    }

    public Texture getBuildingTexture(String buildingType, float stateTime) {
        String type = buildingType == null ? "DEFAULT" : buildingType.trim().toUpperCase();
        return switch (type) {
            case "GENERATOR" -> getTexture(TextureKeys.BUILDING_GENERATOR);
            case "CITY_HOUSE_SMALL" -> getTexture(TextureKeys.BUILDING_CITY_HOUSE_SMALL);
            case "CITY_HOUSE_TALL" -> getTexture(TextureKeys.BUILDING_CITY_HOUSE_TALL);
            case "CITY_SHOP" -> getTexture(TextureKeys.BUILDING_CITY_SHOP);
            case "CITY_HALL" -> getTexture(TextureKeys.BUILDING_CITY_HALL);
            case "CITY_WAREHOUSE" -> getTexture(TextureKeys.BUILDING_CITY_WAREHOUSE);
            default -> getTexture(TextureKeys.BUILDING_DEFAULT);
        };
    }


    public Texture getItemTexture(String itemType) {
        String type = ItemCatalog.normalizeType(itemType);
        return switch (type) {
            case ItemCatalog.STONE -> getTexture(TextureKeys.ITEM_STONE);
            case ItemCatalog.BERRY -> getTexture(TextureKeys.ITEM_BERRY);
            case ItemCatalog.GENERATOR_KIT -> getTexture(TextureKeys.ITEM_GENERATOR_KIT);
            default -> getTexture(TextureKeys.ITEM_WOOD);
        };
    }

    public List<TexturePackDefinition> getAvailablePacks() {
        return Collections.unmodifiableList(new ArrayList<>(availablePacks.values()));
    }

    public TexturePackDefinition getActivePack() {
        return activePack;
    }

    public Path getExternalTexturePackFolder() {
        return settingsRepository.getTexturePackFolder();
    }

    public boolean isActive(String packId) {
        return activePack != null && activePack.getId().equals(packId);
    }

    public void dispose() {
        disposeTextures();
        initialized = false;
        activePack = null;
    }

    private void loadTexturesForActivePack() {
        loadTexture(TextureKeys.TILE_GRASS, "tiles/grass.png");
        loadAnimatedOrStatic(TextureKeys.TILE_WATER, new String[]{"tiles/water_0.png", "tiles/water_1.png", "tiles/water_2.png"}, "tiles/water.png");
        loadTexture(TextureKeys.TILE_WALL, "tiles/wall.png");
        loadTexture(TextureKeys.TILE_SPAWN, "tiles/spawn.png");
        loadTexture(TextureKeys.TILE_FOREST, "tiles/forest.png");
        loadTexture(TextureKeys.TILE_MOUNTAIN, "tiles/mountain.png");
        loadTexture(TextureKeys.TILE_ROAD, "tiles/road.png");

        loadPlayerSet(true);
        loadPlayerSet(false);

        loadTexture(TextureKeys.BUILDING_GENERATOR, "entities/building_generator.png");
        loadTexture(TextureKeys.BUILDING_DEFAULT, "entities/building_default.png");
        loadTexture(TextureKeys.BUILDING_CITY_HOUSE_SMALL, "entities/building_city_house_small.png");
        loadTexture(TextureKeys.BUILDING_CITY_HOUSE_TALL, "entities/building_city_house_tall.png");
        loadTexture(TextureKeys.BUILDING_CITY_SHOP, "entities/building_city_shop.png");
        loadTexture(TextureKeys.BUILDING_CITY_HALL, "entities/building_city_hall.png");
        loadTexture(TextureKeys.BUILDING_CITY_WAREHOUSE, "entities/building_city_warehouse.png");

        loadTexture(TextureKeys.ITEM_WOOD, "items/wood.png");
        loadTexture(TextureKeys.ITEM_STONE, "items/stone.png");
        loadTexture(TextureKeys.ITEM_BERRY, "items/berry.png");
        loadTexture(TextureKeys.ITEM_GENERATOR_KIT, "items/generator_kit.png");
    }

    private void loadPlayerSet(boolean local) {
        String prefix = local ? TextureKeys.PLAYER_LOCAL : TextureKeys.PLAYER_REMOTE;
        String folderPrefix = local ? "player_local" : "player_remote";
        String[] directions = new String[]{TextureKeys.DIR_DOWN, TextureKeys.DIR_UP, TextureKeys.DIR_LEFT, TextureKeys.DIR_RIGHT};

        for (String direction : directions) {
            loadTexture(prefix + "_idle_" + direction, "entities/" + folderPrefix + "_idle_" + direction + ".png");
            loadAnimatedOrStatic(prefix + "_walk_" + direction,
                    new String[]{
                            "entities/" + folderPrefix + "_walk_" + direction + "_0.png",
                            "entities/" + folderPrefix + "_walk_" + direction + "_1.png"
                    },
                    "entities/" + folderPrefix + "_idle_" + direction + ".png");
        }
    }

    private void loadAnimatedOrStatic(String animationKey, String[] candidateFrames, String fallbackPath) {
        List<Texture> frames = new ArrayList<>();
        for (String candidate : candidateFrames) {
            Texture texture = loadTextureInternal(candidate);
            if (texture != null) {
                frames.add(texture);
            }
        }

        if (!frames.isEmpty()) {
            animations.put(animationKey, frames);
            textures.put(animationKey, frames.get(0));
            return;
        }

        Texture fallbackTexture = loadTextureInternal(fallbackPath);
        if (fallbackTexture != null) {
            List<Texture> fallbackFrames = new ArrayList<>();
            fallbackFrames.add(fallbackTexture);
            animations.put(animationKey, fallbackFrames);
            textures.put(animationKey, fallbackTexture);
            return;
        }

        Texture generatedFallback = createFallbackTexture(animationKey);
        registerOwnedTexture(animationKey, generatedFallback);
        List<Texture> generatedFrames = new ArrayList<>();
        generatedFrames.add(generatedFallback);
        animations.put(animationKey, generatedFrames);
    }

    private void loadTexture(String key, String relativePath) {
        Texture texture = loadTextureInternal(relativePath);
        if (texture == null) {
            texture = createFallbackTexture(key);
        }
        registerOwnedTexture(key, texture);
    }

    private Texture loadTextureInternal(String relativePath) {
        if (activePack == null || activePack.isGenerated()) {
            return null;
        }

        FileHandle fileHandle = activePack.isBuiltIn()
                ? Gdx.files.internal("texture-packs/" + activePack.getId() + "/" + relativePath)
                : Gdx.files.absolute(activePack.getRootPath().resolve(relativePath).toString());

        if (!fileHandle.exists()) {
            return null;
        }

        Texture texture = new Texture(fileHandle);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        ownedTextures.add(texture);
        return texture;
    }

    private void registerOwnedTexture(String key, Texture texture) {
        if (!ownedTextures.contains(texture)) {
            ownedTextures.add(texture);
        }
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        textures.put(key, texture);
    }

    private Texture getAnimationFrame(String animationKey, float stateTime, float frameDuration) {
        List<Texture> frames = animations.get(animationKey);
        if (frames == null || frames.isEmpty()) {
            Texture fallback = textures.get(animationKey);
            if (fallback != null) {
                return fallback;
            }
            return getTexture(animationKey);
        }
        int index = (int) Math.floor(stateTime / Math.max(0.01f, frameDuration)) % frames.size();
        return frames.get(index);
    }

    private Texture createFallbackTexture(String key) {
        Pixmap pixmap = key.startsWith("building_city_") || key.startsWith("building_") ? new Pixmap(64, 80, Pixmap.Format.RGBA8888) : new Pixmap(32, 32, Pixmap.Format.RGBA8888);

        if (key.startsWith(TextureKeys.TILE_WATER)) {
            fill(pixmap, new Color(0.23f, 0.46f, 0.84f, 1f));
            drawLinePattern(pixmap, new Color(0.68f, 0.83f, 1.0f, 1f));
        } else if (TextureKeys.TILE_WALL.equals(key)) {
            fill(pixmap, new Color(0.34f, 0.33f, 0.38f, 1f));
            drawBrickLines(pixmap, new Color(0.20f, 0.20f, 0.22f, 1f));
        } else if (TextureKeys.TILE_SPAWN.equals(key)) {
            fill(pixmap, new Color(0.90f, 0.78f, 0.46f, 1f));
            drawBorder(pixmap, new Color(0.75f, 0.56f, 0.20f, 1f));
        } else if (TextureKeys.TILE_FOREST.equals(key)) {
            fill(pixmap, new Color(0.34f, 0.56f, 0.26f, 1f));
            drawForestCanopy(pixmap);
        } else if (TextureKeys.TILE_MOUNTAIN.equals(key)) {
            fill(pixmap, new Color(0.55f, 0.57f, 0.62f, 1f));
            drawMountainFacet(pixmap);
        } else if (TextureKeys.TILE_ROAD.equals(key)) {
            fill(pixmap, new Color(0.68f, 0.57f, 0.40f, 1f));
            drawRoadDetail(pixmap);
        } else if (key.startsWith("player_")) {
            fill(pixmap, new Color(0f, 0f, 0f, 0f));
            drawFallbackHero(pixmap, key.contains("remote"), key.contains("left") ? TextureKeys.DIR_LEFT : key.contains("right") ? TextureKeys.DIR_RIGHT : key.contains("up") ? TextureKeys.DIR_UP : TextureKeys.DIR_DOWN, key.contains("walk") ? (key.endsWith("_1") ? 1 : 0) : -1);
        } else if (key.startsWith("item_")) {
            fill(pixmap, new Color(0f, 0f, 0f, 0f));
            drawFallbackItem(pixmap, key);
        } else if (TextureKeys.BUILDING_GENERATOR.equals(key)) {
            fill(pixmap, new Color(0f, 0f, 0f, 0f));
            drawFallbackBuilding(pixmap, new Color(0.74f, 0.59f, 0.38f, 1f), new Color(0.49f, 0.25f, 0.19f, 1f), "generator");
        } else if (TextureKeys.BUILDING_CITY_HOUSE_SMALL.equals(key)) {
            fill(pixmap, new Color(0f, 0f, 0f, 0f));
            drawFallbackBuilding(pixmap, new Color(0.84f, 0.69f, 0.58f, 1f), new Color(0.66f, 0.29f, 0.25f, 1f), "small");
        } else if (TextureKeys.BUILDING_CITY_HOUSE_TALL.equals(key)) {
            fill(pixmap, new Color(0f, 0f, 0f, 0f));
            drawFallbackBuilding(pixmap, new Color(0.82f, 0.76f, 0.67f, 1f), new Color(0.48f, 0.33f, 0.24f, 1f), "tall");
        } else if (TextureKeys.BUILDING_CITY_SHOP.equals(key)) {
            fill(pixmap, new Color(0f, 0f, 0f, 0f));
            drawFallbackBuilding(pixmap, new Color(0.98f, 0.85f, 0.70f, 1f), new Color(0.84f, 0.41f, 0.46f, 1f), "shop");
        } else if (TextureKeys.BUILDING_CITY_HALL.equals(key)) {
            fill(pixmap, new Color(0f, 0f, 0f, 0f));
            drawFallbackBuilding(pixmap, new Color(0.78f, 0.84f, 0.90f, 1f), new Color(0.56f, 0.32f, 0.30f, 1f), "hall");
        } else if (TextureKeys.BUILDING_CITY_WAREHOUSE.equals(key)) {
            fill(pixmap, new Color(0f, 0f, 0f, 0f));
            drawFallbackBuilding(pixmap, new Color(0.73f, 0.69f, 0.60f, 1f), new Color(0.42f, 0.29f, 0.22f, 1f), "warehouse");
        } else if (TextureKeys.BUILDING_DEFAULT.equals(key)) {
            fill(pixmap, new Color(0f, 0f, 0f, 0f));
            drawFallbackBuilding(pixmap, new Color(0.80f, 0.71f, 0.60f, 1f), new Color(0.50f, 0.25f, 0.21f, 1f), "small");
        } else {
            fill(pixmap, new Color(0.26f, 0.62f, 0.28f, 1f));
            drawChecker(pixmap, new Color(0.30f, 0.69f, 0.31f, 1f));
        }

        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pixmap.dispose();
        return texture;
    }

    private void disposeTextures() {
        for (Texture texture : ownedTextures) {
            if (texture != null) {
                texture.dispose();
            }
        }
        ownedTextures.clear();
        textures.clear();
        animations.clear();
    }

    private void fill(Pixmap pixmap, Color color) {
        pixmap.setColor(color);
        pixmap.fill();
    }

    private void drawChecker(Pixmap pixmap, Color color) {
        pixmap.setColor(color);
        for (int y = 0; y < pixmap.getHeight(); y += 8) {
            for (int x = (y / 8) % 2 == 0 ? 0 : 8; x < pixmap.getWidth(); x += 16) {
                pixmap.fillRectangle(x, y, Math.min(8, pixmap.getWidth() - x), Math.min(8, pixmap.getHeight() - y));
            }
        }
    }

    private void drawLinePattern(Pixmap pixmap, Color color) {
        pixmap.setColor(color);
        for (int y = 6; y < pixmap.getHeight(); y += 9) {
            pixmap.drawLine(2, y, pixmap.getWidth() - 3, Math.min(pixmap.getHeight() - 2, y + 2));
        }
    }

    private void drawBrickLines(Pixmap pixmap, Color color) {
        pixmap.setColor(color);
        for (int y = 8; y < pixmap.getHeight(); y += 8) {
            pixmap.drawLine(0, y, pixmap.getWidth() - 1, y);
        }
        for (int x = 8; x < pixmap.getWidth(); x += 16) {
            pixmap.drawLine(x, 0, x, pixmap.getHeight() / 2);
            pixmap.drawLine(x - 8, pixmap.getHeight() / 2, x - 8, pixmap.getHeight() - 1);
        }
    }

    private void drawBorder(Pixmap pixmap, Color color) {
        pixmap.setColor(color);
        pixmap.drawRectangle(1, 1, pixmap.getWidth() - 2, pixmap.getHeight() - 2);
        pixmap.drawRectangle(3, 3, pixmap.getWidth() - 6, pixmap.getHeight() - 6);
    }

    private void drawForestCanopy(Pixmap pixmap) {
        pixmap.setColor(new Color(0.18f, 0.33f, 0.16f, 1f));
        pixmap.fillCircle(10, 16, 7);
        pixmap.fillCircle(18, 17, 8);
        pixmap.fillCircle(14, 23, 6);
        pixmap.setColor(new Color(0.31f, 0.50f, 0.24f, 1f));
        pixmap.fillCircle(14, 17, 5);
    }

    private void drawMountainFacet(Pixmap pixmap) {
        pixmap.setColor(new Color(0.38f, 0.41f, 0.47f, 1f));
        pixmap.fillTriangle(4, 6, 16, 26, 28, 6);
        pixmap.setColor(new Color(0.73f, 0.76f, 0.82f, 1f));
        pixmap.fillTriangle(16, 26, 21, 15, 11, 15);
        pixmap.setColor(new Color(1f, 1f, 1f, 0.85f));
        pixmap.fillTriangle(16, 26, 18, 21, 14, 21);
    }

    private void drawRoadDetail(Pixmap pixmap) {
        pixmap.setColor(new Color(0.54f, 0.44f, 0.29f, 1f));
        for (int y = 3; y < pixmap.getHeight(); y += 8) {
            pixmap.drawLine(0, y, pixmap.getWidth() - 1, y + 1);
        }
    }

    private void drawFallbackHero(Pixmap pixmap, boolean remote, String direction, int walkFrame) {
        Color shadow = new Color(0.08f, 0.08f, 0.08f, 0.35f);
        pixmap.setColor(shadow);
        fillEllipse(pixmap, 8, pixmap.getHeight() - 8, 16, 5);

        Color skin = remote ? new Color(0.92f, 0.76f, 0.60f, 1f) : new Color(0.96f, 0.80f, 0.67f, 1f);
        Color top = new Color(0.12f, 0.12f, 0.14f, 1f);
        Color dress = remote ? new Color(0.88f, 0.56f, 0.78f, 1f) : new Color(0.96f, 0.44f, 0.72f, 1f);
        Color cap = remote ? new Color(0.55f, 0.61f, 0.74f, 1f) : new Color(0.80f, 0.80f, 0.86f, 1f);
        int bob = walkFrame == 1 ? 1 : 0;
        int headX = 16;
        int headY = 11 + bob;

        if (TextureKeys.DIR_LEFT.equals(direction)) {
            headX = 14;
        } else if (TextureKeys.DIR_RIGHT.equals(direction)) {
            headX = 18;
        } else if (TextureKeys.DIR_UP.equals(direction)) {
            headY = 10 + bob;
        }

        pixmap.setColor(dress);
        pixmap.fillTriangle(16, 15 + bob, 10, 25 + bob, 22, 25 + bob);
        pixmap.setColor(top);
        pixmap.fillRectangle(11, 14 + bob, 10, 7);
        pixmap.setColor(skin);
        pixmap.fillCircle(headX, headY, 5);
        pixmap.setColor(cap);
        pixmap.fillCircle(headX, headY + 1, 5);
        pixmap.setColor(new Color(0.20f, 0.20f, 0.24f, 1f));
        if (TextureKeys.DIR_LEFT.equals(direction)) {
            pixmap.fillRectangle(headX - 7, headY - 1, 4, 2);
        } else if (TextureKeys.DIR_RIGHT.equals(direction)) {
            pixmap.fillRectangle(headX + 3, headY - 1, 4, 2);
        } else {
            pixmap.fillRectangle(headX - 2, headY - 4, 4, 2);
        }
        pixmap.setColor(Color.WHITE);
        pixmap.drawPixel(headX - 1, headY + 1);
        pixmap.drawPixel(headX + 1, headY + 1);
        pixmap.setColor(new Color(0.24f, 0.14f, 0.16f, 1f));
        if (TextureKeys.DIR_UP.equals(direction)) {
            pixmap.fillRectangle(headX - 2, headY - 1, 4, 1);
        } else {
            pixmap.fillRectangle(headX - 2, headY - 2, 4, 1);
        }
        pixmap.setColor(top);
        int legOffset = walkFrame == 1 ? 1 : 0;
        pixmap.fillRectangle(12, 24 + legOffset, 3, 6);
        pixmap.fillRectangle(17, 24 + (walkFrame == 0 ? 1 : 0), 3, 6);
    }


    private void drawFallbackItem(Pixmap pixmap, String key) {
        pixmap.setColor(new Color(0.05f, 0.05f, 0.05f, 0.25f));
        fillEllipse(pixmap, 7, 23, 18, 5);

        if (TextureKeys.ITEM_STONE.equals(key)) {
            pixmap.setColor(new Color(0.45f, 0.48f, 0.54f, 1f));
            pixmap.fillCircle(14, 16, 8);
            pixmap.setColor(new Color(0.65f, 0.68f, 0.73f, 1f));
            pixmap.fillCircle(11, 13, 3);
            pixmap.setColor(new Color(0.31f, 0.33f, 0.38f, 1f));
            pixmap.drawLine(8, 18, 20, 11);
        } else if (TextureKeys.ITEM_BERRY.equals(key)) {
            pixmap.setColor(new Color(0.76f, 0.08f, 0.22f, 1f));
            pixmap.fillCircle(13, 17, 6);
            pixmap.fillCircle(18, 17, 6);
            pixmap.setColor(new Color(0.96f, 0.25f, 0.42f, 1f));
            pixmap.fillCircle(12, 15, 2);
            pixmap.setColor(new Color(0.18f, 0.47f, 0.20f, 1f));
            pixmap.fillTriangle(15, 11, 21, 7, 20, 13);
        } else if (TextureKeys.ITEM_GENERATOR_KIT.equals(key)) {
            pixmap.setColor(new Color(0.49f, 0.34f, 0.23f, 1f));
            pixmap.fillRectangle(8, 11, 16, 14);
            pixmap.setColor(new Color(0.78f, 0.62f, 0.39f, 1f));
            pixmap.drawRectangle(8, 11, 16, 14);
            pixmap.setColor(new Color(0.56f, 0.64f, 0.70f, 1f));
            pixmap.fillRectangle(13, 7, 6, 7);
            pixmap.setColor(new Color(1f, 0.84f, 0.26f, 1f));
            pixmap.fillCircle(16, 18, 4);
        } else {
            pixmap.setColor(new Color(0.50f, 0.30f, 0.16f, 1f));
            pixmap.fillRectangle(8, 12, 16, 10);
            pixmap.setColor(new Color(0.72f, 0.49f, 0.27f, 1f));
            pixmap.fillRectangle(10, 10, 12, 14);
            pixmap.setColor(new Color(0.28f, 0.16f, 0.09f, 1f));
            pixmap.drawLine(11, 11, 19, 23);
            pixmap.drawLine(18, 11, 12, 23);
        }
    }

    private void drawFallbackBuilding(Pixmap pixmap, Color wall, Color roof, String style) {
        int width = pixmap.getWidth();
        int height = pixmap.getHeight();
        pixmap.setColor(new Color(0.08f, 0.08f, 0.08f, 0.28f));
        fillEllipse(pixmap, 8, height - 9, width - 16, 6);

        int bodyX = 10;
        int bodyY = 28;
        int bodyW = width - 20;
        int bodyH = height - 42;

        if ("tall".equals(style)) {
            bodyX = 14;
            bodyY = 20;
            bodyW = width - 28;
            bodyH = height - 34;
        } else if ("hall".equals(style)) {
            bodyX = 8;
            bodyY = 24;
            bodyW = width - 16;
            bodyH = height - 36;
        } else if ("warehouse".equals(style)) {
            bodyX = 8;
            bodyY = 34;
            bodyW = width - 16;
            bodyH = height - 46;
        }

        pixmap.setColor(wall);
        pixmap.fillRectangle(bodyX, bodyY, bodyW, bodyH);
        pixmap.setColor(roof);
        if ("shop".equals(style)) {
            pixmap.fillRectangle(bodyX - 2, bodyY - 12, bodyW + 4, 12);
            pixmap.setColor(new Color(1f, 0.93f, 0.82f, 1f));
            for (int x = bodyX - 1; x < bodyX + bodyW + 2; x += 8) {
                pixmap.fillRectangle(x, bodyY - 12, 4, 12);
            }
            pixmap.setColor(roof);
        } else {
            pixmap.fillTriangle(bodyX - 4, bodyY, bodyX + bodyW / 2, bodyY - 14, bodyX + bodyW + 4, bodyY);
        }

        pixmap.setColor(new Color(0.95f, 0.85f, 0.55f, 1f));
        pixmap.fillRectangle(bodyX + 6, bodyY + 8, 8, 8);
        pixmap.fillRectangle(bodyX + bodyW - 14, bodyY + 8, 8, 8);
        if (bodyW > 28) {
            pixmap.fillRectangle(bodyX + bodyW / 2 - 4, bodyY + 8, 8, 8);
        }
        pixmap.setColor(new Color(0.29f, 0.20f, 0.17f, 1f));
        pixmap.fillRectangle(bodyX + bodyW / 2 - 5, bodyY + bodyH - 10, 10, 10);
        if ("generator".equals(style)) {
            pixmap.setColor(new Color(0.53f, 0.61f, 0.70f, 1f));
            pixmap.fillRectangle(bodyX + bodyW - 16, bodyY - 16, 8, 16);
        }
    }

    private void fillEllipse(Pixmap pixmap, int x, int y, int width, int height) {
        float radiusX = width / 2f;
        float radiusY = height / 2f;
        float centerX = x + radiusX;
        float centerY = y + radiusY;

        for (int py = y; py < y + height; py++) {
            for (int px = x; px < x + width; px++) {
                float normalizedX = (px + 0.5f - centerX) / radiusX;
                float normalizedY = (py + 0.5f - centerY) / radiusY;
                if (normalizedX * normalizedX + normalizedY * normalizedY <= 1f) {
                    pixmap.drawPixel(px, py);
                }
            }
        }
    }

    private String normalizeDirection(String direction) {
        if (direction == null) {
            return TextureKeys.DIR_DOWN;
        }
        return switch (direction.trim().toLowerCase()) {
            case TextureKeys.DIR_UP -> TextureKeys.DIR_UP;
            case TextureKeys.DIR_LEFT -> TextureKeys.DIR_LEFT;
            case TextureKeys.DIR_RIGHT -> TextureKeys.DIR_RIGHT;
            default -> TextureKeys.DIR_DOWN;
        };
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
