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

// TODO: assinar as mensagens enviadas!
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
            } else if (msg[0].equals("LEAVE")){
                // Recebeu a chave do par a ser retirado
                KeyHandlerClass khc = new KeyHandlerClass();
                PublicKey key = khc.encodeStringToKey(msg[1]);
                peer.removeKeyFromList(key);

                // Recebeu o(s) recurso(s) para ser(em) retirado(s)
                String resource = msg[2];
                String[] resourceList = resource.split("\n");
                peer.removeResource(key, resourceList);
            } else if (msg[0].equals("ADD_PEER")) {
                // Todos os outros pares adicionam um determinado par na fila
                String keyString = msg[1];
                String resourceName = msg[2];
                KeyHandlerClass khc = new KeyHandlerClass();
                PublicKey publicKey = khc.encodeStringToKey(keyString);
                peer.getResourceFromHash(resourceName).addPeersToQueue(publicKey);
            } else if (msg[0].equals("NEXT_IN_LINE")) {
                String resourceName = msg[1];
                // Pega o proximo par da fila de pares que querem acessar o recurso
                PublicKey publicKey = peer.getResourceFromHash(resourceName).takePeerOutFromQueue();
                // Se o par for o proximo, usa o recurso
                if (peer.getPublicKey().equals(publicKey)) {
                    peer.useResource(resourceName);
                    System.out.println("You are now using the resource: " + resourceName);
                }
            } else if (msg[0].equals("ANSWER")) {
                String resourceName = msg[1];
                // adiciona o par que vai responder na lista
                KeyHandlerClass khc = new KeyHandlerClass();
                String key = khc.decodeKeyToString(peer.getPublicKey());
                if (peer.getResourceStatusFromHash(resourceName).equals("RELEASED")) { // se não estiver usando, pode usar
                    // manda resposta positiva
                    peer.sendMessage("YES" + peer.getGap() + key);
                } else { // Se estiver usando ou querendo usar (esta "antes" na fila)
                    // manda resposta negativa
                    peer.sendMessage("NO" + peer.getGap() + key);
                }
            } else if (msg[0].equals("YES")) {
                System.out.println(msg[0]);
                String keyString = msg[1];
                KeyHandlerClass khc = new KeyHandlerClass();
                PublicKey key = khc.encodeStringToKey(keyString);
                peer.incrementAnswer();
                peer.addAnsweredPeer(key);
            } else if (msg[0].equals("NO")) {
                System.out.println(msg[0]);
                String keyString = msg[1];
                KeyHandlerClass khc = new KeyHandlerClass();
                PublicKey key = khc.encodeStringToKey(keyString);
                peer.addAnsweredPeer(key);
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
                quitAction(peer.getPublicKey());
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
                // TIMEOUT de 2 segundos para o envio das respostas
                suspendTime(2);
                // Se nem todos os pares responderam...
                if (peer.getAnsweredPeerSize() < peer.getConnectedPeersSize()) {
                    // Procura os pares que não responderam
                    List<PublicKey> notAnswered = peer.findNotAnsweredPeer();
                    // Desconecta os pares que não responderam
                    for (PublicKey key : notAnswered){
                        System.out.println("The peer has been disconnected!");
                        quitAction(key);
                    }
                } else { // Se todos responderam, usa o recurso
                    if (peer.getConnectedPeersSize() == peer.getAnswer()) { // Ninguém esta usando/quer o recurso, pode usar
                        peer.useResource(resourceName);
                    } else if (peer.getConnectedPeersSize() > peer.getAnswer()) {
                        // Se não recebeu resposta positiva de todos os pares conectados, então alguém esta usando/querendo usar
                        // Seta o status do recurso como WANTED
                        peer.wantResource(resourceName);
                        // E adiciona na fila peerToAccess da ResourceClass
                        // TODO: todos os pares precisam adicionar esse par na lista
                        // Envia o par para outros pares adicionarem ele na fila
                        KeyHandlerClass khc = new KeyHandlerClass();
                        String publicKey = khc.decodeKeyToString(peer.getPublicKey());
                        peer.sendMessage("ADD_PEER" + peer.getGap() + publicKey + peer.getGap() + resourceName);
                        // Se adiciona na fila de pares que precisam acessar o recurso
                        peer.getResourceFromHash(resourceName).addPeersToQueue(peer.getPublicKey());
                    }
                }
            } else if (message.contains("release")) { //&& peer.getPeerStatus().equals("SHARING_RESOURCES")) {
                String[] msg = message.split(" ");
                String resourceName = msg[1];
                if (peer.getResourceStatusFromHash(resourceName).equals("HELD")) {
                    peer.releaseResource(resourceName);
                    // se tiver alguém na fila
                    if (peer.getResourceFromHash(resourceName).getQueueSize() > 0) {
                        // se liberou, deixa o proximo da fila usar o recurso
                        peer.getResourceFromHash(resourceName).removePeerFromQueue();
                        peer.sendMessage("NEXT_IN_LINE" + peer.getGap() + resourceName);
                    }
                } else {
                    System.out.println("ERROR: Resource is not being used!");
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

    public void quitAction (PublicKey peerKey) {
        // Envia a chave do par decodificada e o comando
        KeyHandlerClass khc = new KeyHandlerClass();
        String pbKString = khc.decodeKeyToString(peerKey);
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
