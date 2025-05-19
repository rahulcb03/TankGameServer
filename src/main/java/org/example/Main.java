package org.example;

import com.google.protobuf.CodedInputStream;
import org.example.proto.GameProto;

import java.io.IOException;
import java.net.*;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        DatagramSocket socket = new DatagramSocket(5252);

        // isBound() method
        System.out.println("IsBound : " + socket.isBound());

        // isConnected() method
        System.out.println("isConnected : " + socket.isConnected());

        // getInetAddress() method
        System.out.println("InetAddress : " + socket.getInetAddress());

        // getPort() method
        System.out.println("Port : " + socket.getPort());

        // getRemoteSocketAddress() method
        System.out.println("Remote socket address : " +
                socket.getRemoteSocketAddress());

        // getLocalSocketAddress() method
        System.out.println("Local socket address : " +
                socket.getLocalSocketAddress());

        Tank tank1 = receiveReadyPlayer(socket);

        Tank tank2 =receiveReadyPlayer(socket);

        Game game = new Game(tank1, tank2, socket);
        game.startGame();


    }

    private static Tank receiveReadyPlayer(DatagramSocket socket) throws IOException {
        DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
        socket.receive(packet);
        System.out.println("received packet");

        CodedInputStream input = CodedInputStream.newInstance(packet.getData(), 0, packet.getLength());
        GameProto.PlayerReady playerReady = GameProto.PlayerReady.parseFrom(input);
        System.out.println("PlayerReady : " + playerReady.getName());
        return new Tank(playerReady.getName(),packet.getAddress(), packet.getPort());
    }
}