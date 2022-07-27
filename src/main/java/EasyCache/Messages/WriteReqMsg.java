package EasyCache.Messages;

import akka.actor.ActorRef;

import java.util.UUID;

/**
 * This message represents the request of writing a new value in the element identify by the key.
 * Originator is the {@link ActorRef reference} of the {@link EasyCache.Devices.Client client} that performed the request.
 * The originator will send the message to a L2 {@link EasyCache.Devices.Cache cache}.
 */
public class WriteReqMsg extends IdMessage {
    public final int newValue; //new value of item
    public ActorRef originator; //originator of request

    public WriteReqMsg(int key, int newValue, ActorRef originator) {
        super(key);
        this.newValue=newValue;
        this.originator=originator;
    }

    public WriteReqMsg(int key, UUID uuid, int newValue, ActorRef originator) {
        super(key, uuid);
        this.newValue=newValue;
        this.originator=originator;
    }
}
