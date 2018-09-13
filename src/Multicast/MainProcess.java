package Multicast;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Scanner;

public class MainProcess implements Runnable {
    private MulticastSocket socket;
    private InetAddress group;
    private Peer peer;

    public MainProcess () {
        createSocket();
        createPeer();
    }

    private void createSocket () {
        // ips for multicasting: 224.0.0.0 - 239.255.225.255
        socket = null;
        try {
            group = InetAddress.getByName("225.0.0.7");
            socket = new MulticastSocket(6789);
            socket.joinGroup(group);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("IOException at Main Process Builder!");
            System.exit(1);
        }
    }

    private void createPeer() {
        System.out.println("Insert peer's name:");
        Scanner s = new Scanner(System.in);
        String name = s.nextLine();
        peer = new Peer(name, socket, group);
    }

    @Override
    public void run() {
        receiveAll();
        sendRequestMessage();
    }

    public void receiveAll () {
        ReceiveThread receiveMessage = new ReceiveThread(socket, peer);
        receiveMessage.start();
    }

    public void sendRequestMessage () {
        String message = "";
        MessageControlClass mcc = new MessageControlClass(message, peer);
        mcc.executeRequireCommands();
    }
}


