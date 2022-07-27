package EasyCache.Messages;

import akka.actor.ActorRef;

import java.util.Stack;
import java.util.UUID;

/**
 * Represent the response to a {@link RefreshItemReqMsg refresh request}.
 * The response is sent by the {@link EasyCache.Devices.DB database} to the requester of the associated {@link RefreshItemReqMsg refresh request}
 * thanks to the ResponsePath stack, that contain the whole chain of actors that the request has crossed. The message
 * contains both the key of the requested item and the value of the requested item.
 */
public class RefreshItemRespMsg extends ReadRespMsg{

    public RefreshItemRespMsg(int key, int value, Stack<ActorRef> stack, UUID uuid) {
        super(key, value, stack, uuid);
    }
}
