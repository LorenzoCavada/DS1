package EasyCache.Messages;

import java.util.UUID;

/**
 * This message represents the confirmation of a successful {@link CritWriteReqMsg critical write request}.
 * The message is used like in {@link WriteConfirmMsg}.
 */
public class CritWriteConfirmMsg extends WriteConfirmMsg{
    public CritWriteConfirmMsg(int key, UUID uuid) {
        super(key,uuid);
    }
}
