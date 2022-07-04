package EasyCache.Messages;

import akka.actor.ActorRef;

import java.util.UUID;

// Represent the request of refilling the cache with a new element. Is initially sent by the server to the L1 cache after a write operation
// Each L1 cache will then update its cache if the element is already saved in its cache, the L1 cache will then multicast the message to its children
// Each L2 cache will then update its cache if the element is already saved in its cache, if the originator is one of its children, the L2 cache will send a confirmation to the originator
public class RefillMsg extends Message{
    public final int key;   // key of item to be written
    public final int newValue; //new value of item
    public ActorRef originator;
    public final UUID uuid;

    public RefillMsg(int key, int newValue, ActorRef originator, UUID uuid) {
        this.key=key;
        this.newValue=newValue;
        this.originator=originator;
        this.uuid = uuid;
    }
}
