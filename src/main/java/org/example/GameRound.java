package org.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class GameRound {

    private static final int PIXEL_WIDTH = 800;
    private static final int PIXEL_HEIGHT = 600;
    private static final int TILE_SIZE = 10;
    private static final int MAP_WIDTH = PIXEL_WIDTH/TILE_SIZE;
    private static final int MAP_HEIGHT = PIXEL_HEIGHT/TILE_SIZE;

    private static final int MAX_BULLETS = 5;

    private static final int MOVE_SPEED = 3;
    private static final int BULLET_SPEED = 400;
    private static final double ROTATE_SPEED = 2.0;

    private static final int GRACE_PERIOD = 500;


    private Tank player1;
    private Tank player2;

    private ArrayList<Bullet> bullets;

    private Tile[][] map;

    public GameRound(Tank player1, Tank player2) {
        this.player1 = player1;
        this.player2 = player2;
        bullets = new ArrayList<>();

        this.player1.setPosition(TILE_SIZE * 2, TILE_SIZE * 2);
        this.player2.setPosition(PIXEL_WIDTH - TILE_SIZE * 3, PIXEL_HEIGHT - TILE_SIZE * 3);
        this.player1.resetBulletCount();
        this.player2.resetBulletCount();

//        for (int row = 0; row < MAP_HEIGHT; row++) {
//            for (int col = 0; col < MAP_WIDTH; col++) {
//                if (row == 0 || row == MAP_HEIGHT - 1 || col == 0 || col == MAP_WIDTH - 1) {
//                    map[row][col] = new Tile(TileType.WALL); // Walls around the border
//                } else {
//                    map[row][col] = new Tile(TileType.EMPTY);  // Walkable area
//                }
//            }
//        }

        map = readMapFile();


    }

    public ArrayList<Bullet> getBullets() {
        return bullets;
    }
    public Tank getPlayer(String id) {
        return id.equals(player1.getId()) ? player1 : player2;
    }

    public Tile[][] getMap() {
        return map;
    }

    public int[] flatMap() {
        int[] flat = new int[MAP_WIDTH * MAP_HEIGHT];
        for (int row = 0; row < MAP_HEIGHT; row++) {
            for (int col = 0; col < MAP_WIDTH; col++) {
                flat[row * MAP_WIDTH + col] = map[row][col].isWalkable() ? 0 : 1;
            }
        }
        return flat;
    }


    public boolean moveForward(String id) {
        Tank player = getPlayer(id);
        double directionDeg = player.getDirection();
        double directionRad = Math.toRadians(directionDeg);

        return move(player, directionRad);
    }

    public boolean moveBackward(String id) {
        Tank player = getPlayer(id);
        double directionDeg = player.getDirection();
        double directionRad = Math.toRadians(directionDeg + 180);

        return move(player, directionRad);
    }

    public void rotateLeft(String id) {
        getPlayer(id).updateDir(ROTATE_SPEED);
    }

    public void rotateRight(String id) {
        getPlayer(id).updateDir(-ROTATE_SPEED);
    }


    public Bullet shoot(String id){
        Tank player = getPlayer(id);
        if(player.getBulletCount()>= MAX_BULLETS){
            return null;
        }

        //need to adjust where bullets start because will cause issue with self-hit
        //should move it out of the players hit box

        double dx = (player.getHitboxRadius()+10) * Math.cos(Math.toRadians(player.getDirection()));
        double dy = (player.getHitboxRadius()+10) * Math.sin(Math.toRadians(player.getDirection()));

        double x_updated = player.getX() + dx;
        double y_updated = player.getY() - dy;

        if(!checkValidBulletStart(player, x_updated, y_updated)){return null;}
        Bullet bullet = new Bullet(x_updated, y_updated, player.getDirection(), BULLET_SPEED, id);

        bullets.add(bullet);
        player.increaseBulletCount();
        return bullet;
    }

    public Collision checkCollision() {
        for(Bullet bullet : bullets) {
            long deltaTime = System.currentTimeMillis() - bullet.getTime();
            double oldX = bullet.getX();
            double oldY = bullet.getY();

            bullet.update();
            if(checkWallCollision(bullet, oldX, oldY)){
                bullets.remove(bullet);
                getPlayer(bullet.getPlayerId()).decreaseBulletCount();
                return new Collision(bullet, bullet.getX(), bullet.getY());
            }

            Tank p =checkPlayerCollision(bullet);


            if( p != null ){
                if (p.getId().equals(bullet.getPlayerId()) && deltaTime <= GRACE_PERIOD) { return null; }
                bullets.remove(bullet);
                getPlayer(bullet.getPlayerId()).decreaseBulletCount();
                return new Collision(bullet, p.getX(), p.getY(), p);
            }

        }
        return null;

    }

    public boolean checkWallCollision(Bullet bullet, double oldX, double oldY) {
        double dx = bullet.getX() - oldX;
        double dy = bullet.getY() - oldY;
        double distance = Math.hypot(dx, dy);
        int steps = (int) distance;

        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            double x = oldX + dx * t;
            double y = oldY + dy * t;
            if (!isWalkable(x, y)) {
                return true;
            }
        }
        return false;
    }

    private Tank checkPlayerCollision(Bullet bullet) {
        for (Tank tank : new Tank[]{player1, player2}) {

            double dx = bullet.getX() - tank.getX();
            double dy = bullet.getY() - tank.getY();
            double distanceSquared = dx * dx + dy * dy;
            double distance = Math.sqrt(distanceSquared);

            double collisionDistance = bullet.getHitboxRadius() + tank.getHitboxRadius();


            if (distance < collisionDistance ) {
                return tank;
            }
        }

        return null;
    }

    private boolean move(Tank player, double directionRad) {
        double dx = MOVE_SPEED * Math.cos(directionRad);
        double dy = MOVE_SPEED * Math.sin(directionRad);

        double x_updated = player.getX() + dx;
        double y_updated = player.getY() - dy;

        if (isWalkable(x_updated, y_updated)) {
            player.updatePos(dx, -1*dy);
            return true;
        }
        return false;
    }

    private boolean isWalkable(double x, double y) {
        int col = (int) x / TILE_SIZE;
        int row = (int) y / TILE_SIZE;

        if (row < 0 || row >= map.length || col < 0 || col >= map[0].length) {
            return false;
        }

        Tile tile = map[row][col];
        return tile.isWalkable();
    }

    private boolean checkValidBulletStart(Tank player, double x1, double y1) {
        double x0 = player.getX();
        double y0 = player.getY();

        double dx = x1 - x0;
        double dy = y1 - y0;
        double distance = Math.hypot(dx, dy);

        int steps = (int)(distance);
        for (int i = 1; i <= steps; i++) {
            double t = (double)i / steps;
            double x = x0 + dx * t;
            double y = y0 + dy * t;

            if (!isWalkable(x, y)) {
                return false;
            }
        }

        return true;
    }


    private static Tile[][] readMapFile(){
        File file = new File("src/main/java/org/example/map.txt");
        Tile[][] map = new Tile[MAP_HEIGHT][MAP_WIDTH];
        int i =0;
        try(BufferedReader br = new BufferedReader(new FileReader(file))){
            for(String line = br.readLine(); line != null && i < MAP_HEIGHT; line = br.readLine()){
                char[] chars = line.toCharArray();
                if(chars.length != MAP_WIDTH){throw new Error("Invalid map file");}

                for(int j = 0; j < MAP_WIDTH; j++){
                    map[i][j] = new Tile( chars[j] =='1'? TileType.WALL : TileType.EMPTY);
                }
                i++;
            }


        }catch (IOException e){
            throw new RuntimeException(e);
        }

        return map;

    }






}


