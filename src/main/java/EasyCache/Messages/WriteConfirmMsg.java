package EasyCache.Messages;

import java.util.UUID;

/**
 * This message represents the confirmation of a successful {@link WriteReqMsg write request}.
 * This will be originated by a L2 {@link EasyCache.Devices.Cache cache} if the originator of the {@link WriteReqMsg request} is
 * a child of the L2 {@link EasyCache.Devices.Cache cache}. It will be sent to that child.
 * Thus, this message is originated when that L2 {@link EasyCache.Devices.Cache cache} receives a {@link RefillMsg refill}.
 */
public class WriteConfirmMsg extends IdMessage{

    public WriteConfirmMsg(int key, UUID uuid) {
        super(key, uuid);
    }
}
