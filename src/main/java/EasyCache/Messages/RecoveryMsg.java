package EasyCache.Messages;

/**
 * This message is scheduled by a {@link EasyCache.Devices.Cache cache} to itself to trigger its recovery.
 * It could be also sent by the {@link EasyCache.ProjectRunner runner} to manually recover a cache.
 */

public class RecoveryMsg extends Message{
    public RecoveryMsg(){}
}
