package Multicast;

import java.security.PublicKey;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class ResourceClass {
    private String name;
    private String resourceStatus;
    private List<PublicKey> ownerPeer;
    private Queue<PublicKey> peerToAccess;

    public ResourceClass (String n, PublicKey o) {
        name = n;
        ownerPeer = new ArrayList<>();
        addOwnerPeer(o);
        peerToAccess = new ArrayDeque<>();
        setResourceStatus("RELEASED");
    }

    public void setResourceStatus (String status) {
        if (status.equals("RELEASED") || status.equals("HELD") || status.equals("WANTED")){
            resourceStatus = status;
        } else {
            System.out.println("Set Resource Status Error: UNKNOWN STATUS");
        }
    }

    public String getResourceStatus () { return resourceStatus; }
    public int numberOfOwnerPeers () { return ownerPeer.size(); }
    public void addOwnerPeer (PublicKey peer) { ownerPeer.add(peer); }
    public void removeOwnerPeer (PublicKey peer) { ownerPeer.remove(peer); }
    public void addPeersToQueue (PublicKey peer) { peerToAccess.add(peer); }
    public PublicKey takePeerOutFromQueue() { return peerToAccess.poll(); }
    public void removePeerFromQueue() { peerToAccess.remove(); }
    public int getQueueSize () { return peerToAccess.size(); }
    public boolean isOwner (PublicKey key) { return ownerPeer.contains(key); }
}
