package EasyCache;

import EasyCache.Devices.Cache;
import EasyCache.Devices.Client;
import EasyCache.Devices.DB;
import EasyCache.Messages.*;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import EasyCache.Messages.*;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class ProjectRunner {
  final private static int N_L1 = 2; // number of L1 caches
  final private static int N_L2 = N_L1 * 2; // number of L2 caches
  final private static int N_CLIENT = N_L2 * 2; // number of clients
  final private static int N_ITEMS = 20; // number of items to be stored in the DB
  private static final Logger LOGGER = LogManager.getLogger(ProjectRunner.class);

  public static void main(String[] args) {

    // create the 'DS1_project' actor system
    final ActorSystem system = ActorSystem.create("DS1_project");

    // creating items list
    HashMap<Integer, Integer> items = new HashMap<Integer, Integer>();
    for (int i = 0; i < N_ITEMS; i++) {
      items.put(i, i);
    }

    // creating the database
    final ActorRef db = system.actorOf(DB.props(items), "db");

    // create the L1 caches
    List<ActorRef> l1List = new ArrayList<ActorRef>();
    for (int i = 0; i < N_L1; i++) {
      int id = i + 100;
      l1List.add(system.actorOf(Cache.props(id, CacheType.L1), "L1_" + id));
    }

    // create the L2 caches
    List<ActorRef> l2List = new ArrayList<ActorRef>();
    for (int i = 0; i < N_L2; i++) {
      int id = i + 200;
      l2List.add(system.actorOf(Cache.props(id, CacheType.L2), "L2_" + id));
    }

    // create the clients
    List<ActorRef> clientList = new ArrayList<ActorRef>();
    for (int i = 0; i < N_CLIENT; i++) {
      int id = i + 300;
      clientList.add(system.actorOf(Client.props(id), "Client_" + id));
    }

    // associate the L1 caches to the database
    SetChildrenMsg joinDB = new SetChildrenMsg(l1List);
    db.tell(joinDB, ActorRef.noSender());

    //partition the list of all L2 and all Clients in sets of (N_CLIENT/N_L2) and (N_L2/N_L1) elements
    int partitionClientSize = (int) Math.ceil( (double)N_CLIENT / (double) N_L2);
    int partitionL2Size = (int) Math.ceil( (double)N_L2 / (double)N_L1);
    List<List<ActorRef>> partitionsClient = new ArrayList<>();
    List<List<ActorRef>> partitionsL2 = new ArrayList<>();

    for(int i = 0; i < N_CLIENT; i+= partitionClientSize){
      partitionsClient.add(clientList.subList(i, Math.min(i + partitionClientSize, N_CLIENT)));
    }
    for(int i = 0; i < N_L2; i+= partitionL2Size){
      partitionsL2.add(l2List.subList(i, Math.min(i + partitionL2Size, N_L2)));
    }

    //we set parent and children of L1 caches
    //at the same time, we set the L1 cache as parent of the corresponding L2 children
    for(int indexL1=0;indexL1<N_L1;indexL1++){
      SetChildrenMsg childL1=new SetChildrenMsg(partitionsL2.get(indexL1));
      for(int indexL2=0;indexL2<partitionsL2.get(indexL1).size();indexL2++){
        SetParentMsg parentL2=new SetParentMsg(l1List.get(indexL1));
        partitionsL2.get(indexL1).get(indexL2).tell(parentL2, ActorRef.noSender());
      }
      SetParentMsg parentL1=new SetParentMsg(db);
      l1List.get(indexL1).tell(childL1, ActorRef.noSender());
      l1List.get(indexL1).tell(parentL1, ActorRef.noSender());
    }

    //finally we set clients as children of L2 caches
    //at the same time, we set the L2 cache as parent of the corresponding client children
    for(int indexL2=0;indexL2<N_L2;indexL2++){
      SetChildrenMsg childL2=new SetChildrenMsg(partitionsClient.get(indexL2));
      for(int indexClient=0;indexClient<partitionsClient.get(indexL2).size();indexClient++){
        SetParentMsg parentClient=new SetParentMsg(l2List.get(indexL2));
        partitionsClient.get(indexL2).get(indexClient).tell(parentClient, ActorRef.noSender());
      }
      l2List.get(indexL2).tell(childL2, ActorRef.noSender());
    }

    try { Thread.sleep(100); }
    catch (InterruptedException e) { e.printStackTrace(); }
    LOGGER.info("Printing the stating internal state of the node");

    // check initial internal state of each node
    InternalStateMsg internalState = new InternalStateMsg();
    db.tell(internalState, ActorRef.noSender());
    l1List.forEach(cacheL1 -> cacheL1.tell(internalState, ActorRef.noSender()));
    l2List.forEach(cacheL2 -> cacheL2.tell(internalState, ActorRef.noSender()));

    try { Thread.sleep(100); }
    catch (InterruptedException e) { e.printStackTrace(); }
    LOGGER.info("Starting read operation");

    // Client 300 asks for item 1
    clientList.get(0).tell(new DoReadMsg(1), ActorRef.noSender());

    // Client 301 asks for item 1
    clientList.get(1).tell(new DoReadMsg(1), ActorRef.noSender());

    // Client 302 asks for item 1
    clientList.get(2).tell(new DoReadMsg(1), ActorRef.noSender());

    try { Thread.sleep(100); }
    catch (InterruptedException e) { e.printStackTrace(); }
    LOGGER.info("Printing internal state after read operations");


    // printing the internal state
    l1List.forEach(cacheL1 -> cacheL1.tell(internalState, ActorRef.noSender()));
    l2List.forEach(cacheL2 -> cacheL2.tell(internalState, ActorRef.noSender()));

    try { Thread.sleep(100); }
    catch (InterruptedException e) { e.printStackTrace(); }
    LOGGER.info("Starting the write operation");

    // Client 303 asks for write 2 in item with key 1
    clientList.get(3).tell(new DoWriteMsg(1, 2), ActorRef.noSender());

    try { Thread.sleep(100); }
    catch (InterruptedException e) { e.printStackTrace(); }
    LOGGER.info("Starting internal state after the write operation");

    // printing the internal state
    l1List.forEach(cacheL1 -> cacheL1.tell(internalState, ActorRef.noSender()));
    l2List.forEach(cacheL2 -> cacheL2.tell(internalState, ActorRef.noSender()));

    try { Thread.sleep(100); }
    catch (InterruptedException e) { e.printStackTrace(); }
    LOGGER.info("Perform last read");

    // Client 300 asks for item 1
    clientList.get(0).tell(new DoReadMsg(1), ActorRef.noSender());

    try { Thread.sleep(100); }
    catch (InterruptedException e) { e.printStackTrace(); }
    LOGGER.info("Ending the program");

    /*
    try {

      LOGGER.info(">>> Press ENTER to exit <<<");
      System.in.read();
    } 
    catch (IOException ioe) {}*/
    system.terminate();
  }
}
