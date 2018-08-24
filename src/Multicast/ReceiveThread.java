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
    public ArrayList <String> pbKList = new ArrayList<>();
    public String message;

    public ReceiveThread(MulticastSocket s){
        socket = s;
    }

    public String getMessage () { return message; }

    public ArrayList <String> getPbKList () { return pbKList; }

    @Override
    public void run (){
        try {
            while (true) {
                byte[] buffer = new byte[1000];
                DatagramPacket messageIn = new DatagramPacket(buffer, buffer.length);
                socket.receive(messageIn);
                message = new String(messageIn.getData());
                System.out.println("Received:" + message);
                if(message.contains("Sun RSA public key")) {
                    pbKList.add(message);
                    System.out.println("Key Keeped!");
                }
                else if(message.contains("request public keys")) {
                    System.out.println("Key To Send!");
                }
            }
        }  catch (IOException e) {
            System.out.println("IO: " + e.getMessage() + " at ReceiveThread");
            System.exit(1);
        }
    }
}
