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
                KeyHandlerClass khc = new KeyHandlerClass();
                PublicKey pbK = khc.encodeStringToKey(msg[1]);
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
                KeyHandlerClass khc = new KeyHandlerClass();
                PublicKey key = khc.encodeStringToKey(msg[1]);
                peer.removeKeyFromList(key);

                // Recebeu o(s) recurso(s) para ser(em) retirado(s)
                String resource = msg[2];
                String[] resourceList = resource.split("\n");
                peer.removeResource(key, resourceList);
            } else if (msg[0].contains("REQUIRE")) {
                String resourceName = msg[1];
                String pbKString = msg[2];
                KeyHandlerClass khc = new KeyHandlerClass();
                PublicKey pbK = khc.encodeStringToKey(pbKString);
                if (peer.getConnectedPeers() == peer.getAnswer()) { // Ninguém esta usando o recurso, pode usar
                    peer.useResource(resourceName);
                } else if (peer.getConnectedPeers() < peer.getAnswer()) {
                    // Se não recebeu resposta positiva de todos os pares conectados, então alguém esta usando/querendo usar
                    // Seta o status do recurso como WANTED
                    peer.wantResource(resourceName);
                    // E adiciona na fila peerToAccess da ResourceClass
                    peer.getResourceFromHash(resourceName).addPeersToQueue(pbK);
                }
            } else if (msg[0].contains("RELEASE")) {
                String resourceName = msg[1];
                // TODO: fazer com que os pares na lista possam usar o recurso (TESTAR!)
                // TODO: assinar as mensagens enviadas!
                if (peer.getResourceStatusFromHash(resourceName).equals("HELD")) {
                    peer.releaseResource(resourceName);
                    // se tiver alguém na fila
                    if (peer.getResourceFromHash(resourceName).getQueueSize() > 0) {
                        // se liberou, deixa o proximo da fila usar o recurso
                        peer.initializeAnswers();
                        // Pergunta se o proximo pode utilizar o recurso
                        peer.sendMessage("ANSWER" + peer.getGap() + resourceName);
                        PublicKey nextPeerKey = peer.getResourceFromHash(resourceName).getPeerFromQueue();
                        KeyHandlerClass khc = new KeyHandlerClass();
                        String publicKeyString = khc.decodeKeyToString(nextPeerKey);
                        // Envia o comando, o nome do recurso e a chave do proximo par
                        peer.sendMessage("REQUIRE" + peer.getGap() + resourceName + peer.getGap() + publicKeyString);
                    }
                } else {
                    System.out.println("ERROR: Resource is not being used!");
                }
            } else if (msg[0].contains("ANSWER")) {
                String resourceName = msg[1];
                if (peer.getResourceStatusFromHash(resourceName).equals("RELEASED")) { // se não estiver usando, pode usar
                    // manda resposta positiva
                    System.out.println("YES");
                    // se positivo, incrementa numero de respostas positivas
                    peer.incrementAnswer();
                    // adiciona o par que respondeu na lista
                    peer.addAnsweredPeer(peer.getPublicKey());
                } else { // Se estiver usando ou querendo usar (esta antes na fila)
                    // manda resposta negativa
                    System.out.println("NO");
                    // adiciona o par que respondeu na lista
                    peer.addAnsweredPeer(peer.getPublicKey());
                }
            }
        } else if (!message.equals("")) {
            System.out.println(message);
        }
    }

    // Comandos executados ao enviar uma mensagem
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

                // Inicializa contador e registro de respostas
                peer.initializeAnswers();
                // Pergunta se pode usar o recurso
                peer.sendMessage("ANSWER" + peer.getGap() + resourceName);

                // Envia o nome do recurso e o comando
                String data = "REQUIRE" + peer.getGap() + resourceName;

                // Envia a chave do par que vai usar o recurso
                KeyHandlerClass khc = new KeyHandlerClass();
                String publicKey = khc.decodeKeyToString(peer.getPublicKey());
                data += peer.getGap() + publicKey;
                peer.sendMessage(data);
            } else if (message.contains("release")) { //&& peer.getPeerStatus().equals("SHARING_RESOURCES")) {
                String[] msg = message.split(" ");
                // Envia o nome do recurso e o comando
                String resourceName = msg[1];
                String data = "RELEASE" + peer.getGap() + resourceName;

                // Envia a chave do par que vai usar o recurso
                KeyHandlerClass khc = new KeyHandlerClass();
                String publicKey = khc.decodeKeyToString(peer.getPublicKey());
                data += peer.getGap() + publicKey;
                peer.sendMessage(data);
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
        // Envia a chave do par decodificada e o comando
        KeyHandlerClass khc = new KeyHandlerClass();
        String pbKString = khc.decodeKeyToString(peer.getPublicKey());
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
