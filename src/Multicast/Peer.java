package Multicast;

import java.net.InetAddress;
import java.net.MulticastSocket;
import java.security.*;
import java.util.ArrayList;
import java.util.List;

public class Peer {
    private String name;
    private MulticastSocket socket;
    private InetAddress group;
    private PrivateKey pvK;
    private PublicKey pbK;
    private List<PublicKey> peerPbKList;
    private String gap = " --- ";

    public Peer (String n, MulticastSocket s, InetAddress g) {
        name = n;
        socket = s;
        group = g;
        String notification = "The peer: " + name + " has joined.";
        sendMessage(notification);
        createKeys();
        sendPubKeys();
    }

    public void sendMessage(String m) {
        SendThread sendM = new SendThread(socket, group, m);
        sendM.start();
    }

    private void createKeys() {
        KeyPairGenerator keyPairGenerator;
        KeyPair pair;
        try {
            keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(512);
            pair = keyPairGenerator.generateKeyPair();
            pvK = pair.getPrivate();
            pbK = pair.getPublic();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            System.out.println("Key failure!");
            System.exit(1);
        }
        createKeyList();
    }

    private void createKeyList () {
        peerPbKList = new ArrayList<>();
    }

    public void sendPubKeys () {
        KeySignatureClass ksc = new KeySignatureClass();
        String publicKey = ksc.decodeKeyToString(pbK);
        sendMessage("PUBLIC KEY:" + publicKey);
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
        KeySignatureClass ksc = new KeySignatureClass();
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

    //public List<PublicKey> getPeerPbKList () { return peerPbKList; }
}
