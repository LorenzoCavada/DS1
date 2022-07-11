package EasyCache.Messages;

import akka.actor.ActorRef;

import java.util.UUID;

// Represent the request of writing a new value in the element identify by the key
// Is also included the originator of the request, this is for sending the confirmation of the write operation
// This message will be originated by a client and sent to a L2 cache, then will be forwarded to a L1 cache and finally to the DB
public class CritWriteReqMsg extends ReqMessage{
    public final int newValue; //new value of item
    public ActorRef originator; //originator of request

    public CritWriteReqMsg(int key, int newValue, ActorRef originator) {
        super(key);
        this.newValue=newValue;
        this.originator=originator;
    }

    public CritWriteReqMsg(int key, UUID uuid, int newValue, ActorRef originator) {
        super(key, uuid);
        this.newValue=newValue;
        this.originator=originator;
    }
}
