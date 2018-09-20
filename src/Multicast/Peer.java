package Multicast;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class Peer {
    private String name;
    private MulticastSocket socket;
    private InetAddress group; // Endereço do Grupo Multicast

    private PrivateKey pvK;
    private PublicKey pbK;
    private Signature rsaSigner;
    private List<PublicKey> peerPbKList; // Lista de pares conectados ao grupo

    private String peerStatus; // Status do programa (Esperando por peers / Compartilhando recursos)
    private String gap = " --- "; // Espaçamento para identificar os dados no envio das mensagens

    private List<String> myResource; // Lista de recursos do par
    private HashMap<String, ResourceClass> resourceHash; // Hash de recursos de outros pares conhecidos

    private List<PublicKey> answeredPeers; // Lista de pares que responderam o último recurso
    private int answer; // Número de pares que responderam

    public Peer (String n, MulticastSocket s, InetAddress g) {
        name = n; socket = s; group = g;

        createKeys();
        createResources();
        createKeyList();
        sendMessage("The peer: " + name + " has joined.");

        createAnswerVerification();

        setPeerMode();
    }

    public void sendMessage(String m) {
        SendThread sendM = new SendThread(socket, group, m);
        sendM.start();
    }

    private void createKeys() {
        KeyPairGenerator keyPairGenerator = null;
        SecureRandom rand = null;
        try {
            keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            rand = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            System.out.println("Key failure!");
            System.exit(1);
        }
        keyPairGenerator.initialize(1024, rand);
        KeyPair pair = keyPairGenerator.generateKeyPair();
        pvK = pair.getPrivate();
        pbK = pair.getPublic();
        try {
            rsaSigner = Signature.getInstance("SHA1withRSA");
            rsaSigner.initSign(pvK);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            System.out.println("Exception: " + e.getMessage() + " by signing keys.");
        }
    }

    private void createKeyList () {
        peerPbKList = new ArrayList<>();
        addPbKToList(pbK);
    }

    public void sendPubKeysAndResource() {
        // Envia a chave do par
        KeyHandlerClass ksc = new KeyHandlerClass();
        String publicKey = ksc.decodeKeyToString(pbK);
        String data = "ENTRY" + gap + publicKey;
        // Envia o(s) recurso(s) do par
        String resources = String.join("\n", myResource);
        data += gap + resources;
        sendMessage(data);
    }

    public void addPbKToList (PublicKey key) {
        peerPbKList.add(key);
        sendPubKeysAndResource();
    }

    public boolean containsPbK (PublicKey key) {
        return peerPbKList.contains(key);
    }
    public void removeKeyFromList (PublicKey key) {
        peerPbKList.remove(key);
    }

    public void listingKeys () {
        System.out.println("Listing all the public keys: ");
        int count = 0;
        String keyMsg;
        KeyHandlerClass ksc = new KeyHandlerClass();
        for (PublicKey key : peerPbKList) {
            keyMsg = ksc.decodeKeyToString(key);
            count++;
            System.out.println(count + " : Public Key: " + keyMsg);
        }
    }

    public void createResources () {
        List<File> filesInFolder = null;
        try {
            filesInFolder = Files.walk(Paths.get("./shared/" + name))
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            System.out.println("IO: " + e.toString() + " at resource directory");
        }

        myResource = new ArrayList<>();
        resourceHash = new HashMap<>();

        for (File f : filesInFolder) {
            myResource.add(f.getName());
            addResource(f.getName(), pbK);
        }
    }

    public void addResource (String resourceName, PublicKey key) {
        resourceHash.put(resourceName, new ResourceClass(resourceName, key));
    }

    public void createAnswerVerification () {
        answeredPeers = new ArrayList<>();
        answer = 0;
    }

    public PublicKey getPublicKey () { return pbK; }
    public String getName() { return name; }
    public String getGap () { return gap; }
    public MulticastSocket getSocket() { return socket; }
    public InetAddress getGroup() { return group; }

    // Seta o status do par automaticamente quando se tem 3 ou mais pares conectados
    public void setPeerMode () {
        if (peerPbKList.size() < 2) {
            setPeerStatus("WAITING_PEERS");
        } else {
            setPeerStatus("SHARING_RESOURCES");
        }
    }

    public void setPeerStatus (String status) {
        if (status.equals("WAITING_PEERS")) {
            peerStatus = status;
        } else if (status.equals("SHARING_RESOURCES")) {
            if (peerPbKList.size() > 2) {
                peerStatus = status;
                System.out.println("3 or more peers joined the group!");
                System.out.println("  -> Now you can share resources!");
            } else {
                System.out.println("Can't change Peer Status: Not enough peers connected");
            }
        } else {
            System.out.println("Set Program Status Error: UNKNOWN STATUS");
        }
    }

    public int getConnectedPeersSize() { return peerPbKList.size();}
    public String getPeerStatus () { return peerStatus; }
    public List<String> getResourceNameFromHash () {
        return new ArrayList<String>(resourceHash.keySet());
    }
    public ResourceClass getResourceFromHash (String resourceName) { return resourceHash.get(resourceName); }
    public List<String> getMyResource() { return myResource; }

    public String getResourceStatusFromHash (String resourceName) {
        return resourceHash.get(resourceName).getResourceStatus();
    }

    public void useResource (String resourceName) {
        ResourceClass resource = resourceHash.get(resourceName);
        resource.setResourceStatus("HELD");
        resourceHash.put(resourceName, resource);
    }
    public void wantResource (String resourceName) {
        ResourceClass resource = resourceHash.get(resourceName);
        resource.setResourceStatus("WANTED");
        resourceHash.put(resourceName, resource);
    }
    public void releaseResource (String resourceName) {
        ResourceClass resource = resourceHash.get(resourceName);
        resource.setResourceStatus("RELEASED");
        resourceHash.put(resourceName, resource);
    }

    public void removeResource (PublicKey ownerPeer, String[] resource) {
        for (String resourceName : resource) {
            ResourceClass resourceToForget = resourceHash.get(resourceName);

            resourceToForget.removeOwnerPeer(ownerPeer);

            if (resourceToForget.numberOfOwnerPeers() == 0) { // Se nenhum outro par possui o mesmo recurso, retira
                System.out.println("Resource: " + resourceName + " is now gone");
                resourceHash.remove(resourceName);
            } else { // Caso contrário, não retira o recurso
                System.out.println("Peer with resource: " + resourceName + " has left, but someone else still has it");
                resourceHash.replace(resourceName, resourceToForget);
            }
        }
    }

    public void initializeAnswers () {
        answer = 0;
        answeredPeers.clear();
    }
    public void addAnsweredPeer (PublicKey key) { answeredPeers.add(key); }
    public int getAnsweredPeerSize () { return answeredPeers.size(); } // Usado para verificar se algum par caiu
    public int getAnswer () { return answer; } // Retorna o numero de pares que responderam positivamente
    public void incrementAnswer () { answer++; }

    public List<PublicKey> findNotAnsweredPeer () {
        List<PublicKey> notAnsweredPeers = new ArrayList<>();
        for(PublicKey key : peerPbKList) {
            boolean answered = answeredPeers.contains(key);
            if(!answered) {
                notAnsweredPeers.add(key);
            }
        }
        return notAnsweredPeers;
    }


}
