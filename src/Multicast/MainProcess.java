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
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainProcess implements Runnable{
    protected MulticastSocket socket;
    protected InetAddress group;
    private PrivateKey pvK;
    public PublicKey pbK;

    public MainProcess () {
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
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(1024);
            KeyPair pair = keyPairGenerator.generateKeyPair();
            pvK = pair.getPrivate();
            pbK = pair.getPublic();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            System.out.println("Key failure!");
            System.exit(1);
        }
    }

    public String getPublicKey () {
        return pbK.toString();
    }

    @Override
    public void run() {
        createKeys();
        ReceiveThread receiveMessage = new ReceiveThread(socket);
        SendThread sendMessage = new SendThread(socket, group, getPublicKey());

        receiveMessage.start();
        sendMessage.start();

        System.out.println("Send: ");
        try {
            Scanner scan = new Scanner(System.in);
            while (true) {
                String arg = scan.nextLine();
                if (arg.equals("quit")) {
                    socket.leaveGroup(group);
                    break;
                }

                SendThread sendThread = new SendThread(socket, group, getPublicKey());
                sendThread.start();
            }
        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        } catch(IOException e){
            Logger.getLogger(SendThread.class.getName()).log(Level.SEVERE, null, e);
        } finally {
            if (socket != null) socket.close();
        }
    }
}


