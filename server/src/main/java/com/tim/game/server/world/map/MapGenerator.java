package com.tim.game.server.world.map;

public interface MapGenerator {

    GeneratedMap generate(String roomId, int width, int height, float tileSize, long seed);
}