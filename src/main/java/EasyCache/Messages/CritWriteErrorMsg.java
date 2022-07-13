package EasyCache.Messages;

import akka.actor.ActorRef;

import java.util.UUID;

public class CritWriteErrorMsg extends IdMessage{
    public ActorRef originator;
    public CritWriteErrorMsg(int key, ActorRef originator, UUID uuid){
        super(key, uuid);
        this.originator=originator;
    }
}
