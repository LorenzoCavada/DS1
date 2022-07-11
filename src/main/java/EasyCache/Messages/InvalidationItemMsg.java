package EasyCache.Messages;

import java.util.UUID;

public class InvalidationItemMsg extends Message {
    public int key;
    public UUID uuid;

    public InvalidationItemMsg(int key, UUID uuid){
        this.key=key;
        this.uuid=uuid;
    }
}
