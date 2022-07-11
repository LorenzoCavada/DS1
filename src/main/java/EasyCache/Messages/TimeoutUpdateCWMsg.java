package EasyCache.Messages;

public class TimeoutUpdateCWMsg extends Message{
    public InvalidationItemMsg awaitedMsg;
    public TimeoutUpdateCWMsg(InvalidationItemMsg awaited){
        this.awaitedMsg=awaited;
    }
}
