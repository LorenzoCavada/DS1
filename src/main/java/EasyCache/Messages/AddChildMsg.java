package EasyCache.Messages;

import akka.actor.ActorRef;

import java.util.ArrayList;
import java.util.List;

/**
 * This message is used to add a new child to the list of children of an actor.
 * It will be sent to {@link EasyCache.Devices.Cache cache} or {@link EasyCache.Devices.DB database} to inform them of a new child.
 */

public class AddChildMsg extends Message {
    public ActorRef child;   // key of requested item
    public AddChildMsg(ActorRef child) {
        this.child=child;
    }
}
