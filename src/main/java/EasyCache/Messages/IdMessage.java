package EasyCache.Messages;

import java.util.UUID;

/**
 * Abstract class that identifies all messages with an {@link UUID uuid} and a key. Every message that needs an uuid subclass
 * from this class.
 * Thanks to the {@link UUID uuid}, we can chain and associate requests and connected responses.
 */
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
