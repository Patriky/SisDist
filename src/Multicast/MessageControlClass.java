package Multicast;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.security.PublicKey;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MessageControlClass {
    private String message;
    private Peer peer;

    public MessageControlClass (String m, Peer p) {
        message = m;
        peer = p;
    }

    // Comandos executados ao receber uma mensagem
    public void executeReceiveCommands() {
        if (message. contains(peer.getGap())){
            String gap = peer.getGap();
            String[] msg = message.split(gap);
            if (msg[0].equals("Sending public key")) {
                KeyHandlerClass ksc = new KeyHandlerClass();
                PublicKey pbK = ksc.encodeStringToKey(msg[1]);
                if (pbK != null){
                    if (!peer.containsPbK(pbK)){
                        peer.addPbKToList(pbK);
                    }
                } else {
                    System.out.println("Signature Failure!");
                }
            } else if (msg[0].contains("has left.")){
                System.out.println(msg[0]);
                KeyHandlerClass ksc = new KeyHandlerClass();
                PublicKey key = ksc.encodeStringToKey(msg[1]);
                peer.removeKeyFromList(key);
            }
        } else if (!message.contains("")){
            System.out.println(message);
        }
    }

    // Comandos executados ao enviar uma mensagem
    public void executeRequireCommands() {
        Scanner scan = new Scanner(System.in);
        while (true) {
            message = scan.nextLine();
            if (message.equals("quit") || message.equals("exit")) {
                quitAction();
                break;
            } else if (message.equals("list")) {
                peer.listingKeys();
            } else if (message.equals("help")) {
                showHelpMenu();
            } else if (message.contains("require")) {
                String[] msg = message.split(" ");
                String resourceName = msg[1];
            } else if (message.contains("release")) {
                String[] msg = message.split(" ");
                String resourceName = msg[1];
            } else {
                peer.sendMessage(message);
            }
        }
    }

    public void quitAction () {
        KeyHandlerClass ksc = new KeyHandlerClass();
        String pbKString = ksc.decodeKeyToString(peer.getPublicKey());
        String notification = "The peer: " + peer.getName() + " has left." + peer.getGap() + pbKString;
        peer.sendMessage(notification);
        suspendTime(1);
        leaveSocket();
    }

    public void suspendTime (int time) {
        try {
            TimeUnit.SECONDS.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void leaveSocket () {
        MulticastSocket socket = peer.getSocket();
        InetAddress group = peer.getGroup();
        try {
            socket.leaveGroup(group);
        } catch (SocketException e) {
            System.out.println("Socket is" + e.getMessage() + " at MainProcess");
        } catch(IOException e){
            Logger.getLogger(SendThread.class.getName()).log(Level.SEVERE, null, e);
        } finally {
            if (socket != null) socket.close();
        }
    }

    public void showHelpMenu() {
        System.out.println(" - \"list\": List all the connected peers");
        System.out.println(" - \"require\" <resource_name>: Peer REQUIRE a resource to use");
        System.out.println(" - \"release\" <resource_name>: Peer stop using a resource and RELEASE it");
        System.out.println(" - \"status\": Show the status (RELEASED, HELD, WANTED) of all resources and it's name");
        System.out.println(" - \"help\": Show help menu");
        System.out.println(" - \"exit\" or \"quit\": Exit / Quit the process and remove the peer from list");
    }
}
