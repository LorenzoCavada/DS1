package EasyCache.Messages;

import akka.actor.ActorRef;

import java.util.Stack;

// Represent the request of reading the value of the element identify by the key.
// The responsePath is a stack of actors that represent the path that the request has followed.
// This message will be originated by a client which will push its actorRef into the stack and than send the message to the L2 cache.
// The L2 cache will push its actorRef into the stack and then send the message to the L1 cache.
// The L1 cache will push its actorRef into the stack and then send the message to the DB.
public class ReadReqMsg extends Message{
    public final int key;   // key of requested item
    public Stack<ActorRef> responsePath;

    public ReadReqMsg(int key) {
        this.key=key;
        this.responsePath=new Stack<>();
    }
}
