package com.tim.game.shared.DTOs.input;
import com.tim.game.shared.model.Vector2f;
/**
 * Repräsentiert die Bewegungseingabe des clients
 * Sprinten später
 */




/**
 * DTO für eine Bewegungs-Eingabe des Clients.
 * Beispiel: "Bewege dich mit dieser Richtung in diesem Tick".
 */
public class MoveInputDto{
    /**
     * Normalisierte Richtungsvektor.
     * bsp:
     * (0,1) = nach oben
     * (1,0) = nach links
     * (0,0) = keine beweung
     */
    private Vector2f direction;

    public MoveInputDto(){
    }

    public MoveInputDto(Vector2f direction) {
        this.direction = direction;
    }

     public Vector2f getDirection() {
        return direction;
    }

    public void setDirection(Vector2f direction) {
        this.direction = direction;
    }

    @Override
    public String toString() {
        return "MoveInputDto{" +
                "direction=" + direction +
                '}';
    }
}

