package EasyCache.Messages;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CancelTimeoutMsg extends Message{
    public HashSet<UUID> uuids;

    public CancelTimeoutMsg(Set<UUID> uuids){
        this.uuids = new HashSet<>(uuids);
    }
}
