package EasyCache;

import EasyCache.Devices.Cache;
import EasyCache.Devices.Client;
import EasyCache.Devices.DB;
import EasyCache.Messages.*;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import EasyCache.Config;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import scala.Int;

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
    ArrayList<ActorRef> l1List = new ArrayList<ActorRef>();
    for (int i = 0; i < Config.N_L1; i++) {
      int id = i + 100;
      l1List.add(system.actorOf(Cache.props(id, CacheType.L1, db), "L1_" + id));
    }

    // create the L2 caches
    ArrayList<ActorRef> l2List = new ArrayList<ActorRef>();
    for (int i = 0; i < Config.N_L2; i++) {
      int id = i + 200;
      l2List.add(system.actorOf(Cache.props(id, CacheType.L2, db), "L2_" + id));
    }

    // create the clients
    ArrayList<ActorRef> clientList = new ArrayList<ActorRef>();
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
    int partitionClientSize = (int) Math.floor( (double)Config.N_CLIENT / (double) Config.N_L2);
    int partitionL2Size = (int) Math.floor( (double)Config.N_L2 / (double)Config.N_L1);
    List<CopyOnWriteArrayList<ActorRef>> partitionsClient = new ArrayList<>();
    List<CopyOnWriteArrayList<ActorRef>> partitionsL2 = new ArrayList<>();


    for(int i = 0; i < Config.N_CLIENT;i+=partitionClientSize){
      CopyOnWriteArrayList<ActorRef> theList=new CopyOnWriteArrayList<>();
      theList.addAll(clientList.subList(i, Math.min(i+partitionClientSize, Config.N_CLIENT)));
      partitionsClient.add(theList);
    }

    ListIterator<CopyOnWriteArrayList<ActorRef>> iter1C=partitionsClient.listIterator(Config.N_L2);
    ListIterator<CopyOnWriteArrayList<ActorRef>> iter2C=partitionsClient.listIterator();

    while(iter1C.hasNext()){
      iter2C.next().addAll(iter1C.next());
    }

    for(int i = 0; i < Config.N_L2; i+= partitionL2Size){
      CopyOnWriteArrayList<ActorRef> theList=new CopyOnWriteArrayList<>();
      theList.addAll(l2List.subList(i, Math.min(i + partitionL2Size, Config.N_L2)));
      partitionsL2.add(theList);
    }

    ListIterator<CopyOnWriteArrayList<ActorRef>> iter1_L2=partitionsL2.listIterator(Config.N_L1);
    ListIterator<CopyOnWriteArrayList<ActorRef>> iter2_L2=partitionsL2.listIterator();

    while(iter1_L2.hasNext()){
      iter2_L2.next().addAll(iter1_L2.next());
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
    InternalStateMsg internalState = new InternalStateMsg();
    Random rnd = new Random();

    for(int i = 0; i < 51; i++){
      int client = rnd.nextInt(Config.N_CLIENT);
      int op = rnd.nextInt(4);
      int item = rnd.nextInt(Config.N_ITEMS);

      switch (op){
        case 0: //READ
          LOGGER.info("Performing READ operation on client " + client + " for item " + item);
          randomCrash(4, 1, rnd, l1List, l2List);
          DoReadMsg readMsg = new DoReadMsg(item);
          clientList.get(client).tell(readMsg, ActorRef.noSender());
          break;
        case 1: //WRITE
          LOGGER.info("Performing WRITE operation on client " + client + " for item " + item);
          randomCrash(5, 5, rnd, l1List, l2List);
          DoWriteMsg writeMsg = new DoWriteMsg(item, rnd.nextInt(11));
          clientList.get(client).tell(writeMsg, ActorRef.noSender());
          break;
        case 2: //CRIT READ
          LOGGER.info("Performing CRITICAL READ operation on client " + client + " for item " + item);
          randomCrash(3, 10, rnd, l1List, l2List);
          DoCritReadMsg critRead = new DoCritReadMsg(item);
          clientList.get(client).tell(critRead, ActorRef.noSender());
          break;
        case 3: //CRIT WRITE
          LOGGER.info("Performing CRITICAL WRITE operation on client " + client + " for item " + item);
          randomCrash(10, 13, rnd, l1List, l2List);
          DoCritWriteMsg critWrite = new DoCritWriteMsg(item, rnd.nextInt(11));
          clientList.get(client).tell(critWrite, ActorRef.noSender());
          break;
      }
    }

    inputContinue(60*1000);
    LOGGER.info("PRINT INTERNAL STATE");
    l1List.forEach(l1 -> l1.tell(internalState, ActorRef.noSender()));
    l2List.forEach(l2 -> l2.tell(internalState, ActorRef.noSender()));
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

  private static void randomCrash(int end, int gap, Random rnd, ArrayList<ActorRef> l1List, ArrayList<ActorRef> l2List) {
    if(rnd.nextInt(100) < 25) {
      int crash = rnd.nextInt(end) + gap;
      if(rnd.nextBoolean())
        l1List.get(rnd.nextInt(Config.N_L1)).tell(new CrashMsg(CrashType.values()[crash]), ActorRef.noSender());
      else
        l2List.get(rnd.nextInt(Config.N_L2)).tell(new CrashMsg(CrashType.values()[crash]), ActorRef.noSender());
    }
  }
}
