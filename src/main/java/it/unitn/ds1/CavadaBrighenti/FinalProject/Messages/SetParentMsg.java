package it.unitn.ds1.CavadaBrighenti.FinalProject.Messages;

import akka.actor.ActorRef;

public class SetParentMsg extends Message{
    public ActorRef parent;
    public SetParentMsg(ActorRef parent) {
        this.parent=parent;
    }
}
