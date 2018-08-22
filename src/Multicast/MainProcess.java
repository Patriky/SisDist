package Multicast;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

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
        }
    }

    private void createKeys() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("LINK");
            keyPairGenerator.initialize(2048);
            KeyPair pair = keyPairGenerator.generateKeyPair();
            pvK = pair.getPrivate();
            pbK = pair.getPublic();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        createKeys();
        ReceiveThread receiveMessage = new ReceiveThread(socket);
        SendThread sendMessage = new SendThread(socket, group);

        receiveMessage.start();
        sendMessage.start();
    }
}


