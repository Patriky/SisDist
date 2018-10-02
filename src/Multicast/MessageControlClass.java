package Multicast;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Base64;
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
            // Executa de acordo com o comando
            if (msg[0].equals("ENTRY")) { // Executa comandos de entrada de um par
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
                String signature = msg[3];
                byte[] decript = Base64.getDecoder().decode(signature);
                String content = msg[0] + peer.getGap() + msg[1] + peer.getGap() + msg[2];
                // Se não pode verificar a assinatura do par que acabou de entrar, pode haver problemas de segurança
                if (!peer.verifySignature(decript, content)) {
                    // mostra mensagem de erro e encerra o progama
                    System.out.println("Signature Failure: Unknown peer!");
                    System.exit(-1);
                }
            } else if (msg[0].equals("LEAVE")) { // Executa comandos de saída de um par
                String signature = msg[3];
                byte[] decript = Base64.getDecoder().decode(signature);
                String content = msg[0] + peer.getGap() + msg[1] + peer.getGap() + msg[2];
                // Se foram mandadas chaves, deve-se verificar a assinatura
                if (!peer.verifySignature(decript, content)) {
                    System.out.println("ERROR: Signature could not be verified!");
                } else {
                    // Recebeu a chave do par a ser retirado
                    KeyHandlerClass khc = new KeyHandlerClass();
                    PublicKey key = khc.encodeStringToKey(msg[1]);
                    peer.removeKeyFromList(key);
                    // Recebeu o(s) recurso(s) para ser(em) retirado(s)
                    String resource = msg[2];
                    String[] resourceList = resource.split("\n");
                    peer.removeResource(key, resourceList);
                }
            } else if (msg[0].equals("ADD_PEER")) { // Adiciona pares na fila de acesso
                String signature = msg[3];
                byte[] decript = Base64.getDecoder().decode(signature);
                String content = msg[0] + peer.getGap() + msg[1] + peer.getGap() + msg[2];
                // Se foram mandadas chaves, deve-se verificar a assinatura
                if (!peer.verifySignature(decript, content)) {
                    System.out.println("ERROR: Signature could not be verified!");
                } else {
                    // Todos os outros pares adicionam um determinado par na fila
                    String keyString = msg[1];
                    String resourceName = msg[2];
                    KeyHandlerClass khc = new KeyHandlerClass();
                    PublicKey publicKey = khc.encodeStringToKey(keyString);
                    peer.getResourceFromHash(resourceName).addPeersToQueue(publicKey);
                }
            } else if (msg[0].equals("NEXT_IN_LINE")) { // O par da fila de acesso ganha recurso sem pedir permissão
                String resourceName = msg[1];
                // Pega o proximo par da fila de pares que querem acessar o recurso
                PublicKey nextPeer = peer.getResourceFromHash(resourceName).takePeerOutFromQueue();
                // Se o par for o proximo, usa o recurso
                if (peer.getPublicKey().equals(nextPeer)) {
                    peer.useResource(resourceName);
                    System.out.println("You are now using the resource: " + resourceName);
                }
            } else if (msg[0].equals("PERMISSION")) { // Responde se o par pode, ou não, usar o recurso
                String resourceName = msg[1];
                // adiciona o par que vai responder na lista
                KeyHandlerClass khc = new KeyHandlerClass();
                String key = khc.decodeKeyToString(peer.getPublicKey());
                if (peer.getResourceStatusFromHash(resourceName).equals("RELEASED")) { // se não estiver usando, pode usar
                    // manda resposta positiva
                    String data = "YES" + peer.getGap() + key;
                    byte[] signature = khc.sign(peer.getSignature(), data);
                    String encript = Base64.getEncoder().encodeToString(signature);
                    data += peer.getGap() + encript;
                    peer.sendMessage(data);
                } else { // Se estiver usando ou querendo usar (esta "antes" na fila)
                    // manda resposta negativa
                    String data = "NO" + peer.getGap() + key;
                    byte[] signature = khc.sign(peer.getSignature(), data);
                    String encript = Base64.getEncoder().encodeToString(signature);
                    data += peer.getGap() + encript;
                    peer.sendMessage(data);
                }
            } else if (msg[0].equals("YES")) { // Armazena resposta positiva
                String signature = msg[2];
                byte[] decript = Base64.getDecoder().decode(signature);
                String content = msg[0] + peer.getGap() + msg[1];
                if (!peer.verifySignature(decript, content)) {
                    System.out.println("ERROR: Signature could not be verified!");
                } else {
                    System.out.println(msg[0]);
                    String keyString = msg[1];
                    KeyHandlerClass khc = new KeyHandlerClass();
                    PublicKey key = khc.encodeStringToKey(keyString);
                    peer.incrementAnswer();
                    peer.addAnsweredPeer(key);
                }
            } else if (msg[0].equals("NO")) { // Armazena resposta negativa
                String signature = msg[2];
                byte[] decript = Base64.getDecoder().decode(signature);
                String content = msg[0] + peer.getGap() + msg[1];
                if (!peer.verifySignature(decript, content)) {
                    System.out.println("ERROR: Signature could not be verified!");
                } else {
                    System.out.println(msg[0]);
                    String keyString = msg[1];
                    KeyHandlerClass khc = new KeyHandlerClass();
                    PublicKey key = khc.encodeStringToKey(keyString);
                    peer.addAnsweredPeer(key);
                }
            }
        } else if (!message.equals("")) { // Imprime mensagem qualquer
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
            } else if (message.equals("status")) {
                List<String> resourceNameList = peer.getResourceNameFromHash();
                System.out.println("Resource Status:");
                for (String resourceName : resourceNameList) {
                    System.out.println("Name: " + resourceName + " \t " + "Status: "
                            + peer.getResourceStatusFromHash(resourceName));
                }
            } else if (message.equals("require") || message.equals("release")
                    || message.equals("require ") || message.equals("release ")) {
                System.out.println("ERROR: You must require something!");
                System.out.println("Insert \"help\" to see the list of available commands.");
            } else if (peer.getPeerStatus().equals("SHARING_RESOURCES")) {
                if (message.contains("require")) {
                    String[] msg = message.split(" ");
                    String resourceName = msg[1];
                    if (peer.resourceExists(resourceName)) {
                        requireAction(resourceName);
                    } else {
                        System.out.println("ERROR: Resource not found.");
                    }
                } else if (message.contains("release")) {
                    String[] msg = message.split(" ");
                    String resourceName = msg[1];
                    if (peer.resourceExists(resourceName)) {
                        if (peer.getResourceStatusFromHash(resourceName).equals("HELD")) {
                            peer.releaseResource(resourceName);
                            System.out.println("Resource released.");
                            // se alguém estiver na fila
                            if (peer.getResourceFromHash(resourceName).getQueueSize() > 0) { // o proximo da fila usa o recurso
                                // remove o par da fila, não se pode dar require 2 vezes no mesmo recurso
                                peer.getResourceFromHash(resourceName).removePeerFromQueue();
                                // envia mensagem pra achar o par que vai ser o proximo a usar o recurso
                                peer.sendMessage("NEXT_IN_LINE" + peer.getGap() + resourceName);
                            }
                        } else { // não da pra liberar um recurso que não esta sendo usado
                            System.out.println("ERROR: Resource is not being used!");
                        }
                    } else {
                        System.out.println("ERROR: Resource not found.");
                    }
                }
            } else if (peer.getPeerStatus().equals("WAITING_PEERS")) {
                System.out.println("Still waiting for peers: Can't execute require/release commands");
            } else { // Envia mensagem qualquer
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

        // Já que está saindo, libera todos os recursos que está usando
        if (peer.getPeerStatus().equals("SHARING_RESOURCES")) {
            for (String resourceName : peer.getResourceNameFromHash()) {
                if (peer.getResourceStatusFromHash(resourceName).equals("HELD")) {
                    peer.releaseResource(resourceName);
                    if (peer.getResourceFromHash(resourceName).getQueueSize() > 0) {
                        // remove o par da fila, não se pode dar require 2 vezes no mesmo recurso
                        peer.getResourceFromHash(resourceName).removePeerFromQueue();
                        // envia mensagem pra achar o par que vai ser o proximo a usar o recurso
                        peer.sendMessage("NEXT_IN_LINE" + peer.getGap() + resourceName);
                        // suspende o tempo para enviar a mensagem
                        suspendTime(1);
                    }
                }
            }
        }

        // Envia a assinatura do par
        byte[] signature = khc.sign(peer.getSignature(), data);
        String encript = Base64.getEncoder().encodeToString(signature);
        data += peer.getGap() + encript;
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

    public void forceQuit (PublicKey key) {
        // Envia a chave do par decodificada e o comando
        KeyHandlerClass khc = new KeyHandlerClass();
        String pbKString = khc.decodeKeyToString(key);
        String data = "LEAVE" + peer.getGap() + pbKString;

        List<String> resourceList = new ArrayList<>();
        for (String resourceName : peer.getResourceNameFromHash()) {
            // Para cada recurso conhecido, descobrir quais tem a key como ownerPeer
            if(peer.getResourceFromHash(resourceName).isOwner(key)) {
                resourceList.add(resourceName);
            }
            // Se está saindo, libera todos os recursos que o par com key está usando
            if(peer.getPeerStatus().equals("SHARING_RESOURCES")
                    && peer.getResourceFromHash(resourceName).getQueueSize() > 0) {
                // envia mensagem para o próximo par usar o recurso
                peer.sendMessage("NEXT_IN_LINE" + peer.getGap() + resourceName);
                suspendTime(1);
            }
        }
        // Envia os recursos para serem retirados
        String resources = String.join("\n",resourceList);
        data += peer.getGap() + resources;
        // Valida com a assinatura do par que identificou que alguem não respondeu
        byte[] signature = khc.sign(peer.getSignature(), data);
        String encript = Base64.getEncoder().encodeToString(signature);
        data += peer.getGap() + encript;
        peer.sendMessage(data);
    }

    public void requireAction (String resourceName) {
        // Inicializa contador e registro de respostas
        peer.initializeAnswers();
        // Pergunta se tem permissão para usar o recurso
        peer.sendMessage("PERMISSION" + peer.getGap() + resourceName);
        // TIMEOUT de 2 segundos para o envio das respostas
        System.out.println("Waiting for answers...");
        suspendTime(2);
        // Se nem todos os pares responderam...
        if (peer.getAnsweredPeerSize() < peer.getConnectedPeersSize()) {
            // Procura os pares que não responderam
            List<PublicKey> notAnswered = peer.findNotAnsweredPeer();
            // Desconecta os pares que não responderam
            for (PublicKey key : notAnswered){
                System.out.println("The peer has been disconnected!");
                // Retira o par com key
                forceQuit(key);
            }
        } else { // Se todos responderam
            if (peer.getConnectedPeersSize() == peer.getAnswer()) { // Ninguém esta usando/quer o recurso, pode usar
                peer.useResource(resourceName);
            } else if (peer.getConnectedPeersSize() > peer.getAnswer()) { // Se alguém esta usando/querendo usar
                // Seta o status do recurso como WANTED se o par não estiver usando o recurso
                if (!peer.getResourceStatusFromHash(resourceName).equals("HELD")) {
                    peer.wantResource(resourceName);
                    // Envia o par para outros pares adicionarem ele na fila
                    KeyHandlerClass khc = new KeyHandlerClass();
                    String publicKey = khc.decodeKeyToString(peer.getPublicKey());
                    String data = "ADD_PEER" + peer.getGap() + publicKey + peer.getGap() + resourceName;
                    byte[] signature = khc.sign(peer.getSignature(), data);
                    String encript = Base64.getEncoder().encodeToString(signature);
                    data += peer.getGap() + encript;
                    peer.sendMessage(data);
                    // O par se adiciona na sua fila de acesso
                    peer.getResourceFromHash(resourceName).addPeersToQueue(peer.getPublicKey());
                } else {
                    System.out.println("ERROR: Using the same resource twice is not permitted!");
                }
            }
        }
    }

    public void showHelpMenu() {
        System.out.println(" - \"list\": Show a list of connected peers");
        System.out.println(" - \"require\" <resource_name>: The peer REQUIRE a resource to use");
        System.out.println(" - \"release\" <resource_name>: The peer RELEASE a resource");
        System.out.println(" - \"status\": Show the name and status (RELEASED, HELD, WANTED) of available resources");
        System.out.println(" - \"help\": Show help menu");
        System.out.println(" - \"exit\" or \"quit\": Exit/Quit the process and remove the peer from list");
    }
}
