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


    // Method to print the current run configuration
    public static void printConfig(){
        LOGGER.info("N_L1: " + N_L1);
        LOGGER.info("N_L2: " + N_L2);
        LOGGER.info("N_CLIENT: " + N_CLIENT);
        LOGGER.info("N_ITEMS: " + N_ITEMS);
    }
}
