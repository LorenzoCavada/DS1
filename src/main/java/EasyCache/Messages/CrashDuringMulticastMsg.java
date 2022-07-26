package EasyCache.Messages;

import EasyCache.CrashType;

/**
 * This message is used to schedule the next crash during a multicast in a {@link EasyCache.Devices.Cache cache}
 */
public class CrashDuringMulticastMsg extends CrashMsg {
    public int afterNMessage;
    public CrashDuringMulticastMsg(CrashType type, int afterNMessage){
        super(type);
        this.afterNMessage=afterNMessage;
    }

    public CrashDuringMulticastMsg(CrashType type, int afterNMessage, int delay){
        super(type, delay);
        this.afterNMessage=afterNMessage;
    }
}

