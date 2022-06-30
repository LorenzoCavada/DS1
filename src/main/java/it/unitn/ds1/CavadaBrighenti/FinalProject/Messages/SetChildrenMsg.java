package it.unitn.ds1.CavadaBrighenti.FinalProject.Messages;

import akka.actor.ActorRef;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SetChildrenMsg extends Message {
    public List<ActorRef> children;   // key of requested item
    public SetChildrenMsg(List<ActorRef> children) {
        this.children=new ArrayList<>(children);
    }
}
