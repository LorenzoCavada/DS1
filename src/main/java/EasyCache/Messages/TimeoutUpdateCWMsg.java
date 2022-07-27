package EasyCache.Messages;

/**
 * This message is used in L2 {@link EasyCache.Devices.Cache caches} to go in timeout when the parent L1 {@link EasyCache.Devices.Cache cache}
 * crashes between the sending of a {@link InvalidationItemConfirmMsg} and the receival of a {@link CritRefillMsg}. When
 * the timer expires, this message is sent to self to trigger that timeout handling.
 */
public class TimeoutUpdateCWMsg extends Message{
    public InvalidationItemMsg awaitedMsg;
    public TimeoutUpdateCWMsg(InvalidationItemMsg awaited){
        this.awaitedMsg=awaited;
    }
}
