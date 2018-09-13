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

    public void executeReceiveCommands() {
        if (message.contains("PUBLIC KEY:")) {
            message = message.replace("PUBLIC KEY:", "");
            KeySignatureClass ksc = new KeySignatureClass();
            PublicKey pbK = ksc.encodeStringToKey(message);
            if (pbK != null){
                if (!peer.containsPbK(pbK)){
                    peer.addPbKToList(pbK);
                }
            }
            else {
                System.out.println("Signature Failure!");
            }
        }
        else if (message.contains("The peer:")) {
            String gap = peer.getGap();
            String[] msg = message.split(gap);
            System.out.println(msg[0]);
            if (message.contains("has left.")){
                KeySignatureClass ksc = new KeySignatureClass();
                PublicKey key = ksc.encodeStringToKey(msg[1]);
                peer.removeKeyFromList(key);
            }
        }
    }

    public void executeRequireCommands() {
        Scanner scan = new Scanner(System.in);
        while (true) {
            message = scan.nextLine();
            if (message.equals("quit") || message.equals("exit")) {
                KeySignatureClass ksc = new KeySignatureClass();
                String pbKString = ksc.decodeKeyToString(peer.getPublicKey());
                String notification = "The peer: " + peer.getName() + " has left." + peer.getGap() + pbKString;
                peer.sendMessage(notification);
                suspendTime(1);
                leaveSocket();
                break;
            } else if (message.equals("list")) {
                peer.listingKeys();
            } else if (message.equals("help")) {
                System.out.println(" - quit or exit -> exit the process and remove peer from list");
                System.out.println(" - list -> list the peers on the list");
                System.out.println(" - help -> show help menu");
            } else {
                peer.sendMessage(message);
            }
        }
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
}
