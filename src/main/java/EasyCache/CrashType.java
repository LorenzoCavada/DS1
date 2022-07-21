package EasyCache;

// Enum used to identify the type of crashes
public enum CrashType {
    NOW,
    //READ OPERATION
    BEFORE_READ_REQ_FW, //before a read request is forwarded to the parent
    AFTER_READ_REQ_FW, //after a read request is forwarded to the parent
    BEFORE_READ_RESP_FW, //before a read response is forwarded to a child
    BEFORE_READ_RESP, //before a read response (with cached value) is performed
    //WRITE OPERATION
    BEFORE_WRITE_REQ_FW, //before a write request is forwarded to the parent
    AFTER_WRITE_REQ_FW, //after a write request is forwarded to the parent
    BEFORE_REFILL, //before refill is applied in a cache
    BEFORE_WRITE_CONFIRM, //before a write confirm is sent (only L2 caches)
    //CRITICAL READ
    BEFORE_CRIT_READ_REQ_FW, //before a crit read request is forwarded to the parent
    AFTER_CRIT_READ_REQ_FW, //after a crit read request is forwarded to the parent
    BEFORE_CRIT_READ_RESP_FW, //before a crit read response is forwarded to a child
    //CRITICAL WRITE
    BEFORE_CRIT_WRITE_REQ_FW, //before a crit write request is forwarded to parent
    AFTER_CRIT_WRITE_REQ_FW, //after a crit write request is forwarded to parent
    BEFORE_ITEM_INVALIDATION, //before item invalidation request is applied
    BEFORE_ITEM_INVALID_CONFIRM_SEND, //before item invalidation confirm is sent (L2 caches)
    BEFORE_ITEM_INVALID_CONFIRM_RESP, //before item invalidation arrives from L2 caches
    BEFORE_CRIT_REFILL, //before crit refill is applied in a cache
    BEFORE_CRIT_WRITE_CONFIRM, //before a crit write confirm is sent (only L2 caches)
    //OTHERS
    DURING_REFILL_MULTICAST, //crashes during multicast
    DURING_INVALID_ITEM_MULTICAST,
    DURING_CRIT_REFILL_MULTICAST,
    DURING_CRIT_WRITE_ERROR_MULTICAST,

    DURING_CANCEL_TIMEOUT_MULTICAST,
    NONE
}
