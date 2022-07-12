package EasyCache.Messages;

import java.util.UUID;

public class InvalidationItemMsg extends IdMessage {

    public InvalidationItemMsg(int key, UUID uuid){
        super(key, uuid);
    }
}
