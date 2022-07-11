package EasyCache.Messages;


// Sent to a client to trigger the read process.
// This message specify the key of the item that the client need to read.
// This message is used mostly for debug purposes.
public class DoReadMsg extends IdMessage {

    public DoReadMsg(int key) {
        super(key);
    }
}
