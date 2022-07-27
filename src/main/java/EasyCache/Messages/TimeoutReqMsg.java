package EasyCache.Messages;

/**
 * This message is used in L2 {@link EasyCache.Devices.Cache caches} to go in timeout when the parent L1 {@link EasyCache.Devices.Cache cache}
 * crashes or in {@link EasyCache.Devices.Client clients} to go in timeout when the parent L2 {@link EasyCache.Devices.Cache cache} crashes.
 * When the timer for the response of a request expires, this message is sent to self to trigger the timeout handling.
 * The timeout message include the message for which we are waiting a response.
 */
public class TimeoutReqMsg extends Message{
    public IdMessage awaitedMsg;
    public TimeoutReqMsg(IdMessage awaited){
        this.awaitedMsg=awaited;
    }
}
