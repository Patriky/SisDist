package Multicast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SendThread extends Thread{
    private MulticastSocket socket;
    private InetAddress group;
    String message;

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