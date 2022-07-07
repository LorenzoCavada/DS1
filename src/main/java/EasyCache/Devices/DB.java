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

  /* -- START OF Sending message methods ----------------------------------------------------- */

  /**
   * This method is used to send a message to a given actor, it will also simulate the network delays
   * @param m is the message to send
   * @param dest is the actorRef of the destination actor
   */
  private void sendMessage(Serializable m, ActorRef dest){
    try { Thread.sleep(rnd.nextInt(10)); }
    catch (InterruptedException e) { e.printStackTrace(); }
    dest.tell(m, getSelf());
  }

  /**
   * This method is used to send a message to all the children of the cache.
   * The multicast will use Thread.sleep in order to simulate a network delay.
   * @param m represents the message to be sent to the children.
   */
  private void multicast(Serializable m) {
    for (ActorRef p: children) {
      p.tell(m, getSelf());
      try { Thread.sleep(rnd.nextInt(10)); }
      catch (InterruptedException e) { e.printStackTrace(); }

    }
  }

  /* -- END of Sending message methods ----------------------------------------------------- */



  /* -- START OF Configuration message methods ----------------------------------------------------- */

  /**
   * This method is called when a SetChildrenMsg is received.
   * This method is used to set the children of the DB when the system is created.
   * @param msg is the SetChildrenMsg message which contains the list of children of the DB.
   */
  private void onSetChildrenMsg(SetChildrenMsg msg) {
    this.children = msg.children;
    StringBuilder sb = new StringBuilder();
    for (ActorRef c: children) {
      sb.append(c.path().name() + "; ");
    }
    LOGGER.debug("DB " + this.id + "; children_set_to: [" + sb + "];");
  }

  /* -- END OF Configuration message methods ----------------------------------------------------- */



  /* -- START OF read and write message methods ----------------------------------------------------- */

  /**
   * This method is called when a ReadReqMsg is received.
   * The DB will get the ActorRef to which he needs to send the response by popping the first element of the responsePath contained in the request.
   * The responsePath is the list of the ActorRefs that the message has gone through.
   * Then the DB will get the requested item from its memory.
   * After that it will create the response message and then send it.
   * @param msg
   */
  private void onReadReqMsg(ReadReqMsg msg){
    ActorRef nextHop = msg.responsePath.pop();
    Integer key = msg.key;
    ReadRespMsg resp = new ReadRespMsg(key, this.items.get(key), msg.responsePath, msg.uuid);
    LOGGER.debug("DB " + this.id + "; read_request_received_from: " + nextHop.path().name() + "; key: " + key + "; read_response_sent;");
    sendMessage(resp, nextHop);
  }

  /**
   * This method is called when a WriteReqMsg is received.
   * The DB will update the item with the new value and then will send a Refill message to all its children.
   * @param msg is the WriteReqMsg message which contains the key and the value to be updated.
   */
  private void onWriteReqMsg(WriteReqMsg msg){
    Integer key = msg.key;
    RefillMsg resp = new RefillMsg(key, msg.newValue, msg.originator, msg.uuid);
    LOGGER.debug("DB " + this.id + "; write_request_received_for_key: " + key + "; value: " + msg.newValue + "; write_performed");
    multicast(resp);
  }

  /* -- START OF read and write message methods ----------------------------------------------------- */



  /* -- START OF crash handling message methods ----------------------------------------------------- */

  /**
   * This methode is trigger when a AddChildMsg is received.
   * This can happen when a L2 cache detects that its L1 parent cache has crashed and want to set the DB has its new parent.
   * This cache needs to be added to the list of children of the DB.
   * @param msg is the AddChildMsg message which contains the ActorRef of the new child.
   */
  private void onAddChildMsg(AddChildMsg msg) {
    if (!this.children.contains(msg.child))
      this.children.add(msg.child);
    StringBuilder sb = new StringBuilder();
    for (ActorRef c: children) {
      sb.append(c.path().name() + ";");
    }
    LOGGER.debug("DB; adding_new_child: " + msg.child.path().name() + "; new_children_list: [" + sb + "];");
  }

  /* -- END OF crash handling message methods ----------------------------------------------------- */



  /* -- START OF debug message methods ----------------------------------------------------- */

  /**
   * This methode is trigger when a InternalStateMsg is received.
   * This methode will print the current state of the cache, so the saved item and the list of children.
   * @param msg
   */
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

  /* -- END OF debug message methods ----------------------------------------------------- */

  /**
   * Here we define the mapping between the received message types and our actor methods
   */
  @Override
  public Receive createReceive() {
    return receiveBuilder()
      .match(ReadReqMsg.class,    this::onReadReqMsg)
      .match(AddChildMsg.class, this::onAddChildMsg)
      .match(WriteReqMsg.class,    this::onWriteReqMsg)
      .match(SetChildrenMsg.class,    this::onSetChildrenMsg)
      .match(InternalStateMsg.class,   this::onInternalStateMsg)
      .build();
  }
}
