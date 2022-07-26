package EasyCache.Messages;

import EasyCache.Config;
import EasyCache.CrashType;

/**
 * This message is used to schedule the next crash in a {@link EasyCache.Devices.Cache cache}
 */
public class CrashMsg extends Message {
    public CrashType type; //type of crash

    public int delay; //delay before recover
    public CrashMsg(CrashType type){
        this.type=type;
        this.delay=Config.RECOVERY_DELAY;
    }

    public CrashMsg(CrashType type, int delay){
        this.type=type;
        this.delay=delay;
    }
}

