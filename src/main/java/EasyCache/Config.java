package EasyCache;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;

public class Config implements Serializable {

    private static final Logger LOGGER = LogManager.getLogger(ProjectRunner.class);

    final public static int N_L1 = 2; // number of L1 caches
    final public static int N_L2 = N_L1 * 2; // number of L2 caches
    final public static int N_CLIENT = N_L2 * 2; // number of clients
    final public static int N_ITEMS = 20; // number of items to be stored in the DB

    final public static int RECOVERY_DELAY = 1500; //caches recover after these milliseconds

    final public static int TIMEOUT_CACHE = 500; //cache goes in timeout after 500 ms

    final public static int TIMEOUT_CACHE_INVALIDATION = 400; //

    final public static int TIMEOUT_DB_INVALIDATION = 300; //

    final public static int TIMEOUT_CLIENT = 1000; //client goes in timeout after 1000 ms

    final public static boolean VERBOSE_LOG = false; //if true, prints verbose log messages
    // Method to print the current run configuration
    public static void printConfig(){
        LOGGER.info("N_L1: " + N_L1);
        LOGGER.info("N_L2: " + N_L2);
        LOGGER.info("N_CLIENT: " + N_CLIENT);
        LOGGER.info("N_ITEMS: " + N_ITEMS);
        LOGGER.info("RECOVER_DELAY: " + RECOVERY_DELAY);
        LOGGER.info("TIMEOUT_CACHE: " + TIMEOUT_CACHE);
        LOGGER.info("TIMEOUT_CLIENT: " + TIMEOUT_CLIENT);
        LOGGER.info("VERBOSE_LOG: " + VERBOSE_LOG);
    }
}
