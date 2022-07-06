package EasyCache.Messages;

import java.io.Serializable;
import java.util.UUID;

abstract public class ReqMessage extends Message {
    public int key;
    public UUID uuid;

    public ReqMessage(int key){
        this.key=key;
        this.uuid=UUID.randomUUID();
    }

    public ReqMessage(int key, UUID uuid){
        this.key=key;
        this.uuid=uuid;
    }
}
