package Multicast;

import java.security.PublicKey;
import java.util.*;

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

    /** Resource Status **/
    public String getResourceStatus () { return resourceStatus; } // retorna o status do recurso

    /** Owner Peers **/
    public int numberOfOwnerPeers () { return ownerPeer.size(); } // retorna o numero de pares donos do recurso
    public void addOwnerPeer (PublicKey peer) { ownerPeer.add(peer); } // adiciona um par na lista de donos
    public void removeOwnerPeer (PublicKey peer) { ownerPeer.remove(peer); } // remove um par da lista de donos
    public boolean isOwner (PublicKey key) { return ownerPeer.contains(key); } // verifica se o par é o dono do recurso

    /** Access Queue **/
    public void addPeersToQueue (PublicKey peer) { peerToAccess.add(peer); } // adiciona um par na fila de acesso
    public PublicKey takePeerOutFromQueue() { return peerToAccess.poll(); } // retorna e retira o proximo par da fila de acesso
    public void removePeerFromQueue() { peerToAccess.remove(); } // remove o proximo par da fila de acesso
    public int getQueueSize () { return peerToAccess.size(); } // retorna o tamanho da fila de acesso
}
