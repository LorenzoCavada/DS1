package EasyCache.Messages;

import akka.actor.ActorRef;

import java.util.Stack;
import java.util.UUID;

/**
 * Represent the request of reading the value of the element identified by the key.
 * ResponsePath is a stack of actors that are on the path that the request follows.
 * This message will be originated by a {@link EasyCache.Devices.Client client} which will push its actorRef into the stack
 * and then send the message to the L2 {@link EasyCache.Devices.Cache cache}.
 */
public class ReadReqMsg extends IdMessage {
    public Stack<ActorRef> responsePath;

    public ReadReqMsg(int key) {
        super(key);
        this.responsePath=new Stack<>();
    }

    public ReadReqMsg(int key, UUID uuid) {
        super(key, uuid);
        this.responsePath=new Stack<>();
    }
}
