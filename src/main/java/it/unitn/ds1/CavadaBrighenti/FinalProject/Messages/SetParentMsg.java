package it.unitn.ds1.CavadaBrighenti.FinalProject.Messages;

import akka.actor.ActorRef;

// This message is used to set the parent of a node.
// Is intended to be sent to the caches and the clients to inform them of their parent.
// The L1 caches will receive the ActorRef of the DB, the L2 caches will receive an ActoRef of a L1 cache and the clients will receive an ActorRef of a L2 cache.
public class SetParentMsg extends Message{
    public ActorRef parent;
    public SetParentMsg(ActorRef parent) {
        this.parent=parent;
    }
}
