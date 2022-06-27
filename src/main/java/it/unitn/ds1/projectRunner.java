package it.unitn.ds1;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.io.IOException;

import it.unitn.ds1.Chatter.JoinGroupMsg;
import it.unitn.ds1.Chatter.StartChatMsg;
import it.unitn.ds1.Chatter.PrintHistoryMsg;

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
    int partitionSize = N_L2 / N_L1;
    int j = 0;
    for(int i = 0; i < N_L2; i+= partitionSize){
      List<ActorRef> children = l2List.subList(i, Math.min(i + partitionSize, N_L2));
      Cache.JoinGroupMsg joinL1 = new Cache.JoinGroupMsg(children, db);
      l1List.get(j).tell(joinL1, ActorRef.noSender());
      j++;
    }


    try {
      System.out.println(">>> Wait for the chats to stop and press ENTER <<<");
      System.in.read();

      // after chats stop, send actors a message to print their logs
      PrintHistoryMsg msg = new PrintHistoryMsg();
      for (ActorRef peer: group) {
        peer.tell(msg, null);
      }
      System.out.println(">>> Press ENTER to exit <<<");
      System.in.read();
    } 
    catch (IOException ioe) {}
    system.terminate();
  }
}
