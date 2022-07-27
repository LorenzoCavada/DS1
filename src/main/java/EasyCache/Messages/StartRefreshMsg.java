package EasyCache.Messages;

/**
 * This message is sent by a L1 {@link EasyCache.Devices.Cache cache} after recover to tell a L2 {@link EasyCache.Devices.Cache cache}
 * that is still its child to start send {@link RefreshItemReqMsg} for items it has in memory.
 */
public class StartRefreshMsg extends Message{

    public StartRefreshMsg(){

    }
}
