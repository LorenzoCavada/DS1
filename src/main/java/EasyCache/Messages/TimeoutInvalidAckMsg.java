package EasyCache.Messages;

/**
 * This message is used during critical writes in {@link EasyCache.Devices.DB database} to go in timeout when {@link InvalidationItemConfirmMsg} are not
 * received from all children. Thus we can assume one child is crashed and then a {@link CritWriteErrorMsg} is generated and sent to
 * the originator of the {@link CritWriteReqMsg critical write request}.
 */
public class TimeoutInvalidAckMsg extends Message{
    public InvalidationItemMsg awaitedMsg;
    public TimeoutInvalidAckMsg(InvalidationItemMsg awaited){
        this.awaitedMsg=awaited;
    }
}
