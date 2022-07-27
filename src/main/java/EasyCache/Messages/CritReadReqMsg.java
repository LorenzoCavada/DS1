package EasyCache.Messages;

import akka.actor.ActorRef;

import java.util.Stack;
import java.util.UUID;

/**
 * This message represents the request of reading critically the value of the element identified by the key.
 * The message is used like in {@link ReadReqMsg}.
 */
public class CritReadReqMsg extends ReadReqMsg {

    public CritReadReqMsg(int key) {
        super(key);
    }
    public CritReadReqMsg(int key, UUID uuid) {
        super(key, uuid);
    }
}
