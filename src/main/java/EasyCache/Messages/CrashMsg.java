package EasyCache.Messages;

import EasyCache.Config;
import EasyCache.CrashType;

public class CrashMsg extends Message {
    public CrashType type; //type of crash

    public int delay;
    public CrashMsg(CrashType type){
        this.type=type;
        this.delay=Config.RECOVERY_DELAY;
    }

    public CrashMsg(CrashType type, int delay){
        this.type=type;
        this.delay=delay;
    }
}

