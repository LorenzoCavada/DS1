package EasyCache.Messages;

import java.util.UUID;

// Represent the request of asking the children if getself() is parent
public class IsStillParentReqMsg extends Message{

    public UUID uuid;
    public IsStillParentReqMsg(){
        this.uuid=UUID.randomUUID();
    }
}
