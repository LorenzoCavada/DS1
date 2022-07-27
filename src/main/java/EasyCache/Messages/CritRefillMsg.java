package EasyCache.Messages;

import akka.actor.ActorRef;

import java.util.UUID;

/**
 * This message is used to refill {@link EasyCache.Devices.Cache caches} with the new value of an element they already have saved during a {@link CritWriteReqMsg critical write}.
 * This message works like in {@link RefillMsg}.
 */
public class CritRefillMsg extends RefillMsg{

    public CritRefillMsg(int key, int newValue, ActorRef originator, UUID uuid) {
        super(key, newValue, originator, uuid);
    }
}
