package Multicast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SendThread extends Thread{
    private MulticastSocket socket;
    private InetAddress group;

    public SendThread(MulticastSocket s, InetAddress g){
        socket = s;
        group = g;
    }

    @Override
    public void run (){
        try {
            System.out.println("Send: ");
            Scanner scan = new Scanner(System.in);
            while (true){
                String arg = scan.nextLine();
                if (arg.equals("quit")) {
                    socket.leaveGroup(group);
                    break;
                }
                byte[] argB = arg.getBytes();
                DatagramPacket messageOut = new DatagramPacket(argB, arg.length(), group, 6789);
                socket.send(messageOut);
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