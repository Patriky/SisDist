package Multicast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class SendThread extends Thread{
    private MulticastSocket socket;
    private InetAddress group;
    public String message;

    public SendThread(MulticastSocket s, InetAddress g, String m){
        socket = s;
        group = g;
        message = m;
    }

    @Override
    public void run (){
        try {
            byte[] mBytes = message.getBytes();
            DatagramPacket messageOut = new DatagramPacket(mBytes, message.length(), group, 6789);
            socket.send(messageOut);
        } catch (IOException e) {
            System.out.println("IO: " + e.getMessage() + " at SendThread");
            System.exit(1);
        }
    }
}