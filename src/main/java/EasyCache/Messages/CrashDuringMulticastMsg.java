package EasyCache.Messages;

import EasyCache.CrashType;

public class CrashDuringMulticastMsg extends CrashMsg {
    public int afterNMessage;
    public CrashDuringMulticastMsg(int afterNMessage){
        super(CrashType.DURING_MULTICAST);
        this.afterNMessage=afterNMessage;
    }

    public CrashDuringMulticastMsg(int afterNMessage, int delay){
        super(CrashType.DURING_MULTICAST, delay);
        this.afterNMessage=afterNMessage;
    }
}

