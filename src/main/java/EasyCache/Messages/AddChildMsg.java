package EasyCache.Messages;

import akka.actor.ActorRef;

import java.util.ArrayList;
import java.util.List;

// This message is used to set the list of children of a node.
// Is intended to be sent to the cache and the DB to inform them of the list of the children associate to them.
// The DB will receive a list of L1 caches, the L1 caches will receive a list of L2 caches and the L2 caches will receive a list of clients.
public class AddChildMsg extends Message {
    public ActorRef child;   // key of requested item
    public AddChildMsg(ActorRef child) {
        this.child=child;
    }
}
