package com.tim.game.shared.DTOs.input;
/**
 * Allgemeine Aktionen des Spielers
 * Attacke, interaktionen (Kiste öffnen)
 * genreischer ansatz , actions sind mit "ATTACK","INTERACT","USE_ITEM" erweiterbar
 * targetX und TargetY -> positionen auf die sich die aktion bezieht (z.b mausposition/tile)
 */

/**
 * DTO für allgemeine Aktionen des Spielers, z.B. Attacke oder Interaktion.
 */
public class ActionInputDto {

    /**
     * Art der Aktion, z.B.:
     * - "ATTACK"
     * - "INTERACT"
     * - "USE_ITEM"
     */
    private String action;

    /**
     * Zielposition der Aktion (z.B. Mausposition in Weltkoordinaten).
     */
    private float targetX;
    private float targetY;

    public ActionInputDto() {
    }

    public ActionInputDto(String action, float targetX, float targetY) {
        this.action = action;
        this.targetX = targetX;
        this.targetY = targetY;
    }

     public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public float getTargetX() {
        return targetX;
    }

    public void setTargetX(float targetX) {
        this.targetX = targetX;
    }

    public float getTargetY() {
        return targetY;
    }

    public void setTargetY(float targetY) {
        this.targetY = targetY;
    }

    @Override
    public String toString() {
        return "ActionInputDto{" +
                "action='" + action + '\'' +
                ", targetX=" + targetX +
                ", targetY=" + targetY +
                '}';
    }

}