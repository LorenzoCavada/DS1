package EasyCache.Messages;


/**
 * This message is sent by the {@link EasyCache.ProjectRunner runner} to a {@link EasyCache.Devices.Client client} to trigger a
 * {@link CritReadReqMsg critical read request}. It is used like in {@link DoReadMsg}.
 */
public class DoCritReadMsg extends DoReadMsg {

    public DoCritReadMsg(int key) {
        super(key);
    }
}