package org.example;

public class Collision {

    private Bullet bullet;
    private double x;
    private double y;
    private Tank tankHit;

    public Collision(Bullet bullet, double x, double y, Tank tankHit) {
        this.bullet = bullet;
        this.x = x;
        this.y = y;
        this.tankHit = tankHit;
    }

    public Collision(Bullet bullet, double x, double y) {
        this.bullet = bullet;
        this.x = x;
        this.y = y;
        this.tankHit = null;
    }

    public Bullet getBullet() {
        return bullet;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public Tank getTankHit() {
        return tankHit;
    }
}
