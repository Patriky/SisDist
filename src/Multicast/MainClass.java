package Multicast;

import java.net.*;
import java.io.*;

public class MainClass {
    public static void main(String args[]) {
		// ips for multicasting: 224.0.0.0 - 239.255.225.255
		MulticastSocket socket = null;
        try {
            InetAddress group = InetAddress.getByName("230.230.230.230");
            socket = new MulticastSocket(6789);
            socket.joinGroup(group);

            ReceiveThread receiveMessage = new ReceiveThread(socket);
            SendThread sendMessage = new SendThread(socket, group);

            receiveMessage.start();
            sendMessage.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
}