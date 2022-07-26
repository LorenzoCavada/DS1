package EasyCache;

/**
 * Enum used to identify the type of crashes
 */
public enum CrashType {
    /**
     * Crash immediately
     */
    NOW,
    //READ OPERATION
    /**
     * before a {@link EasyCache.Messages.ReadReqMsg read request} is forwarded to the parent
     */
    BEFORE_READ_REQ_FW,
    /**
     * after a {@link EasyCache.Messages.ReadReqMsg read request} is forwarded to the parent
     */
    AFTER_READ_REQ_FW,
    /**
     * before a {@link EasyCache.Messages.ReadRespMsg read response} is forwarded to a child
     */
    BEFORE_READ_RESP_FW,
    /**
     * before a {@link EasyCache.Messages.ReadRespMsg read response} (with cached value) is performed
     */
    BEFORE_READ_RESP,
    //WRITE OPERATION
    /**
     * before a {@link EasyCache.Messages.WriteReqMsg write request} is forwarded to the parent
     */
    BEFORE_WRITE_REQ_FW,
    /**
     * after a {@link EasyCache.Messages.WriteReqMsg write request} is forwarded to the parent
     */
    AFTER_WRITE_REQ_FW,
    /**
     * before {@link EasyCache.Messages.RefillMsg refill} is applied in a {@link EasyCache.Devices.Cache}
     */
    BEFORE_REFILL,
    /**
     * before a {@link EasyCache.Messages.WriteConfirmMsg write confirm} is sent
     */
    BEFORE_WRITE_CONFIRM,
    /**
     * during the multicast of a {@link EasyCache.Messages.RefillMsg refill}
     */
    DURING_REFILL_MULTICAST,

    //CRITICAL READ
    /**
     * before a {@link EasyCache.Messages.CritReadReqMsg crit read request} is forwarded to the parent
     */
    BEFORE_CRIT_READ_REQ_FW,
    /**
     * after a {@link EasyCache.Messages.CritReadReqMsg crit read request} is forwarded to the parent
     */
    AFTER_CRIT_READ_REQ_FW,
    /**
     * before a {@link EasyCache.Messages.CritReadRespMsg crit read response} is forwarded to a child
     */
    BEFORE_CRIT_READ_RESP_FW,

    //CRITICAL WRITE
    /**
     * before a {@link EasyCache.Messages.CritWriteReqMsg crit write request} is forwarded to parent
     */
    BEFORE_CRIT_WRITE_REQ_FW,
    /**
     * after a {@link EasyCache.Messages.CritWriteReqMsg crit write request} is forwarded to parent
     */
    AFTER_CRIT_WRITE_REQ_FW,
    /**
     * before a {@link EasyCache.Messages.InvalidationItemMsg item invalidation request} is a {@link EasyCache.Devices.Cache}
     */
    BEFORE_ITEM_INVALIDATION,
    /**
     * before a {@link EasyCache.Messages.InvalidationItemConfirmMsg item invalidation confirmation} is sent by a {@link EasyCache.Devices.Cache}
     */
    BEFORE_ITEM_INVALID_CONFIRM_SEND,
    /**
     * before a {@link EasyCache.Messages.InvalidationItemConfirmMsg item invalidation confirmation} arrives from a L2 {@link EasyCache.Devices.Cache}
     */
    BEFORE_ITEM_INVALID_CONFIRM_RESP,
    /**
     * before {@link EasyCache.Messages.CritRefillMsg crit refill} is applied in a {@link EasyCache.Devices.Cache}
     */
    BEFORE_CRIT_REFILL, //before crit refill is applied in a cache
    /**
     * before a {@link EasyCache.Messages.CritWriteConfirmMsg crit write confirm} is sent
     */
    BEFORE_CRIT_WRITE_CONFIRM,
    //OTHERS
    /**
     * during the multicast of a {@link EasyCache.Messages.InvalidationItemMsg invalidation item message}
     */
    DURING_INVALID_ITEM_MULTICAST,
    /**
     * during the multicast of a {@link EasyCache.Messages.CritRefillMsg crit refill}
     */
    DURING_CRIT_REFILL_MULTICAST,
    /**
     * during the multicast of a {@link EasyCache.Messages.CritWriteErrorMsg crit write error}
     */
    DURING_CRIT_WRITE_ERROR_MULTICAST,
    /**
     * during the multicast of a {@link EasyCache.Messages.CancelTimeoutMsg cancel timeout message}
     */
    DURING_CANCEL_TIMEOUT_MULTICAST,
    /**
     * during the refresh of items of a L2 {@link EasyCache.Devices.Cache}
     */
    DURING_REFRESH, //before a read request is forwarded to the parent
    /**
     * No crash
     */
    NONE
}
