package com.tim.game.shared.DTOs.input;

/**
 * Wenn spieler bauen platzieren wollen
 * buildingType → welcher Gebäudetyp (z. B. "GENERATOR", "TURRET", "FURNACE")
 * tileX, tileY → Rasterkoordinaten (Tile-Position), damit der Server weiß, wo gebaut werden soll
 */


/**
 * DTO für Bau-/Platzierungs-Eingaben des Clients.
 * Beispiel: "Platziere einen 'GENERATOR' auf Tile (10, 5)".
 */
public class BuildInputDto {

    /**
     * Typ des zu bauenden Objekts, z.B.:
     * - "GENERATOR"
     * - "TURRET"
     * - "FURNACE"
     */
    private String buildingType;

    /**
     * Zielposition auf dem Tile-Grid.
     */
    private int tileX;
    private int tileY;

    public BuildInputDto() {
    }

    public BuildInputDto(String buildingType, int tileX, int tileY) {
        this.buildingType = buildingType;
        this.tileX = tileX;
        this.tileY = tileY;
    }
    public String getBuildingType() {
        return buildingType;
    }

    public void setBuildingType(String buildingType) {
        this.buildingType = buildingType;
    }

    public int getTileX() {
        return tileX;
    }

    public void setTileX(int tileX) {
        this.tileX = tileX;
    }

    public int getTileY() {
        return tileY;
    }

    public void setTileY(int tileY) {
        this.tileY = tileY;
    }

    @Override
    public String toString() {
        return "BuildInputDto{" +
                "buildingType='" + buildingType + '\'' +
                ", tileX=" + tileX +
                ", tileY=" + tileY +
                '}';
    }
}