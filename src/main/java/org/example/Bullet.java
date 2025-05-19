package org.example;

import java.util.Date;

public class Bullet {
    private static final int HITBOX_RADIUS = 7;

    private double x, y, angle, speed;
    private String playerId;

    private long spawnTime;
    private long lastUpdateTime;

    public Bullet(double x, double y, double angle, double speed, String playerId) {
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.speed = speed;
        this.spawnTime = System.currentTimeMillis();
        this.lastUpdateTime = spawnTime;
        this.playerId = playerId;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getAngle() {
        return angle;
    }

    public double getSpeed() {
        return speed;
    }
    public long getTime() {
        return spawnTime;
    }
    public int getHitboxRadius() {
        return HITBOX_RADIUS;
    }
    public String getPlayerId() {
        return playerId;
    }

    public void update() {
        long now = System.currentTimeMillis();
        long deltaTime = now - lastUpdateTime;
        double distance = (speed/1000) * deltaTime;
        lastUpdateTime = now;
        x += distance * Math.cos(Math.toRadians(angle)) ;
        y -= distance * Math.sin(Math.toRadians(angle)) ;
    }
}
