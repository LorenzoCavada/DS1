package EasyCache.Messages;

public class ReqErrorMsg extends Message{
    public ReqMessage awaitedMsg;
    public ReqErrorMsg(ReqMessage awaited){
        this.awaitedMsg=awaited;
    }
}
