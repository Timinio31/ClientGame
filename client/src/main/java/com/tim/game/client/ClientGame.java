package com.tim.game.client;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.tim.game.client.config.ClientSettings;
import com.tim.game.client.config.ClientSettingsRepository;
import com.tim.game.client.net.ClientConfig;
import com.tim.game.client.screen.GameScreen;
import com.tim.game.client.screen.MainMenuScreen;
import com.tim.game.client.screen.SettingsScreen;
import com.tim.game.client.texture.TexturePackManager;

public class ClientGame extends Game {
    private final ClientSettingsRepository settingsRepository = new ClientSettingsRepository();
    private final TexturePackManager texturePackManager = TexturePackManager.getInstance();
    private ClientSettings clientSettings;

    @Override
    public void create() {
        clientSettings = settingsRepository.load();
        texturePackManager.initialize(clientSettings.getTexturePackId());
        showMainMenu();
    }

    @Override
    public void setScreen(Screen screen) {
        Screen previousScreen = getScreen();
        super.setScreen(screen);
        if (previousScreen != null) {
            previousScreen.dispose();
        }
    }

    public void showMainMenu() {
        setScreen(new MainMenuScreen(this));
    }

    public void showSettings() {
        setScreen(new SettingsScreen(this));
    }

    public void startGame(ClientConfig config) {
        setScreen(new GameScreen(this, config));
    }

    public ClientSettings getClientSettings() {
        if (clientSettings == null) {
            clientSettings = settingsRepository.load();
        }
        return clientSettings;
    }

    public void saveClientSettings(ClientSettings settings) {
        clientSettings = settings == null ? new ClientSettings() : settings.normalizedCopy();
        settingsRepository.save(clientSettings);
    }

    public ClientSettingsRepository getSettingsRepository() {
        return settingsRepository;
    }

    public TexturePackManager getTexturePackManager() {
        return texturePackManager;
    }

    @Override
    public void dispose() {
        super.dispose();
        texturePackManager.dispose();
    }

    public static String newClientId() {
        return "c" + System.currentTimeMillis();
    }
}
