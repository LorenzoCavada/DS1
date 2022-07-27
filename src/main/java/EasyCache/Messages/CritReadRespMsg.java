package EasyCache.Messages;

import akka.actor.ActorRef;

import java.util.Stack;
import java.util.UUID;

/**
 * This message represents the response of a critical reading. The message is used like in {@link ReadRespMsg}.
 */
public class CritReadRespMsg extends ReadRespMsg{

    public CritReadRespMsg(int key, int value, Stack<ActorRef> stack, UUID uuid) {
        super(key, value, stack, uuid);
    }
}

