package EasyCache.Messages;

import java.util.UUID;

abstract public class IdMessage extends Message {
    public int key;
    public UUID uuid;

    public IdMessage(int key){
        this.key=key;
        this.uuid=UUID.randomUUID();
    }

    public IdMessage(int key, UUID uuid){
        this.key=key;
        this.uuid=uuid;
    }
}
