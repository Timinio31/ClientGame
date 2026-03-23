package com.tim.game.client;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public class ClientDesktopLauncher {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setTitle("ClientGame");
        cfg.setWindowedMode(1280, 720);
        cfg.useVsync(true);

        new Lwjgl3Application(new ClientGame(), cfg);
    }
}
