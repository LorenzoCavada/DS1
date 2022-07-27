package EasyCache.Messages;

import akka.actor.ActorRef;

import java.util.ArrayList;
import java.util.List;

/**
 * This message is used by the {@link EasyCache.ProjectRunner runner} to tell a {@link EasyCache.Devices.Cache cache} or
 * the {@link EasyCache.Devices.DB database} which are its children.
 * The {@link EasyCache.Devices.DB database} will receive a list of L1 {@link EasyCache.Devices.Cache caches}, the L1
 * {@link EasyCache.Devices.Cache caches} will receive a list of L2 {@link EasyCache.Devices.Cache caches} and the L2
 * {@link EasyCache.Devices.Cache caches} will receive a list of {@link EasyCache.Devices.Client clients}.
 */
public class SetChildrenMsg extends Message {
    public List<ActorRef> children;   // key of requested item
    public SetChildrenMsg(List<ActorRef> children) {
        this.children=new ArrayList<>(children);
    }
}
