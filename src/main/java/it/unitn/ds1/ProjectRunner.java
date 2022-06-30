package it.unitn.ds1;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

public class ProjectRunner {
  final private static int N_L1 = 2; // number of L1 caches
  final private static int N_L2 = N_L1 * 2; // number of L2 caches
  final private static int N_CLIENT = N_L2 * 2; // number of clients
  final private static int N_ITEMS = 20; // number of items to be stored in the DB

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
      l1List.add(system.actorOf(Cache.props(id, Messages.typeCache.L1), "L1_" + id));
    }

    // create the L2 caches
    List<ActorRef> l2List = new ArrayList<ActorRef>();
    for (int i = 0; i < N_L2; i++) {
      int id = i + 200;
      l2List.add(system.actorOf(Cache.props(id, Messages.typeCache.L2), "L2_" + id));
    }

    // create the clients
    List<ActorRef> clientList = new ArrayList<ActorRef>();
    for (int i = 0; i < N_CLIENT; i++) {
      int id = i + 300;
      clientList.add(system.actorOf(Client.props(id), "Client_" + id));
    }

    // associate the L1 caches to the database
    Messages.SetChildrenMsg joinDB = new Messages.SetChildrenMsg(l1List);
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
      Messages.SetChildrenMsg childL1=new Messages.SetChildrenMsg(partitionsL2.get(indexL1));
      for(int indexL2=0;indexL2<partitionsL2.get(indexL1).size();indexL2++){
        Messages.SetParentMsg parentL2=new Messages.SetParentMsg(l1List.get(indexL1));
        partitionsL2.get(indexL1).get(indexL2).tell(parentL2, ActorRef.noSender());
      }
      Messages.SetParentMsg parentL1=new Messages.SetParentMsg(db);
      l1List.get(indexL1).tell(childL1, ActorRef.noSender());
      l1List.get(indexL1).tell(parentL1, ActorRef.noSender());
    }

    //finally we set clients as children of L2 caches
    //at the same time, we set the L2 cache as parent of the corresponding client children
    for(int indexL2=0;indexL2<N_L2;indexL2++){
      Messages.SetChildrenMsg childL2=new Messages.SetChildrenMsg(partitionsClient.get(indexL2));
      for(int indexClient=0;indexClient<partitionsClient.get(indexL2).size();indexClient++){
        Messages.SetParentMsg parentClient=new Messages.SetParentMsg(l2List.get(indexL2));
        partitionsClient.get(indexL2).get(indexClient).tell(parentClient, ActorRef.noSender());
      }
      l2List.get(indexL2).tell(childL2, ActorRef.noSender());
    }

    try { Thread.sleep(100); }
    catch (InterruptedException e) { e.printStackTrace(); }
    System.out.println("\n\nPrinting the stating internal state of the node");

    // check initial internal state of each node
    Messages.InternalStateMsg internalState = new Messages.InternalStateMsg();
    db.tell(internalState, ActorRef.noSender());
    l1List.forEach(cacheL1 -> cacheL1.tell(internalState, ActorRef.noSender()));
    l2List.forEach(cacheL2 -> cacheL2.tell(internalState, ActorRef.noSender()));

    try { Thread.sleep(100); }
    catch (InterruptedException e) { e.printStackTrace(); }
    System.out.println("\n\nStarting read operation");

    // Client 300 asks for item 1
    clientList.get(0).tell(new Messages.DoReadMsg(1), ActorRef.noSender());

    // Client 301 asks for item 1
    clientList.get(1).tell(new Messages.DoReadMsg(1), ActorRef.noSender());

    // Client 302 asks for item 1
    clientList.get(2).tell(new Messages.DoReadMsg(1), ActorRef.noSender());

    try { Thread.sleep(100); }
    catch (InterruptedException e) { e.printStackTrace(); }
    System.out.println("\n\nPrinting internal state after read operations");


    // printing the internal state
    l1List.forEach(cacheL1 -> cacheL1.tell(internalState, ActorRef.noSender()));
    l2List.forEach(cacheL2 -> cacheL2.tell(internalState, ActorRef.noSender()));

    try { Thread.sleep(100); }
    catch (InterruptedException e) { e.printStackTrace(); }
    System.out.println("\n\nStarting the write operation");

    // Client 303 asks for write 2 in item with key 1
    clientList.get(3).tell(new Messages.DoWriteMsg(1, 2), ActorRef.noSender());

    try { Thread.sleep(100); }
    catch (InterruptedException e) { e.printStackTrace(); }
    System.out.println("\n\nStarting internal state after the write operation");

    // printing the internal state
    l1List.forEach(cacheL1 -> cacheL1.tell(internalState, ActorRef.noSender()));
    l2List.forEach(cacheL2 -> cacheL2.tell(internalState, ActorRef.noSender()));

    try { Thread.sleep(100); }
    catch (InterruptedException e) { e.printStackTrace(); }
    System.out.println("\n\nPerform last read");

    // Client 300 asks for item 1
    clientList.get(0).tell(new Messages.DoReadMsg(1), ActorRef.noSender());

    try { Thread.sleep(100); }
    catch (InterruptedException e) { e.printStackTrace(); }
    System.out.println("\n\nEnding the program");

    /*
    try {

      System.out.println(">>> Press ENTER to exit <<<");
      System.in.read();
    } 
    catch (IOException ioe) {}*/
    system.terminate();
  }
}
