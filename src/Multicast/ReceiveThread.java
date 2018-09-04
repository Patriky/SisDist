package Multicast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.util.ArrayList;

public class ReceiveThread extends Thread {
    private MulticastSocket socket;
    private MainProcess mainProcess;
    public ArrayList <String> pbKList = new ArrayList<>();
    public String message;
    public String publicKey;

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
                    publicKey = message;
                    mainProcess.sendPubKeys();
                }
                else if (message.contains("list")) {
                    listingKeys();
                }
                else if (message.contains("The peer:")) {
                    System.out.println(message);
                    if (message.contains("has left.")){
                        pbKList.remove(publicKey);
                    }
                }
            }
        }  catch (IOException e) {
            System.out.println("IO: " + e.getMessage() + " at ReceiveThread");
            System.exit(1);
        }
    }
}
