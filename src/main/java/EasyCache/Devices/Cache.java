package EasyCache.Devices;

import EasyCache.Messages.*;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import EasyCache.CacheType;
import EasyCache.Messages.*;

import java.io.Serializable;
import java.util.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
public class Cache extends AbstractActor {

  private Random rnd = new Random(); // Used to generate random value
  private ActorRef parent; // reference to the parent, may be a L1 Cache or the DB
  private List<ActorRef> children; // the list of children, they can be or a list of L2 caches or a list of Clients
  private final int id; // ID of the current actor
  private final CacheType type; // type of the current cache, may be L1 or L2

  private HashMap<Integer, Integer> savedItems; // the items saved in the cache

  private static final Logger LOGGER = LogManager.getLogger(Cache.class);
  /* -- Actor constructor --------------------------------------------------- */

  public Cache(int id, CacheType type) {
    this.id = id;
    this.type=type;
    this.savedItems=new HashMap<>();
  }

  static public Props props(int id, CacheType type) {
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
      .match(SetChildrenMsg.class, this::onSetChildrenMsg)
      .match(SetParentMsg.class, this::onSetParentMsg)
      .match(ReadReqMsg.class, this::onReadReqMsg)
      .match(ReadRespMsg.class, this::onReadRespMsg)
      .match(WriteReqMsg.class, this::onWriteReqMsg)
      .match(RefillMsg.class, this::onRefillMsg)
      .match(InternalStateMsg.class, this::onInternalStateMsg)
      .build();
  }

  // This method is used to set the children of the cache. Is triggered by a SetChildrenMsg message.
  private void onSetChildrenMsg(SetChildrenMsg msg) {
    this.children = msg.children;
    StringBuilder sb = new StringBuilder();
    for (ActorRef c: children) {
      sb.append(c.path().name() + ";");
    }
    LOGGER.info("Cache " + this.id + "; children_set_to: [" + sb + "]");
  }

  // This method is used to set the parent of the cache. Is triggered by a SetParentMsg message.
  private void onSetParentMsg(SetParentMsg msg) {
    this.parent = msg.parent;
    LOGGER.info("Cache " + this.id + "; parent_set_to: " + msg.parent.path().name() + ";");
  }

  // This method is used to handle the ReadReqMsg message that represent the read request message.
  // This message can come both by a L1 cache or from a Client
  // If the requested element is in the cache, the value associated to the key is returned to the requester
  // Else the message is forwarded to the parent cache
  private void onReadReqMsg(ReadReqMsg msg){
    if(savedItems.containsKey(msg.key)){
      ActorRef nextHop=msg.responsePath.pop();
      Integer key = msg.key;
      ReadRespMsg resp = new ReadRespMsg(key, this.savedItems.get(key), msg.responsePath);
      LOGGER.info("Cache " + this.id + "; read_req_for_item: " + msg.key + "; cached_value: " + this.savedItems.get(msg.key) + ";");
      sendMessage(resp, nextHop);
    }else{
      msg.responsePath.push(getSelf());
      LOGGER.info("Cache " + this.id + "; read_req_for_item: " + msg.key + "; forward_to_parent: " + parent.path().name() + ";");
      sendMessage(msg, parent);
    }
  }

  // This method is used to handle the WriteReqMsg message which represent the write request message.
  // A cache can only forward the request to its parent till it reach the DB.
  private void onWriteReqMsg(WriteReqMsg msg){
    LOGGER.info("Cache " + this.id + "; write_req_for_item: " + msg.key + "; forward_to_parent: " + parent.path().name() + ";");
    sendMessage(msg, parent);
  }


  // This method is used to handle the ReadRespMsg message which represent the read response message.
  // After a read the cache needs to store the value in its memory and then forward it to its children
  private void onReadRespMsg(ReadRespMsg msg) {
    Integer key = msg.key;
    savedItems.put(key, msg.value);
    ActorRef nextHop = msg.responsePath.pop();
    LOGGER.info("Cache " + this.id + "; read_resp_for_item = " + msg.key + "; forward_to " + nextHop.path().name() + ";");
    sendMessage(msg, nextHop);
  }

  // This method is used to handle the RefillMsg message. The RefillMsg message represent the ack of a write request.
  // The cache will first check if the value is stored in its memory, in that case will update it
  // Then, if the cache is a L1 cache, it will simply forward the message to all its children
  // If the cache is a L2 cache, it will check if the originator of the request is one of its children
  // If yes the L2 cache will send the confirmation of the write operation to the client
  private void onRefillMsg(RefillMsg msg) {
    Integer key = msg.key;
    if(savedItems.containsKey(key)){
      LOGGER.info("Cache " + this.id + "; update_value_for_item: " + msg.key + ";");
      savedItems.put(key, msg.newValue);
    }
    if(this.type == CacheType.L1){
      multicast(msg);
    }else if(this.type == CacheType.L2){
      ActorRef originator = msg.originator;
      if(children.contains(originator)) {
        WriteConfirmMsg resp = new WriteConfirmMsg(msg.key);
        LOGGER.info("Cache " + this.id + "; write_ack_for_item: " + msg.key + "; forward_to: " + msg.originator.path().name() + ";");
        sendMessage(resp, originator);
      }
    }
  }


  // This methode is trigger when a InternalStateMsg is received.
  // This methode will print the current state of the cache, so the saved item, the list of children and its parent
  private void onInternalStateMsg(InternalStateMsg msg) {
    StringBuilder sb = new StringBuilder();
    sb.append("INTERNAL_STATE: Cache " + this.id + "; items: [");
    for(Integer k : savedItems.keySet()){
      sb.append(k + ":" + savedItems.get(k) + ";");
    }
    sb.append("]; children: [");
    for(ActorRef ch : children){
      sb.append(ch.path().name() + ";");
    }
    sb.append("]; Parent: " + parent.path().name());

    LOGGER.info(sb);
  }

  // This method is used to send a message to a given actor, is needed to simulate the network delays
  private void sendMessage(Serializable m, ActorRef dest){
    try { Thread.sleep(rnd.nextInt(10)); }
    catch (InterruptedException e) { e.printStackTrace(); }
    dest.tell(m, getSelf());
  }
}
