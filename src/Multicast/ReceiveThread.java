package Multicast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;

public class ReceiveThread extends Thread {
    private MulticastSocket socket;
    private Peer peer;

    public ReceiveThread(MulticastSocket s, Peer p){
        socket = s;
        peer = p;
    }

    @Override
    public void run (){
        try {
            while (true) {
                byte[] buffer = new byte[10240];
                DatagramPacket messageIn = new DatagramPacket(buffer, buffer.length);
                socket.receive(messageIn);
                String message = new String(messageIn.getData()).trim();
                MessageControlClass mcc = new MessageControlClass(message, peer);
                mcc.executeReceiveCommands();
            }
        }  catch (IOException e) {
            System.out.println("IO: " + e.getMessage() + " at ReceiveThread");
        }
    }
}
