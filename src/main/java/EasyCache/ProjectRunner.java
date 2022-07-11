package EasyCache;

import EasyCache.Devices.Cache;
import EasyCache.Devices.Client;
import EasyCache.Devices.DB;
import EasyCache.Messages.*;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import EasyCache.Config;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class ProjectRunner {

  private static final Logger LOGGER = LogManager.getLogger(ProjectRunner.class);

  public static void main(String[] args){

    //Logging current run configuration
    Config.printConfig();
    // ---- START OF INITIALIZATION ----

    // create the 'DS1_project' actor system
    final ActorSystem system = ActorSystem.create("DS1_project");

    // creating items list
    HashMap<Integer, Integer> items = new HashMap<Integer, Integer>();
    for (int i = 0; i < Config.N_ITEMS; i++) {
      items.put(i, i);
    }

    // creating the database
    final ActorRef db = system.actorOf(DB.props(items), "db");

    // create the L1 caches
    List<ActorRef> l1List = new ArrayList<ActorRef>();
    for (int i = 0; i < Config.N_L1; i++) {
      int id = i + 100;
      l1List.add(system.actorOf(Cache.props(id, CacheType.L1, db), "L1_" + id));
    }

    // create the L2 caches
    List<ActorRef> l2List = new ArrayList<ActorRef>();
    for (int i = 0; i < Config.N_L2; i++) {
      int id = i + 200;
      l2List.add(system.actorOf(Cache.props(id, CacheType.L2, db), "L2_" + id));
    }

    // create the clients
    List<ActorRef> clientList = new ArrayList<ActorRef>();
    // create the availableL2Msg for the client
    SetAvailableL2Msg availableL2Msg = new SetAvailableL2Msg(l2List);
    for (int i = 0; i < Config.N_CLIENT; i++) {
      int id = i + 300;
      clientList.add(system.actorOf(Client.props(id), "Client_" + id));
      clientList.get(i).tell(availableL2Msg, ActorRef.noSender());
    }

    // associate the L1 caches to the database
    SetChildrenMsg joinDB = new SetChildrenMsg(l1List);
    db.tell(joinDB, ActorRef.noSender());

    //partition the list of all L2 and all Clients in sets of (Config.N_CLIENT/Config.N_L2) and (Config.N_L2/Config.N_L1) elements
    int partitionClientSize = (int) Math.ceil( (double)Config.N_CLIENT / (double) Config.N_L2);
    int partitionL2Size = (int) Math.ceil( (double)Config.N_L2 / (double)Config.N_L1);
    List<List<ActorRef>> partitionsClient = new ArrayList<>();
    List<List<ActorRef>> partitionsL2 = new ArrayList<>();

    for(int i = 0; i < Config.N_CLIENT; i+= partitionClientSize){
      partitionsClient.add(clientList.subList(i, Math.min(i + partitionClientSize, Config.N_CLIENT)));
    }
    for(int i = 0; i < Config.N_L2; i+= partitionL2Size){
      partitionsL2.add(l2List.subList(i, Math.min(i + partitionL2Size, Config.N_L2)));
    }

    //we set parent and children of L1 caches
    //at the same time, we set the L1 cache as parent of the corresponding L2 children
    for(int indexL1=0;indexL1<Config.N_L1;indexL1++){
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
    for(int indexL2=0;indexL2<Config.N_L2;indexL2++){
      SetChildrenMsg childL2=new SetChildrenMsg(partitionsClient.get(indexL2));
      for(int indexClient=0;indexClient<partitionsClient.get(indexL2).size();indexClient++){
        SetParentMsg parentClient=new SetParentMsg(l2List.get(indexL2));
        partitionsClient.get(indexL2).get(indexClient).tell(parentClient, ActorRef.noSender());
      }
      l2List.get(indexL2).tell(childL2, ActorRef.noSender());
    }

    // ---- END OF INITIALIZATION ----
    inputContinue();

    // message for checking the internal state of the caches
    InternalStateMsg internalState = new InternalStateMsg();

    // client 300 critical asks for item 1
    LOGGER.info("Client300 critical asks for item 1");
    clientList.get(0).tell(new DoCritReadMsg(1), ActorRef.noSender());

    inputContinue();

    // let's make cache100 crash
    LOGGER.info("Make Cache100 crash");
    l1List.get(0).tell(new CrashMsg(), ActorRef.noSender());

    //inputContinue();

    // client 304 asks for item 1
    //LOGGER.info("Client304 asks for item 1");
    //clientList.get(4).tell(new DoReadMsg(1), ActorRef.noSender());

    //inputContinue();

    // check the internal state of the cache -> Cache 201 should now be connected to the database
    //LOGGER.info("Check internal state of Cache");
    //l1List.forEach(cacheL1 -> cacheL1.tell(internalState, ActorRef.noSender()));
    //l2List.forEach(cacheL2 -> cacheL2.tell(internalState, ActorRef.noSender()));

    inputContinue();

    // client 304 asks for a write to item 1
    LOGGER.info("Client304 asks for a write to item 1");
    clientList.get(4).tell(new DoWriteMsg(1, 5), ActorRef.noSender());

    //inputContinue();

    // Client 300 asks for item 2 -> this will allow the Cache 200 to detect the crash of Cache 100
    //LOGGER.info("Client 300 asks for item 2 ");
    //clientList.get(0).tell(new DoReadMsg(2), ActorRef.noSender());

    inputContinue();

    // check the internal state of the cache -> Cache 200 should now be connected to the database and have refreshed the memory
    LOGGER.info("Check internal state of Cache");
    l1List.forEach(cacheL1 -> cacheL1.tell(internalState, ActorRef.noSender()));
    l2List.forEach(cacheL2 -> cacheL2.tell(internalState, ActorRef.noSender()));

    inputContinue();

    LOGGER.info("Make Cache100 recover");
    l1List.get(0).tell(new RecoveryMsg(), ActorRef.noSender());

    inputContinue();

    // Client 300 asks for item 1 -> should see the last read from client 302 (item:1 - value:5)
    LOGGER.info("Client 300 asks for item 1 ");
    clientList.get(0).tell(new DoReadMsg(1), ActorRef.noSender());

    system.terminate();
  }

  public static void inputContinue(int delay) {
    try { Thread.sleep(delay); }
    catch (InterruptedException e) { e.printStackTrace(); }
  }

  public static void inputContinue() {
    try { Thread.sleep(1000); }
    catch (InterruptedException e) { e.printStackTrace(); }
  }
}
