package EasyCache.Messages;

import akka.actor.ActorRef;

import java.util.UUID;

/**
 * This message is used by the {@link EasyCache.Devices.DB database} when it receives a {@link CritWriteReqMsg critical write request}
 * for an item there is already an ongoing {@link CritWriteReqMsg critical write} or when it goes in timeout while waiting for the
 * reception of {@link InvalidationItemConfirmMsg invalidation confirmations}.
 */
public class CritWriteErrorMsg extends IdMessage{
    public ActorRef originator;
    public CritWriteErrorMsg(int key, ActorRef originator, UUID uuid){
        super(key, uuid);
        this.originator=originator;
    }
}
