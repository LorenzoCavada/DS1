package EasyCache.Messages;

import java.util.UUID;

/**
 * This message is used by a {@link EasyCache.Devices.Cache cache} to update the list of its children after it has recovered from a crash.
 * At recover, this message is sent to all the children, that will respond with a {@link IsStillParentRespMsg}.
 */
public class IsStillParentReqMsg extends Message{

    public UUID uuid;
    public IsStillParentReqMsg(){
        this.uuid=UUID.randomUUID();
    }
}
