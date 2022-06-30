package it.unitn.ds1.CavadaBrighenti.FinalProject.Messages;

import akka.actor.ActorRef;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

// This message is used to set the list of available L2 caches.
// Is intended to be sent to the client to inform him to which L2 caches he can connect.
public class SetAvailableL2Msg extends Message {
    public List<ActorRef> availL2;   // key of requested item
    public SetAvailableL2Msg(List<ActorRef> availL2) {
        this.availL2 =new ArrayList<>(availL2);
    }
}
