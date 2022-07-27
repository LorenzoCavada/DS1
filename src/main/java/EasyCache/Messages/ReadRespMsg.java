package EasyCache.Messages;

import akka.actor.ActorRef;

import java.util.Stack;
import java.util.UUID;

/**
 * Represent the response to a {@link ReadReqMsg read request}.
 * The response is sent by the {@link EasyCache.Devices.DB database} to the requester of the associated {@link ReadReqMsg request}
 * thanks to the ResponsePath stack, that contain the whole chain of actors that the request has crossed. The message contains
 * both the key of the requested item and the value of the requested item.
 */
public class ReadRespMsg extends IdMessage{
    public final int value; //value of requested item
    public Stack<ActorRef> responsePath;


    public ReadRespMsg(int key, int value, Stack<ActorRef> stack, UUID uuid) {
        super(key, uuid);
        this.responsePath= (Stack<ActorRef>) stack.clone();
        this.value = value;
    }
}

