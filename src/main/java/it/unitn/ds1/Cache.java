package it.unitn.ds1;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;

import java.io.Serializable;
import java.util.*;

class Cache extends AbstractActor {

  private Random rnd = new Random(); // Used to generate random value
  private ActorRef parent; // reference to the parent, may be a L1 Cache or the DB
  private List<ActorRef> children; // the list of children, they can be or a list of L2 caches or a list of Clients
  private final int id; // ID of the current actor
  private final Messages.typeCache type; // type of the current cache, may be L1 or L2

  private HashMap<Integer, Integer> savedItems; // the items saved in the cache

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

    // multicast to all the children of the cache (do not send any message to self)
    for (ActorRef p: children) {
      p.tell(m, getSelf());
      // simulate network delays using sleep
      try { Thread.sleep(rnd.nextInt(10)); }
      catch (InterruptedException e) { e.printStackTrace(); }

    }
  }

  // Here we define the mapping between the received message types and our actor methods
  @Override
  public Receive createReceive() {
    return receiveBuilder()
      .match(Messages.SetChildrenMsg.class, this::onSetChildrenMsg)
      .match(Messages.SetParentMsg.class, this::onSetParentMsg)
      .match(Messages.ReadReqMsg.class, this::onReadReqMsg)
      .match(Messages.ReadRespMsg.class, this::onReadRespMsg)
      .match(Messages.WriteReqMsg.class, this::onWriteReqMsg)
      .match(Messages.RefillMsg.class, this::onRefillMsg)
      .build();
  }

  // This message is used to set the children of the cache
  private void onSetChildrenMsg(Messages.SetChildrenMsg msg) {
    this.children = msg.children;
    System.out.println("Cache " + this.id + ";setChildren;children = " + msg.children + ";");
  }

  // This message is used to set the parent of the cache
  private void onSetParentMsg(Messages.SetParentMsg msg) {
    this.parent = msg.parent;
    System.out.println("Cache " + this.id + ";setParent;parent = " + msg.parent + ";");
  }

  // This message is used to handle the read request message which can come both by a L1 cache or from a Client
  // If the requested element is in the cache, the value associated to the key is returned to the requester
  // Else the message is forwarded to the parent cache
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

  // This message is used to handle the write request message.
  // A cache can only forward the request to its parent till it reach the DB.
  private void onWriteReqMsg(Messages.WriteReqMsg msg){
    sendMessage(msg, parent);
  }


  // This message is used to handle the read response message.
  // After a read the cache needs to store the value in its memory and then forward it to its children
  private void onReadRespMsg(Messages.ReadRespMsg msg) {
    Integer key = msg.key;
    savedItems.put(key, msg.value);
    ActorRef nextHop = msg.responsePath.pop();
    sendMessage(msg, nextHop);
  }

  // This message is used to handle the refill message. This message is the response to a write request.
  // The cache will first check if the value is stored in its memory, in that case will update it
  // Then, if the cache is a L1 cache, it will simply forward the message to all its children
  // If the cache is a L2 cache, it will check if the originator of the request is one of its children
  // If yes the L2 cache will send the confirmation of the write operation to the client
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


  // This method is used to send a message to a given actor, is needed to simulate the network delays
  private void sendMessage(Serializable m, ActorRef dest){
    try { Thread.sleep(rnd.nextInt(10)); }
    catch (InterruptedException e) { e.printStackTrace(); }
    dest.tell(m, getSelf());
  }
}
