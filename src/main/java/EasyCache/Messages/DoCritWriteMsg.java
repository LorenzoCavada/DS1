package EasyCache.Messages;

/**
 * This message is sent by the {@link EasyCache.ProjectRunner runner} to a {@link EasyCache.Devices.Client client} to trigger a
 * {@link CritWriteReqMsg critical write request}. It is used like in {@link DoWriteMsg}.
 */
public class DoCritWriteMsg extends DoWriteMsg {

    public DoCritWriteMsg(int key, int newValue) {
        super(key, newValue);
    }
}
