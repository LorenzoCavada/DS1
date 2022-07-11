package EasyCache.Messages;

import java.util.UUID;

// Represent the request of asking the children if getself() is parent
public class IsStillParentRespMsg extends Message{
    public boolean response;
    public UUID uuid;
    public IsStillParentRespMsg(boolean resp, UUID uuid){
        this.response=resp;
        this.uuid=uuid;
    }
}
