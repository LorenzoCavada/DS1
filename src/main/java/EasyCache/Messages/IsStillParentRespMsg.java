package EasyCache.Messages;

// Represent the request of asking the children if getself() is parent
public class IsStillParentRespMsg extends Message{
    public boolean response;
    public IsStillParentRespMsg(boolean resp){
        this.response=resp;
    }
}
