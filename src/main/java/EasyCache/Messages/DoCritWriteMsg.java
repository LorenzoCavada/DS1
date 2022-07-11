package EasyCache.Messages;

// Sent to a client to trigger the write process.
// This message specify the key of the item that the client need to write and the newValue to set to the item.
// This message is used mostly for debug purposes.
public class DoCritWriteMsg extends DoWriteMsg {

    public DoCritWriteMsg(int key, int newValue) {
        super(key, newValue);
    }
}
