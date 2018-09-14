package Multicast;

import java.net.InetAddress;
import java.net.MulticastSocket;
import java.security.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Peer {
    private String name;
    private MulticastSocket socket;
    private InetAddress group;

    private PrivateKey pvK;
    private PublicKey pbK;
    private Signature rsaSigner;
    private List<PublicKey> peerPbKList; // Lista de pares conhecidos (conectados ao grupo)
    private String gap = " --- "; // Espaçamento para identificar os dados no envio das mensagens

    private List<String> resource; // Lista de recursos do par
    private HashMap<String, ResourceClass> resourceHash; // Hash de recursos conhecidos de outros pares conhecidos

    private List<PublicKey> answeredPeers; // Lista de pares que responderam o último recurso
    private int answer; // Número de pares que responderam

    public Peer (String n, MulticastSocket s, InetAddress g) {
        name = n;
        socket = s;
        group = g;
        String notification = "The peer: " + name + " has joined.";
        sendMessage(notification);
        createKeys();
        createKeyList();
        addPbKToList(pbK);
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

    public PublicKey getPublicKey () { return pbK; }
    public String getName() { return name; }
    public String getGap () { return gap; }
    public MulticastSocket getSocket() { return socket; }
    public InetAddress getGroup() { return group; }
}
