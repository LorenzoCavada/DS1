package EasyCache.Messages;

import akka.actor.ActorRef;

import java.util.Stack;
import java.util.UUID;

// Represent the response of a reading request.
// The response is sent to the actor identified by the responsePath and contain both the key of the requested item and the value of the requested item
// The responsePath stack will contain the whole chain of actors that the request has crossed.
// The DB will get by popping the first element of the stack the actorRef to the L1 cache to which the request has been made
// This L1 cache will pop again the first element of the stack and will get the L2 cache which received the read request from the client
// This L2 cache will pop again the first element of the stack and will get the client that made the request so will be able to forward the response to him
public class ReadRespMsg extends IdMessage{
    public final int value; //value of requested item
    public Stack<ActorRef> responsePath;


    public ReadRespMsg(int key, int value, Stack<ActorRef> stack, UUID uuid) {
        super(key, uuid);
        this.responsePath= (Stack<ActorRef>) stack.clone();
        this.value = value;
    }
}

