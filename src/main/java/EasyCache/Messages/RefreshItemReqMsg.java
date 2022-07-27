package EasyCache.Messages;

import akka.actor.ActorRef;

import java.util.Stack;

/**
 * This message represents the request of refreshing an element identified by the key.
 * ResponsePath is a stack of actors that are on the path that the request follows.
 * It is performed by a L2 {@link EasyCache.Devices.Cache cache}, which will push its actorRef into the stack and then
 * send the message to the L1 {@link EasyCache.Devices.Cache cache}.
 */
public class RefreshItemReqMsg extends ReadReqMsg {

    public RefreshItemReqMsg(int key) {
        super(key);
    }
}
