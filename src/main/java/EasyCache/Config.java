package EasyCache;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;

public class Config implements Serializable {

    private static final Logger LOGGER = LogManager.getLogger(ProjectRunner.class);
    /**
     * number of L1 caches
     */
    final public static int N_L1 = 2;
    /**
     * number of L2 caches
     */
    final public static int N_L2 = N_L1 * 2;
    /**
     * number of clients
     */
    final public static int N_CLIENT = N_L2 * 2;
    /**
     * number of items to be stored in the DB
     */
    final public static int N_ITEMS = 5;
    /**
     * max number of cache that could simultanously crash
     */
    final public static int MAX_N_CACHE_CRASH = 1;

    /**
     * maximum delay for sending messages in milliseconds
     */
    final public static int SEND_MAX_DELAY = 50;

    /**
     * caches recover by default after these milliseconds
     */
    final public static int RECOVERY_DELAY = 1500;

    /**
     * cache goes in timeout after 5 times the maximum delay of the message
     */
    final public static int TIMEOUT_CACHE = SEND_MAX_DELAY * 5;

    /**
     * in case of a critical write operation cache goes in timeout after 10 times the maximum delay of the message
     */
    final public static int TIMEOUT_CACHE_CRIT_WRITE = SEND_MAX_DELAY * 10;

    /**
     * in case of a critical write operation cache goes in timeout after 7 times the maximum delay of the message when waiting for the {@link EasyCache.Messages.CritRefillMsg}
     */
    final public static int TIMEOUT_CACHE_INVALIDATION = SEND_MAX_DELAY * 7;

    /**
     * in case of a critical write operation DB goes in timeout after 6 times the maximum delay of the message when waiting for the {@link EasyCache.Messages.InvalidationItemConfirmMsg}
     */
    final public static int TIMEOUT_DB_INVALIDATION = SEND_MAX_DELAY * 6;

    /**
     * client goes in timeout after 10 times the maximum delay of the message
     */
    final public static int TIMEOUT_CLIENT = SEND_MAX_DELAY * 10;

    /**
     * in case of a critical write operation, client goes in timeout after 12 times the maximum delay of the message
     */
    final public static int TIMEOUT_CLIENT_CRIT_WRITE = SEND_MAX_DELAY * 12; //

    /**
     * if true, prints verbose log messages
     */
    final public static boolean VERBOSE_LOG = false;
    // Method to print the current run configuration
    public static void printConfig(){
        LOGGER.info("N_L1: " + N_L1);
        LOGGER.info("N_L2: " + N_L2);
        LOGGER.info("N_CLIENT: " + N_CLIENT);
        LOGGER.info("N_ITEMS: " + N_ITEMS);
    }
}
