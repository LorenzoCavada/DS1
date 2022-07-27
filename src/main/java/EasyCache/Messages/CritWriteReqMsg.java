package EasyCache.Messages;

import akka.actor.ActorRef;

import java.util.UUID;

/**
 * This message represents the request of critically writing a new value in the element identify by the key.
 * The message is used like in {@link WriteReqMsg}.
 */
public class CritWriteReqMsg extends WriteReqMsg {

    public CritWriteReqMsg(int key, int newValue, ActorRef originator) {
        super(key, newValue, originator);
    }

    public CritWriteReqMsg(int key, UUID uuid, int newValue, ActorRef originator) {
        super(key, uuid, newValue, originator);
    }
}
