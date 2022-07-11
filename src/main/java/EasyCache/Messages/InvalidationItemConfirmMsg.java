package EasyCache.Messages;

import java.util.UUID;

public class InvalidationItemConfirmMsg extends Message {
    public int key;
    public UUID uuid;

    public InvalidationItemConfirmMsg(int key, UUID uuid){
        this.key=key;
        this.uuid=uuid;
    }
}
