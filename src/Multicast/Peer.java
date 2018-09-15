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
        createKeyList();
        sendMessage("The peer: " + name + " has joined.");

        createResources();
        createAnswerVerification();

        setPeerStatus("WAITING_PEERS");
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

    public void sendPubKeys () {
        KeyHandlerClass ksc = new KeyHandlerClass();
        String publicKey = ksc.decodeKeyToString(pbK);
        sendMessage("Sending public key" + gap + publicKey);
    }

    public void addPbKToList (PublicKey key) {
        peerPbKList.add(key);
        sendPubKeys();
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
            resourceHash.put(f.getName(), new ResourceClass(f.getName(), pbK));
        }
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

    public void setPeerStatus (String status) {
        if (status.equals("WAITING_PEERS") || status.equals("SHARING_RESOURCES")){
            peerStatus = status;
        } else {
            System.out.println("Set Program Status Error: UNKNOWN STATUS");
        }
        if (peerStatus.equals("SHARING_RESOURCES")) {
            System.out.println("3 or more peers joined the group!");
            System.out.println("  -> Now you can share resources!");
        }
    }

    public String getPeerStatus () { return peerStatus; }

    public List<String> getResourceNameFromHash () {
        return new ArrayList<String>(resourceHash.keySet());
    }

    public List<String> getMyResource() { return myResource; }

    public String getResourceStatusFromHash (String resourceName) {
        return resourceHash.get(resourceName).getResourceStatus();
    }

    public void removeResource (PublicKey ownerPeer, String[] resource) {
        for (String resourceName : resource) {
            ResourceClass resourceToForget = resourceHash.get(resourceName);

            // TODO: arrumar erro do comando abaixo
            resourceToForget.removeOwnerPeer(ownerPeer);

            if (resourceToForget.numberOfOwnerPeers() == 0) { // Se nenhum outro par possui o mesmo recurso, retira
                System.out.println("Resource: " + resourceName + " is now gone");
                resourceHash.remove(resourceName);
            } else { // Caso contrário, não retira o recurso
                System.out.println("Peer with resource: " + resourceName + " left, but someone else still has it");
                resourceHash.replace(resourceName, resourceToForget);
            }
        }
    }
}
