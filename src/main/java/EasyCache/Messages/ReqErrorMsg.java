package EasyCache.Messages;

/**
 * This message is generated in {@link EasyCache.Devices.Cache cache} to tell a {@link EasyCache.Devices.Client client}
 * its request has gone wrong. It is used when a {@link EasyCache.Devices.Client client} request an operation on an invalid
 * item or when a L2 {@link EasyCache.Devices.Cache cache} goes in timeout while receiving a response from a L1 {@link EasyCache.Devices.Cache cache}.
 */
public class ReqErrorMsg extends Message{
    public IdMessage awaitedMsg;
    public ReqErrorMsg(IdMessage awaited){
        this.awaitedMsg=awaited;
    }
}
