package EasyCache.Devices;

import EasyCache.Messages.*;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import EasyCache.Messages.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.util.*;

public class DB extends AbstractActor {
  private Random rnd = new Random();
  private List<ActorRef> children; // the list of children (L2 caches)
  private final int id = -1;         // ID of the current actor

  private HashMap<Integer, Integer> items;

  private static final Logger LOGGER = LogManager.getLogger(DB.class);
  /* -- Actor constructor --------------------------------------------------- */

  public DB(HashMap<Integer, Integer> items) {
    this.items=new HashMap<>();
    this.items.putAll(items);

  }
  static public Props props(HashMap<Integer, Integer> items) {
    return Props.create(DB.class, () -> new DB(items));
  }

  /* -- Actor behaviour ----------------------------------------------------- */


  // This method is called when a ReadReqMsg is received.
  // The DB will get the ActorRef to which he needs to send the response.
  // Then the DB will get the requested item from its memory.
  // After that it will create the response message and then send it.
  private void onReadReqMsg(ReadReqMsg msg){
    ActorRef nextHop = msg.responsePath.pop();
    Integer key = msg.key;
    ReadRespMsg resp = new ReadRespMsg(key, this.items.get(key), msg.responsePath, msg.uuid);
    try { Thread.sleep(rnd.nextInt(10)); }
    catch (InterruptedException e) { e.printStackTrace(); }
    nextHop.tell(resp, getSelf());
  }

  // This method is called when a WriteReqMsg is received.
  // The DB will update the item with the new value and then will send a Refill message to everyone.
  private void onWriteReqMsg(WriteReqMsg msg){
    Integer key = msg.key;
    RefillMsg resp = new RefillMsg(key, msg.newValue, msg.originator, msg.uuid);
    multicast(resp);
  }

  // This method is called when a SetChildrenMsg is received.
  // This method is used to set the children of the DB.
  private void onSetChildrenMsg(SetChildrenMsg msg) {
    this.children = msg.children;
    StringBuilder sb = new StringBuilder();
    for (ActorRef c: children) {
      sb.append(c.path().name() + "; ");
    }
    LOGGER.debug("DB " + this.id + "; children_set_to: [" + sb + "]");
  }

  // This methode is trigger when a InternalStateMsg is received.
  // This methode will print the current state of the cache, so the saved item and the list of children.
  private void onInternalStateMsg(InternalStateMsg msg) {
    StringBuilder sb = new StringBuilder();
    sb.append("INTERNAL_STATE: DB " + this.id + "; items: [");
    for(Integer k : items.keySet()){
      sb.append(k + ":" + items.get(k) + ";");
    }
    sb.append("]; children: [");
    for(ActorRef ch : children){
      sb.append(ch.path().name() + ";");
    }
    sb.append("];");
    LOGGER.debug(sb);
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
