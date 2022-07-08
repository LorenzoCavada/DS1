package EasyCache.Messages;

import akka.actor.ActorRef;

import java.util.Stack;

// Represent the request of refreshing an item identify by the key.
// The responsePath is a stack of actors that represent the path that the request has followed.
// This message will be originated by a cache after a crash and needs to check if the value saved in himself needs to be updated
// The L2 cache will push its actorRef into the stack and then send the message to the L1 cache.
// The L1 cache will push its actorRef into the stack and then send the message to the DB.
public class RefreshItemReqMsg extends ReqMessage{
    public Stack<ActorRef> responsePath;

    public RefreshItemReqMsg(int key) {
        super(key);
        this.responsePath=new Stack<>();
    }
}
