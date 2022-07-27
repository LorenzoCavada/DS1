package EasyCache.Messages;

import akka.actor.ActorRef;

/**
 * This message is used by the {@link EasyCache.ProjectRunner runner} to tell an actor who is its parent.
 * The L1 {@link EasyCache.Devices.Cache caches} will set as parent the {@link EasyCache.Devices.DB database}, the L2
 * {@link EasyCache.Devices.Cache caches} will set a L1 {@link EasyCache.Devices.Cache caches} and clients will set a L2
 * {@link EasyCache.Devices.Cache cache}.
 */
public class SetParentMsg extends Message{
    public ActorRef parent;
    public SetParentMsg(ActorRef parent) {
        this.parent=parent;
    }
}
