package Multicast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;

public class ReceiveThread extends Thread {
    private MulticastSocket socket;
    private MainProcess mainProcess;
    public ArrayList <String> pbKList = new ArrayList<>();
    public String message;

    public ReceiveThread(MulticastSocket s, MainProcess mp){
        socket = s;
        mainProcess = mp;
    }

    protected void listingKeys () {
        System.out.println("Listing all the public keys: ");
        int count = 0;
        for (String key : pbKList) {
            count++;
            System.out.println(count + ": " + key);
        }
    }

    @Override
    public void run (){
        try {
            while (true) {
                byte[] buffer = new byte[1000];
                DatagramPacket messageIn = new DatagramPacket(buffer, buffer.length);
                socket.receive(messageIn);
                message = new String(messageIn.getData());

                if (message.contains("Sun RSA public key") && !pbKList.contains(message)) {
                    pbKList.add(message);
                    mainProcess.sendPubKeys();
                    System.out.println("public key sent!");
                }
                else if (message.contains("list")) {
                    listingKeys();
                }
            }
        }  catch (IOException e) {
            System.out.println("IO: " + e.getMessage() + " at ReceiveThread");
            System.exit(1);
        }
    }
}
