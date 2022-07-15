package EasyCache.Messages;

import EasyCache.CrashType;

public class CrashMsg extends Message {
    public CrashType type; //type of crash
    public CrashMsg(CrashType type){
        this.type=type;
    }
}

