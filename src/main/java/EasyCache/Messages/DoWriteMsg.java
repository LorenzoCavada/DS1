package EasyCache.Messages;

/**
 * This message is sent by the {@link EasyCache.ProjectRunner runner} to a {@link EasyCache.Devices.Client client} to trigger a
 * {@link WriteReqMsg write request}. We need to specifiy the key of the item that the client need to write and the new value.
 */
public class DoWriteMsg extends IdMessage {
    public final int newValue; // newValue to set

    public DoWriteMsg(int key, int newValue) {
        super(key);
        this.newValue=newValue;
    }
}
