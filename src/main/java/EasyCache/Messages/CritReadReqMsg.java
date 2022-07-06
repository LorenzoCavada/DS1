package EasyCache.Messages;

import akka.actor.ActorRef;

import java.util.Stack;
import java.util.UUID;

// Represent the request of reading critically the value of the element identify by the key.
// The responsePath is a stack of actors that represent the path that the request has followed.
// This message will be originated by a client which will push its actorRef into the stack and than send the message to the L2 cache.
// The L2 cache will push its actorRef into the stack and then send the message to the L1 cache.
// The L1 cache will push its actorRef into the stack and then send the message to the DB.
public class CritReadReqMsg extends ReqMessage{
    public Stack<ActorRef> responsePath;

    public CritReadReqMsg(int key) {
        super(key);
        this.responsePath=new Stack<>();
    }
}
