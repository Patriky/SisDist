package Multicast;

import java.net.*;
import java.io.*;

public class MulticastPeer{
    public static void main(String args[]) {
		// ips for multicasting: 224.0.0.0 - 239.255.225.255
		MulticastSocket s = null;
		try {
            InetAddress group = InetAddress.getByName("230.230.230.230");
            s = new MulticastSocket(6789);
            s.joinGroup(group);
            ReceiveThread receiveThread = new ReceiveThread(s);
            receiveThread.start();
            SendMessage sendMessage = new SendMessage(s, group);
            sendMessage.Send();
        } catch (SocketException e){
		    System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
		    System.out.println("IO: " + e + e.getMessage());
        } finally {
		    if(s != null) s.close();
		}
	}
}