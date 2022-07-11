package EasyCache.Messages;

public class TimeoutInvalidAckMsg extends Message{
    public InvalidationItemMsg awaitedMsg;
    public TimeoutInvalidAckMsg(InvalidationItemMsg awaited){
        this.awaitedMsg=awaited;
    }
}
