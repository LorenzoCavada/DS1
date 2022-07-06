package EasyCache.Messages;

public class TimeoutMsg extends Message{
    public ReqMessage awaitedMsg;
    public TimeoutMsg(ReqMessage awaited){
        this.awaitedMsg=awaited;
    }
}
