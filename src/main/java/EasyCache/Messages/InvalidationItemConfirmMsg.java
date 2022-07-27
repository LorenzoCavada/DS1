package EasyCache.Messages;

import java.util.UUID;

/**
 * This message is used during the process of a {@link CritWriteReqMsg critical write}. It is sent by the {@link EasyCache.Devices.Cache caches}
 * to acknowledge the receival of a {@link InvalidationItemMsg}.
 */
public class InvalidationItemConfirmMsg extends IdMessage {

    public InvalidationItemConfirmMsg(int key, UUID uuid){
        super(key,uuid);
    }
}
