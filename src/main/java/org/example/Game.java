package org.example;

import com.google.protobuf.CodedInputStream;
import org.example.proto.GameProto;

import javax.swing.plaf.basic.BasicButtonListener;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

public class Game {

    private final int TICK_RATE = 30; // 30 FPS
    private final long TICK_INTERVAL_MS = 1000 / TICK_RATE;

    private String gameId;
    private Tank tank1, tank2;
    private int player1score, player2score;
    private DatagramSocket socket;

    public Game(Tank player1, Tank player2, DatagramSocket socket) {
        this.tank1 = player1;
        this.tank2 = player2;
        player1score = 0;
        player2score = 0;
        gameId = UUID.randomUUID().toString();
        this.socket = socket;
    }

    public void startGame() throws IOException, InterruptedException {

        BlockingQueue<GameProto.PlayerAction> actionQueue = new LinkedBlockingQueue<>();


        Thread networkThread = new Thread(() -> {
            while (true) {
                try {
                    DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
                    socket.receive(packet);

                    CodedInputStream input = CodedInputStream.newInstance(packet.getData(), 0, packet.getLength());
                    GameProto.PlayerAction action = GameProto.PlayerAction.parseFrom(input);

                    actionQueue.put(action);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        networkThread.start();

        while(true){

            GameRound gameRound = new GameRound(tank1, tank2);
            sendStartGame(gameRound);
            actionQueue.clear();
            while(true){
                long start = System.nanoTime();

                while(!actionQueue.isEmpty()){

//                    long elapsed = (System.nanoTime() - start)/1_000_000;
//                    if(elapsed >= TICK_INTERVAL_MS){break;}

                    GameProto.PlayerAction action = actionQueue.poll();
                    if(action == null){break;}

                    switch (action.getActionCase()){
                        case FORWARD -> gameRound.moveForward(action.getForward().getPlayerId());
                        case BACKWARD -> gameRound.moveBackward(action.getBackward().getPlayerId());
                        case ROTATERIGHT -> gameRound.rotateRight(action.getRotateRight().getPlayerId());
                        case ROTATELEFT -> gameRound.rotateLeft(action.getRotateLeft().getPlayerId());
                        case SHOOT -> {
                            Bullet bullet = gameRound.shoot(action.getShoot().getPlayerId());
                            if(bullet != null){
                                sendFired(bullet);
                            }
                        }

                    }
                }

                Collision collision =gameRound.checkCollision();
                if(collision != null){
                    sendHit(collision.getX(), collision.getY());

                    if(collision.getTankHit() != null) {
                        Tank hitTank = collision.getTankHit();
                        if (hitTank == tank1) {
                            player2score++;
                            sendRoundOver(tank2);

                        } else {
                            player1score++;
                            sendRoundOver(tank1);

                        }
                        break;
                    }
                }

                sendGameState(gameRound);

                // Delay until next tick
                long elapsed = (System.nanoTime() - start)/1_000_000;
                long sleep = TICK_INTERVAL_MS - elapsed;
                if (sleep > 0) {
                    try {
                        Thread.sleep(sleep);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }




        }
    }


    private void sendHit(double x, double y) throws IOException {
        GameProto.Hit hit = GameProto.Hit.newBuilder().setX(x).setY(y).build();
        GameProto.ServerMessage serverMessage = GameProto.ServerMessage.newBuilder().setHit(hit).setHit(hit).build();
        byte[] data = serverMessage.toByteArray();
        DatagramPacket packet1 = new DatagramPacket(data, data.length, tank1.getAddress(), tank1.getPort());
        DatagramPacket packet2 = new DatagramPacket(data, data.length, tank2.getAddress(), tank2.getPort());
        socket.send(packet1);
        socket.send(packet2);
    }

    private void sendRoundOver(Tank tank) throws IOException {
        GameProto.Player p1 = GameProto.Player.newBuilder()
                .setPlayerId(tank.getId())
                .setX(tank.getX())
                .setName(tank.getName())
                .setY(tank.getY())
                .setDirection(tank.getDirection())
                .setScore(tank.getId().equals(tank1.getId())? player1score: player2score)
                .build();

        GameProto.RoundOver roundOver = GameProto.RoundOver.newBuilder().setWinner(p1).build();

        GameProto.ServerMessage serverMessage = GameProto.ServerMessage.newBuilder().setRoundOver(roundOver).build();
        byte[] data = serverMessage.toByteArray();
        DatagramPacket packet1 = new DatagramPacket(data, data.length, tank1.getAddress(), tank1.getPort());
        DatagramPacket packet2 = new DatagramPacket(data, data.length, tank2.getAddress(), tank2.getPort());
        socket.send(packet1);
        socket.send(packet2);

    }

    private void sendGameState(GameRound gameRound) throws IOException {
        ArrayList<Bullet> bulletsArrayList = gameRound.getBullets();
        ArrayList<GameProto.Bullet> bullets = new ArrayList<>();
        for(int i = 0; i < bulletsArrayList.size(); i++){
            Bullet b = bulletsArrayList.get(i);
            bullets.add( GameProto.Bullet.newBuilder()
                .setX(b.getX())
                .setY(b.getY())
                .setSpeed(b.getSpeed())
                .setDirection(b.getAngle())
                .build());
        }

        GameProto.Player p1 = GameProto.Player.newBuilder()
                .setPlayerId(tank1.getId())
                .setX(tank1.getX())
                .setY(tank1.getY())
                .setName(tank1.getName())
                .setDirection(tank1.getDirection())
                .setScore(player1score)
                .build();
        GameProto.Player p2 = GameProto.Player.newBuilder()
                .setPlayerId(tank2.getId())
                .setX(tank2.getX())
                .setName(tank2.getName())
                .setY(tank2.getY())
                .setDirection(tank2.getDirection())
                .setScore(player2score)
                .build();

        GameProto.GameState gameState1 = GameProto.GameState.newBuilder()
                .addAllBullets(bullets)
                .setGameId(gameId)
                .setYou(p1)
                .setOpponent(p2)
                .build();
        GameProto.GameState gameState2 = GameProto.GameState.newBuilder()
                .addAllBullets(bullets)
                .setGameId(gameId)
                .setYou(p2)
                .setOpponent(p1)
                .build();

        GameProto.ServerMessage serverMessage1 = GameProto.ServerMessage.newBuilder().setGameState(gameState1).build();
        GameProto.ServerMessage serverMessage2 = GameProto.ServerMessage.newBuilder().setGameState(gameState2).build();
        byte[] data1= serverMessage1.toByteArray();
        byte[] data2= serverMessage2.toByteArray();
        DatagramPacket packet1 = new DatagramPacket(data1, data1.length, tank1.getAddress(), tank1.getPort());
        DatagramPacket packet2 = new DatagramPacket(data2, data2.length, tank2.getAddress(), tank2.getPort());
        System.out.println(gameState1.toString());
        socket.send(packet1);
        socket.send(packet2);

    }

    private void sendFired(Bullet bullet) throws IOException {
        GameProto.Bullet b= GameProto.Bullet.newBuilder()
                .setX(bullet.getX())
                .setY(bullet.getY())
                .setDirection(bullet.getAngle())
                .setSpeed(bullet.getSpeed())
                .build();

        GameProto.ServerMessage serverMessage = GameProto.ServerMessage.newBuilder()
                .setBullet(b).build();
        byte[] data = serverMessage.toByteArray();
        DatagramPacket packet1 = new DatagramPacket(data, data.length, tank1.getAddress(), tank1.getPort());
        DatagramPacket packet2 = new DatagramPacket(data, data.length, tank2.getAddress(), tank2.getPort());


        socket.send(packet1);
        socket.send(packet2);

    }
    private void sendStartGame(GameRound gameRound) throws IOException {
        GameProto.Player p1 = GameProto.Player.newBuilder()
                .setPlayerId(tank1.getId())
                .setName(tank1.getName())
                .setX(tank1.getX())
                .setY(tank1.getY())
                .setDirection(tank1.getDirection())
                .setScore(player1score)
                .build();
        GameProto.Player p2 = GameProto.Player.newBuilder()
                .setPlayerId(tank2.getId())
                .setName(tank2.getName())
                .setX(tank2.getX())
                .setY(tank2.getY())
                .setDirection(tank2.getDirection())
                .setScore(player2score)
                .build();

        GameProto.GameStart gameStartForP1 = GameProto.GameStart.newBuilder()
                .setGameId(gameId)
                .setYou(p1)
                .setOpponent(p2)
                .addAllMap(Arrays.stream(gameRound.flatMap())
                        .boxed()
                        .toList())
                .build();


        GameProto.GameStart gameStartForP2 = GameProto.GameStart.newBuilder()
                .setGameId(gameId)
                .setYou(p2)
                .setOpponent(p1)
                .addAllMap(Arrays.stream(gameRound.flatMap())
                        .boxed()
                        .toList())
                .build();

        GameProto.ServerMessage serverMessage1 = GameProto.ServerMessage.newBuilder()
                .setGameStart(gameStartForP1)
                .build();

        GameProto.ServerMessage serverMessage2 = GameProto.ServerMessage.newBuilder()
                .setGameStart(gameStartForP2)
                .build();
        byte[] bytes1 = serverMessage1.toByteArray();
        DatagramPacket packet1 = new DatagramPacket(bytes1,bytes1.length, tank1.getAddress(), tank1.getPort() );
        byte[] bytes2 = serverMessage2.toByteArray();
        DatagramPacket packet2 = new DatagramPacket(bytes2,bytes2.length, tank2.getAddress(), tank2.getPort() );

        GameProto.ServerMessage s = GameProto.ServerMessage.parseFrom(bytes1);
        System.out.println(s.toString());
        System.out.println(bytes1.length);
        socket.send(packet1);
        socket.send(packet2);
    }

}
