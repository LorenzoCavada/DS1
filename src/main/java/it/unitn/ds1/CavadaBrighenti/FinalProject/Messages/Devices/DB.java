package it.unitn.ds1.CavadaBrighenti.FinalProject.Messages.Devices;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import it.unitn.ds1.CavadaBrighenti.FinalProject.Messages.*;

import java.io.Serializable;
import java.util.*;

public class DB extends AbstractActor {
  private Random rnd = new Random();
  private List<ActorRef> children; // the list of children (L2 caches)
  private final int id = -1;         // ID of the current actor

  private HashMap<Integer, Integer> items;

  /* -- Actor constructor --------------------------------------------------- */

  public DB(HashMap<Integer, Integer> items) {

    this.items=new HashMap<>();
    this.items.putAll(items);

  }
  static public Props props(HashMap<Integer, Integer> items) {
    return Props.create(DB.class, () -> new DB(items));
  }

  /* -- Actor behaviour ----------------------------------------------------- */

  private void onReadReqMsg(ReadReqMsg msg){
    ActorRef nextHop = msg.responsePath.pop();
    Integer key = msg.key;
    ReadRespMsg resp = new ReadRespMsg(key, this.items.get(key), msg.responsePath);
    try { Thread.sleep(rnd.nextInt(10)); }
    catch (InterruptedException e) { e.printStackTrace(); }
    nextHop.tell(resp, getSelf());
  }

  private void onWriteReqMsg(WriteReqMsg msg){
    Integer key = msg.key;
    RefillMsg resp = new RefillMsg(key, msg.newValue, msg.originator);
    multicast(resp);
  }

  private void onSetChildrenMsg(SetChildrenMsg msg) {
    this.children = msg.children;
    System.out.println("DB " + this.id + ";setChildren;children = " + msg.children + ";");
  }

  private void onInternalStateMsg(InternalStateMsg msg) {
    StringBuilder sb = new StringBuilder();
    sb.append("DB " + this.id + ";items:[");
    for(Integer k : items.keySet()){
      sb.append(k + ":" + items.get(k) + ";");
    }
    sb.append("]; Children:[");
    for(ActorRef ch : children){
      sb.append(ch.path().name() + ";");
    }
    sb.append("];");
    System.out.println(sb);
  }

  private void multicast(Serializable m) {
    // multicast to all peers in the group (do not send any message to self)
    for (ActorRef p: children) {
      p.tell(m, getSelf());
      // simulate network delays using sleep
      try { Thread.sleep(rnd.nextInt(10)); }
      catch (InterruptedException e) { e.printStackTrace(); }

    }
  }


  // Here we define the mapping between the received message types
  // and our actor methods
  @Override
  public Receive createReceive() {
    return receiveBuilder()
      .match(ReadReqMsg.class,    this::onReadReqMsg)
      .match(WriteReqMsg.class,    this::onWriteReqMsg)
      .match(SetChildrenMsg.class,    this::onSetChildrenMsg)
      .match(InternalStateMsg.class,   this::onInternalStateMsg)
      .build();
  }
}
