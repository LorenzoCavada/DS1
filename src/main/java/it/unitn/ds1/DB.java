package it.unitn.ds1;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;

import java.io.Serializable;
import java.util.*;

class DB extends AbstractActor {
  private Random rnd = new Random();
  private List<ActorRef> children; // the list of children (L2 caches)
  private final int id = -1;         // ID of the current actor

  private HashMap<Integer, Integer> items;

  /* -- Message types ------------------------------------------------------- */

  public static class JoinGroupMsg implements Serializable {
    private final List<ActorRef> children; // list of children members
    public JoinGroupMsg(List<ActorRef> children) {
      this.children=new ArrayList<>(children);
    }
  }

  /* -- Actor constructor --------------------------------------------------- */

  public DB(HashMap<Integer, Integer> items) {

    this.items=new HashMap<>();
    this.items.putAll(items);

  }
  static public Props props(HashMap<Integer, Integer> items) {
    return Props.create(DB.class, () -> new DB(items));
  }

  /* -- Actor behaviour ----------------------------------------------------- */

  private void onReadReqMsg(Messages.ReadReqMsg msg){
    ActorRef nextHop = msg.responsePath.pop();
    Integer key = msg.key;
    Messages.ReadRespMsg resp = new Messages.ReadRespMsg(key, this.items.get(key), msg.responsePath);
    try { Thread.sleep(rnd.nextInt(10)); }
    catch (InterruptedException e) { e.printStackTrace(); }
    nextHop.tell(resp, getSelf());
  }

  private void onWriteReqMsg(Messages.WriteReqMsg msg){
    Integer key = msg.key;
    Messages.RefillMsg resp = new Messages.RefillMsg(key, msg.newValue, msg.originator);
    multicast(resp);
  }

  private void onJoinGroupMsg(JoinGroupMsg msg) {
    this.children = msg.children;
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
      .match(Messages.ReadReqMsg.class,    this::onReadReqMsg)
      .match(Messages.WriteReqMsg.class,    this::onWriteReqMsg)
      .match(JoinGroupMsg.class,    this::onJoinGroupMsg)
      .build();
  }
}
