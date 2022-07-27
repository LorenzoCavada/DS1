package EasyCache.Messages;

import java.util.UUID;

/**
 * This message is used during the process of a {@link CritWriteReqMsg critical write}. It is sent by the {@link EasyCache.Devices.DB database}
 * to all {@link EasyCache.Devices.Cache caches} to inform them to mark the item identified by the key as invalid before applying
 * the critical write. {@link EasyCache.Devices.Cache Caches} will not provide the value of invalid items to {@link EasyCache.Devices.Client clients}.
 */
public class InvalidationItemMsg extends IdMessage {

    public InvalidationItemMsg(int key, UUID uuid){
        super(key, uuid);
    }
}
