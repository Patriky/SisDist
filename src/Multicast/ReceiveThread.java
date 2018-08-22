package Multicast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.security.PrivateKey;
import java.security.PublicKey;

public class ReceiveThread extends Thread {
    private MulticastSocket socket;

    public ReceiveThread(MulticastSocket s){
        socket = s;
    }

    @Override
    public void run (){
        try {
            while (true) {
                byte[] buffer = new byte[1000];
                DatagramPacket messageIn = new DatagramPacket(buffer, buffer.length);
                socket.receive(messageIn);
                String message = new String(messageIn.getData());
                System.out.println("Received:" + message);
            }
        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO: " + e.getMessage());
        } finally {
            if (socket != null) socket.close();
        }
    }
}
