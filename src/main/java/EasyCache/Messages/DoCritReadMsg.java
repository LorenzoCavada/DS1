package EasyCache.Messages;


// Sent to a client to trigger the critical read process.
// This message specify the key of the item that the client need to read directly from the DB.
// This message is used mostly for debug purposes.
public class DoCritReadMsg extends DoReadMsg {

    public DoCritReadMsg(int key) {
        super(key);
    }
}