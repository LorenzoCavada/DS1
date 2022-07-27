package EasyCache.Messages;

import java.util.UUID;

/**
 * This message is used by a {@link EasyCache.Devices.Cache cache} or a {@link EasyCache.Devices.Client client} to answer to
 * a {@link IsStillParentReqMsg}. We check if the sender of the {@link IsStillParentReqMsg} is still the parent of the receiver.
 */
public class IsStillParentRespMsg extends Message{
    public boolean response;
    public UUID uuid;
    public IsStillParentRespMsg(boolean resp, UUID uuid){
        this.response=resp;
        this.uuid=uuid;
    }
}
