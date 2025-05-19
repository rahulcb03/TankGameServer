package org.example;

import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.UUID;

public class Tank {

    private static final int HITBOX_RADIUS = 20;
    private String id;
    private String name;
    private InetAddress address;
    private int port;


    private double x, y;
    private double direction;
    private int bulletCount;

    public Tank(String name, InetAddress address, int port) {
        this.name = name;
        id = UUID.randomUUID().toString();
        this.x = 0;
        this.y = 0;
        this.direction = 0;
        bulletCount = 0;
        this.address = address;
        this.port = port;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getX() {
        return x;
    }
    public double getY() {
        return y;
    }
    public double getDirection() {
        return direction;
    }

    public int getBulletCount() {
        return bulletCount;
    }

    public void increaseBulletCount() {
        bulletCount++;
    }
    public void decreaseBulletCount() {
        bulletCount--;
    }
    public void resetBulletCount() {
        bulletCount = 0;
    }

    public InetAddress getAddress() {
        return address;
    }
    public int getPort() {
        return port;
    }

    public int getHitboxRadius() {
        return HITBOX_RADIUS;
    }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }
    public void updatePos(double dx, double dy) {
        x += dx;
        y += dy;
    }

    public void updateDir(double degrees) {
        this.direction = (this.direction + degrees) % 360;
        if (this.direction < 0) {
            this.direction += 360;
        }
    }






}
