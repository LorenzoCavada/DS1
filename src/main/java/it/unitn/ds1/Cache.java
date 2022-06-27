package it.unitn.ds1;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;

import java.io.Serializable;
import java.util.*;

class Cache extends AbstractActor {

  private Random rnd = new Random();
  private ActorRef parent;
  private List<ActorRef> children; // the list of children
  private final int id;         // ID of the current actor
  private final Messages.typeCache type;

  private HashMap<Integer, Integer> savedItems;

  /* -- Message types ------------------------------------------------------- */

  // Start message that informs every chat participant about its peers
  public static class JoinGroupMsg implements Serializable {
    private final List<ActorRef> children; // list of group members
    private final ActorRef parent;
    public JoinGroupMsg(List<ActorRef> children, ActorRef parent, Messages.typeCache type) {
      this.children = new ArrayList<>(children);
      this.parent=parent;
    }
  }

  /* -- Actor constructor --------------------------------------------------- */

  public Cache(int id, Messages.typeCache type) {
    this.id = id;
    this.type=type;
    this.savedItems=new HashMap<>();
  }

  static public Props props(int id, Messages.typeCache type) {
    return Props.create(Cache.class, () -> new Cache(id, type));
  }

  /* -- Actor behaviour ----------------------------------------------------- */

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
      .match(JoinGroupMsg.class, this::onJoinGroupMsg)
      .match(Messages.ReadReqMsg.class, this::onReadReqMsg)
      .match(Messages.ReadRespMsg.class, this::onReadRespMsg)
      .match(Messages.WriteReqMsg.class, this::onWriteReqMsg)
      .match(Messages.RefillMsg.class, this::onRefillMsg)
      .build();
  }

  private void onJoinGroupMsg(JoinGroupMsg msg) {
    this.children = msg.children;
    this.parent=msg.parent;
    System.out.println("Cache " + this.id + ";joined;parent = " + msg.parent + ";");
  }

  private void onReadReqMsg(Messages.ReadReqMsg msg){
    if(savedItems.containsKey(msg.key)){
      ActorRef nextHop=msg.responsePath.pop();
      Integer key = msg.key;
      Messages.ReadRespMsg resp = new Messages.ReadRespMsg(key, this.savedItems.get(key), msg.responsePath);
      sendMessage(resp, nextHop);
    }else{
      msg.responsePath.push(getSelf());
      sendMessage(msg, parent);
    }
  }

  private void onWriteReqMsg(Messages.WriteReqMsg msg){
    sendMessage(msg, parent);
  }

  private void onReadRespMsg(Messages.ReadRespMsg msg) {
    Integer key = msg.key;
    savedItems.put(key, msg.value);
    ActorRef nextHop = msg.responsePath.pop();
    sendMessage(msg, nextHop);
  }

  private void onRefillMsg(Messages.RefillMsg msg) {
    Integer key = msg.key;
    if(savedItems.containsKey(key)){
      savedItems.put(key, msg.newValue);
    }

    if(this.type == Messages.typeCache.L1){
      multicast(msg);
    }else if(this.type == Messages.typeCache.L2){
      ActorRef originator = msg.originator;
      if(children.contains(originator)) {
        Messages.WriteConfirmMsg resp = new Messages.WriteConfirmMsg(msg.key);
        sendMessage(resp, originator);
      }
    }
  }

  private void sendMessage(Serializable m, ActorRef dest){
    try { Thread.sleep(rnd.nextInt(10)); }
    catch (InterruptedException e) { e.printStackTrace(); }
    dest.tell(m, getSelf());
  }
}
