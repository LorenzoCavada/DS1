package EasyCache.Messages;

public class ReqErrorMsg extends Message{
    public IdMessage awaitedMsg;
    public ReqErrorMsg(IdMessage awaited){
        this.awaitedMsg=awaited;
    }
}
