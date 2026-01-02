package com.tim.game.shared.model;

/**Ein einfacher 2D-Vektor mit x und y als float.
 * Wird später für Positionen, Bewegungsrichtungen usw. genutzt. 
 * Bewusst simpel gehalten, kein Over-Engineering. 
 */


public class Vector2f{
    private float x;
    private float y;

    public Vector2f(){}

    public Vector2f(float x, float y){
        this.x = x;
        this.y = y;
    }

    public float getX(){
        return x;
    }

    public float getY(){
        return y;
    }

    public void setX(float x){
        this.x = x;
    }

    public void setY(float y){
        this.y = y;
    }

    /**
     * Setze beide varibalen gleichzeitig
     */
    public void set(float x, float y){
        this.x = x;
        this.y = y;
    }

    /**
     * Addiert einen anderen Vektor auf diesen
     */
    public void add( Vector2f other){
        this.x += other.x;
        this.y += other.y;
    }

    @Override
    public String toString() {
        return "Vector2f{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}