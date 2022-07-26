package EasyCache.Messages;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * This message is used to cancel all timers for pending request in a {@link EasyCache.Devices.Client client} whose parent
 * (L2 {@link EasyCache.Devices.Cache cache}) has a parent (L1 {@link EasyCache.Devices.Cache cache}) that recovers before the L2
 * {@link EasyCache.Devices.Cache cache} notices the crash. It contains a set of {@link UUID uuid} associated to timers to cancel
 */
public class CancelTimeoutMsg extends Message{
    public HashSet<UUID> uuids;

    public CancelTimeoutMsg(Set<UUID> uuids){
        this.uuids = new HashSet<>(uuids);
    }
}
