package Multicast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SendThread extends Thread{
    private MulticastSocket socket;
    private InetAddress group;
    String pbK;

    public SendThread(MulticastSocket s, InetAddress g, String publicK){
        socket = s;
        group = g;
        pbK = publicK;
    }

    @Override
    public void run (){
        try {
            byte[] pubB = pbK.getBytes();
            DatagramPacket messageOut = new DatagramPacket(pubB, pbK.length(), group, 6789);
            socket.send(messageOut);
        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        } catch(IOException e){
            Logger.getLogger(SendThread.class.getName()).log(Level.SEVERE, null, e);
        } finally {
            if (socket != null) socket.close();
        }
    }
}