package Multicast;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.security.PublicKey;
import java.util.List;
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
        if (message.contains(peer.getGap())){
            // Separa mensagem e dados
            String gap = peer.getGap();
            String[] msg = message.split(gap);

            if (msg[0].equals("ENTRY")) {
                // Recebeu a chave do par a ser adicionado
                KeyHandlerClass ksc = new KeyHandlerClass();
                PublicKey pbK = ksc.encodeStringToKey(msg[1]);
                if (!peer.containsPbK(pbK)){
                    peer.addPbKToList(pbK);
                }

                // Recebeu o(s) recurso(s) para ser(em) adicionado(s)
                String resource = msg[2];
                String[] resourceList = resource.split("\n");
                for (String r : resourceList) {
                    peer.addResource(r, pbK);
                }
            } else if (msg[0].contains("LEAVE")){
                // Recebeu a chave do par a ser retirado
                KeyHandlerClass ksc = new KeyHandlerClass();
                PublicKey key = ksc.encodeStringToKey(msg[1]);
                peer.removeKeyFromList(key);

                // Recebeu o(s) recurso(s) para ser(em) retirado(s)
                String resource = msg[2];
                String[] resourceList = resource.split("\n");
                peer.removeResource(key, resourceList);
            }
        } else if (!message.equals("")) {
            System.out.println(message);
        }
    }

    // Comandos executados ao enviar uma mensagem
    // TODO: para os comandos require, release e status, enviar mensagem com os dados
    public void executeRequireCommands() {
        Scanner scan = new Scanner(System.in);
        while (true) {
            message = scan.nextLine();
            if (message.equals("quit") || message.equals("exit")) {
                peer.sendMessage("The peer: " + peer.getName() + " has left.");
                quitAction();
                break;
            } else if (message.equals("list")) {
                peer.listingKeys();
            } else if (message.equals("help")) {
                showHelpMenu();
            } else if (message.contains("require")) { //&& peer.getPeerStatus().equals("SHARING_RESOURCES")) {
                String[] msg = message.split(" ");
                String resourceName = msg[1];
                // TODO: mandar mensagens se o par pode, ou não, acessar o recurso
                if (peer.getResourceStatusFromHash(resourceName).equals("RELEASED")) {
                    peer.useResource(resourceName);
                } else {
                    peer.wantResource(resourceName);
                }
            } else if (message.contains("release")) { //&& peer.getPeerStatus().equals("SHARING_RESOURCES")) {
                String[] msg = message.split(" ");
                String resourceName = msg[1];
                if (peer.getResourceStatusFromHash(resourceName).equals("HELD")) {
                    peer.releaseResource(resourceName);
                }
            } else if (message.equals("status")) {
                List<String> resourceNames = peer.getResourceNameFromHash();
                System.out.println("Resource Status:");
                for (String resourceName : resourceNames) {
                    System.out.println("Name: " + resourceName + " \t " + "Status: "
                            + peer.getResourceStatusFromHash(resourceName));
                }
            } else {
                peer.sendMessage(message);
            }
        }
    }

    public void quitAction () {
        // Envia a chave do par decodificada
        KeyHandlerClass ksc = new KeyHandlerClass();
        String pbKString = ksc.decodeKeyToString(peer.getPublicKey());
        String data = "LEAVE" + peer.getGap() + pbKString;

        // Envia também os recursos desse par
        String resources = String.join("\n", peer.getMyResource());
        data += peer.getGap() + resources;
        peer.sendMessage(data);

        // Interrompe a tarefa para que o envio dos dados ocorra antes!
        suspendTime(1);

        // Remove o par do grupo
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
        System.out.println(" - \"list\": List the connected peers");
        System.out.println(" - \"require\" <resource_name>: Peer REQUIRE a resource to use");
        System.out.println(" - \"release\" <resource_name>: Peer stop using a resource and RELEASE it");
        System.out.println(" - \"status\": Show the name and status (RELEASED, HELD, WANTED) of the resources");
        System.out.println(" - \"help\": Show help menu");
        System.out.println(" - \"exit\" or \"quit\": Exit / Quit the process and remove the peer from list");
    }
}
