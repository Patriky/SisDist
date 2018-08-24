package Multicast;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainProcess implements Runnable{
    protected MulticastSocket socket;
    protected InetAddress group;
    private PrivateKey pvK;
    public PublicKey pbK;

    public MainProcess () {
        createSocket();
        createKeys();
    }

    private void createSocket () {
        // ips for multicasting: 224.0.0.0 - 239.255.225.255
        socket = null;
        try {
            group = InetAddress.getByName("230.230.230.230");
            socket = new MulticastSocket(6789);
            socket.joinGroup(group);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("IOException at Main Process Builder!");
            System.exit(1);
        }
    }

    private void createKeys() {
        KeyPairGenerator keyPairGenerator = null;
        KeyPair pair = null;
        try {
            keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(1024);
            pair = keyPairGenerator.generateKeyPair();
            pvK = pair.getPrivate();
            pbK = pair.getPublic();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            System.out.println("Key failure!");
            System.exit(1);
        }
    }

    public void receiveAll () {
        ReceiveThread receiveMessage = new ReceiveThread(socket);
        receiveMessage.start();
    }

    public void sendPubKeys () {
        String publicKey = pbK.toString();
        SendThread sendMessage = new SendThread(socket, group, publicKey);
        sendMessage.start();
    }

    public void sendRequestMessage () {
        Scanner scan = new Scanner(System.in);
        String arg;
        try {
            while (true) {
                arg = scan.nextLine();
                if (arg.equals("quit")) {
                    socket.leaveGroup(group);
                    break;
                }
                else if (arg.equals("send public key")) {
                    sendPubKeys();
                }
                SendThread sendThread = new SendThread(socket, group, arg);
                sendThread.start();
            }
        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage() + " at MainProcess");
        } catch(IOException e){
            Logger.getLogger(SendThread.class.getName()).log(Level.SEVERE, null, e);
        } finally {
            if (socket != null) socket.close();
        }
    }

    @Override
    public void run() {
        receiveAll();
        sendPubKeys();
        sendRequestMessage();
    }
}


