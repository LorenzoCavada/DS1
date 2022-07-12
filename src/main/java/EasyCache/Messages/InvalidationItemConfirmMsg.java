package EasyCache.Messages;

import java.util.UUID;

public class InvalidationItemConfirmMsg extends IdMessage {

    public InvalidationItemConfirmMsg(int key, UUID uuid){
        super(key,uuid);
    }
}
