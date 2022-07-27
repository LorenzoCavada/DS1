package EasyCache.Messages;

import akka.actor.ActorRef;

import java.util.UUID;

/**
 * This message is used to refill {@link EasyCache.Devices.Cache caches} with the new value of an element they already have saved during a {@link WriteReqMsg write}.
 * It is initially sent by the {@link EasyCache.Devices.DB database} to its children after it applied the {@link WriteReqMsg write}.
 * This message is ultimately propagated to all alive L2 {@link EasyCache.Devices.Cache caches}.
 */
public class RefillMsg extends IdMessage{
    public final int newValue; //new value of item
    public ActorRef originator;

    public RefillMsg(int key, int newValue, ActorRef originator, UUID uuid) {
        super(key,uuid);
        this.newValue=newValue;
        this.originator=originator;
    }
}
