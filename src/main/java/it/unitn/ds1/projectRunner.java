package it.unitn.ds1;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

public class projectRunner {
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
    DB.JoinGroupMsg joinDB = new DB.JoinGroupMsg(l1List);
    db.tell(joinDB, ActorRef.noSender());

    // associate the parents and children to L1 caches
    int partitionClientSize = N_CLIENT / N_L2;
    int partitionL2Size = N_L2 / N_L1;
    List<List<ActorRef>> partitionsClient = new ArrayList<>();
    List<List<ActorRef>> partitionsL2 = new ArrayList<>();

    for(int i = 0; i < N_CLIENT; i+= partitionClientSize){
      partitionsClient.add(clientList.subList(i, Math.min(i + partitionClientSize, N_CLIENT)));
    }
    for(int i = 0; i < N_L2; i+= partitionL2Size){
      partitionsL2.add(l2List.subList(i, Math.min(i + partitionL2Size, N_L2)));
    }

    for(int indexL1=0;indexL1<N_L1;indexL1++){
      Cache.JoinGroupMsg joinL1=new Cache.JoinGroupMsg(partitionsL2.get(indexL1), db);
      l1List.get(indexL1).tell(joinL1, ActorRef.noSender());
    }
    for(int indexL2=0;indexL2<N_L2;indexL2++){
      Cache.JoinGroupMsg joinL2=new Cache.JoinGroupMsg(partitionsClient.get(indexL2), l1List.get(indexL2/partitionL2Size));
      l2List.get(indexL2).tell(joinL2, ActorRef.noSender());
    }


    try {
      System.out.println(">>> Wait for the chats to stop and press ENTER <<<");
      System.in.read();

      System.out.println(">>> Press ENTER to exit <<<");
      System.in.read();
    } 
    catch (IOException ioe) {}
    system.terminate();
  }
}
