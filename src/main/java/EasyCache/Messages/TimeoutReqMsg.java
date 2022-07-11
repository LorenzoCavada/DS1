package EasyCache.Messages;

public class TimeoutReqMsg extends Message{
    public IdMessage awaitedMsg;
    public TimeoutReqMsg(IdMessage awaited){
        this.awaitedMsg=awaited;
    }
}
