package EasyCache.Messages;

/**
 * This message is sent by the {@link EasyCache.ProjectRunner runner} to a {@link EasyCache.Devices.Client client} to trigger a
 * {@link ReadReqMsg read request}. We need to specifiy the key of the item that the client need to read.
 */
public class DoReadMsg extends IdMessage {

    public DoReadMsg(int key) {
        super(key);
    }
}
