package EasyCache.Messages;

import akka.actor.ActorRef;

import java.util.ArrayList;
import java.util.List;

/**
 * This message is used by the {@link EasyCache.ProjectRunner runner} to tell a {@link EasyCache.Devices.Client client} the
 * list of available L2 {@link EasyCache.Devices.Cache caches}.
 * The {@link EasyCache.Devices.Client client} will use this list to randomly select a new parent when its old parent crashes.
 */
public class SetAvailableL2Msg extends Message {
    public List<ActorRef> availL2;   // key of requested item
    public SetAvailableL2Msg(List<ActorRef> availL2) {
        this.availL2 =new ArrayList<>(availL2);
    }
}
