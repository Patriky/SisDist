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
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainProcess implements Runnable {
    private String name;
    protected MulticastSocket socket;
    protected InetAddress group;
    private PrivateKey pvK;
    public PublicKey pbK;

    public MainProcess () {
        createSocket();
        setName();
        createKeys();
        String notification = "The peer: " + getName() + " has joined.";
        sendMessage(notification);
    }

    public void setName () {
        Scanner s = new Scanner(System.in);
        System.out.print("Insert peer's name: ");
        name = s.nextLine();
    }

    public String getName () { return name; }

    private void createSocket () {
        // ips for multicasting: 224.0.0.0 - 239.255.225.255
        socket = null;
        try {
            group = InetAddress.getByName("225.0.0.7"); // Lincoln
            //group = InetAddress.getByName("230.230.230.230"); // Gabriel Eugenio
            //group = InetAddress.getByName("224.0.1.255"); // Mateus
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
        ReceiveThread receiveMessage = new ReceiveThread(socket, this);
        receiveMessage.start();
    }

    public void sendMessage(String m) {
        SendThread sendM = new SendThread(socket, group, m);
        sendM.start();
    }

    public void sendPubKeys () {
        String publicKey = pbK.toString();
        sendMessage(publicKey);
    }

    public void leaveSocket () {
        try {
            socket.leaveGroup(group);
        } catch (SocketException e) {
            System.out.println("Socket is" + e.getMessage() + " at MainProcess");
        } catch(IOException e){
            Logger.getLogger(SendThread.class.getName()).log(Level.SEVERE, null, e);
        } finally {
            if (socket != null) socket.close();
        }
    }

    public void sendRequestMessage () {
        Scanner scan = new Scanner(System.in);
        String arg;
        while (true) {
            arg = scan.nextLine();
            if (arg.equals("quit")) {
                String notification = "The peer: " + getName() + " has left.";
                sendMessage(notification);
                try {
                    TimeUnit.MILLISECONDS.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                leaveSocket();
                break;
            }
            sendMessage(arg);
        }
    }

    @Override
    public void run() {
        sendPubKeys();
        receiveAll();
        sendRequestMessage();
    }
}


