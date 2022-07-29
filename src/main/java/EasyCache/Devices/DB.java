package EasyCache.Devices;

import EasyCache.CacheType;
import EasyCache.Config;
import EasyCache.Messages.*;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import scala.concurrent.duration.Duration;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * This cache represent the behaviour of the database.
 */
public class DB extends AbstractActor {
  /**
   * Used to generate random value.
   */
  private Random rnd = new Random();

  /**
   * the list of children. At the beginning it is a list of L1 {@link Cache caches}. Then it could become a mixed list of
   * L2 and L1 {@link Cache caches}.
   */
  private List<ActorRef> children;
  /**
   * numeric ID of the database.
   */
  private final int id = -1;

  /**
   * the full set of items in the system, as a map (key, value).
   */
  private HashMap<Integer, Integer> items;

  /**
   * this map contains the set of children for which we have received a {@link InvalidationItemConfirmMsg} for a given
   * {@link CritWriteReqMsg critical write request}, represent by its uuid.
   */
  private Map<UUID, Set<ActorRef>> receivedInvalidAck;

  /**
   * map of all the critical write request for which we are waiting the {@link InvalidationItemConfirmMsg} with corresponding timer.
   */
  private Map<UUID, Cancellable> invalidAckTimeouts;

  /**
   * data structure containing the uuids of all ongoing {@link CritWriteReqMsg}, to easily send a {@link CritRefillMsg critical refill}
   * after having received all the {@link InvalidationItemConfirmMsg invalid confirmations}.
   */
  private Map<UUID, CritWriteReqMsg> critWrites;

  /**
   * Time of last sending of message. It is used to guarantee fifoness in sending messages.
   */
  private long timeLastSend;
  /**
   * Delay of last sent message. It is used to guarantee fifoness.
   */
  private int lastDelay;

  private static final Logger LOGGER = LogManager.getLogger(DB.class);

  /* -- Actor constructor --------------------------------------------------- */

  /**
   * Constructor of the DB actor.
   * @param items the set of items in the system.
   */
  public DB(HashMap<Integer, Integer> items) {
    this.items=new HashMap<>();
    this.items.putAll(items);
    this.receivedInvalidAck=new HashMap<>();
    this.invalidAckTimeouts=new HashMap<>();
    this.critWrites =new HashMap<>();
    this.timeLastSend=System.currentTimeMillis();
    this.lastDelay=0;
  }
  static public Props props(HashMap<Integer, Integer> items) {
    return Props.create(DB.class, () -> new DB(items));
  }

  /* -- Actor behaviour ----------------------------------------------------- */

  /* -- START OF Sending message methods ----------------------------------------------------- */

  /**
   * This method is used to send a {@link Message} to a given actor, simulating also a network delay.
   * @param m the {@link Message} to send.
   * @param dest the reference of the destination actor.
   */
  private void sendMessage(Message m, ActorRef dest){
    int delay = rnd.nextInt(Config.SEND_MAX_DELAY);
    this.lastDelay=delay;
    long thisTime=System.currentTimeMillis();
    if (lastDelay > (thisTime-timeLastSend)){
      delay+=lastDelay - (thisTime-timeLastSend);
    }
    getContext().system().scheduler().scheduleOnce(
            Duration.create(delay, TimeUnit.MILLISECONDS),        // when to send the message
            dest,                                          // destination actor reference
            m,                                  // the message to send
            getContext().system().dispatcher(),                 // system dispatcher
            getSelf()                                           // source of the message (myself)
    );
    this.timeLastSend=thisTime;

  }

  /**
   * This method is used to send a {@link Message} to all the children of the cache.
   * Will call sendMessage to send the {@link Message} to a child.
   * @param m the {@link Message} to be sent to the children.
   */
  private void multicast(Message m) {
    for (ActorRef p: children) {
      sendMessage(m, p);
    }
  }

  /* -- END of Sending message methods ----------------------------------------------------- */



  /* -- START OF Configuration message methods ----------------------------------------------------- */

  /**
   * This method is used to set the children of the database at the initialization of the tree.
   * @param msg the {@link SetChildrenMsg} message which contain the list of children to set.
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
   * This method is used to handle the arrival of a {@link ReadReqMsg} message.
   * The DB will create a {@link ReadRespMsg response} with the value associated to the requested key.
   * The message will be sent to the child (a {@link Cache cache}) popped from responsePath object.
   * @param msg the {@link ReadReqMsg} message which contains the key of the element to be read.
   */
  private void onReadReqMsg(ReadReqMsg msg) {
    ActorRef nextHop = msg.responsePath.pop();
    Integer key = msg.key;
    LOGGER.debug("DB " + this.id + "; read_request_received_from: " + nextHop.path().name() + "; key: " + key + "; MSG_ID: " + msg.uuid + "; read_response_sent; ");
    ReadRespMsg resp = new ReadRespMsg(key, this.items.get(key), msg.responsePath, msg.uuid);
    sendMessage(resp, nextHop);
  }

  /**
   * This method is used to handle the arrival of a {@link CritReadReqMsg} message.
   * The DB will create a {@link CritReadRespMsg response} with the value associated to the requested key.
   * The message will be sent to the child (a {@link Cache cache}) popped from responsePath object.
   * @param msg the {@link CritReadReqMsg} message which contains the key of the element to be read.
   */
  private void onCritReadReqMsg(CritReadReqMsg msg){
    ActorRef nextHop = msg.responsePath.pop();
    Integer key = msg.key;
    LOGGER.debug("DB " + this.id + "; critical_read_request_received_from: " + nextHop.path().name() + "; key: " + key + "; MSG_ID: " + msg.uuid + "; critical_read_response_sent;");
    CritReadRespMsg resp = new CritReadRespMsg(key, this.items.get(key), msg.responsePath, msg.uuid);
    sendMessage(resp, nextHop);
  }

  /**
   * This method is used to handle the arrival of a {@link CritWriteReqMsg} message.
   * The request is saved in the map of all the ongoing critical writes.
   * The DB will first ask all the {@link Cache caches} to invalidate the item associated with the key of the {@link CritWriteReqMsg request}.
   * Then it will wait the confirmation of invalidations from ALL its children. A timer is set for this purpose for detect a possible crash
   * of one of its children and potentially going in timeout.
   * @param msg the {@link CritWriteReqMsg} message which contains the key of the element to be read and the new value to set.
   */
  private void onCritWriteReqMsg(CritWriteReqMsg msg){
    Integer key = msg.key;
    if(!isPerformingCritWriteOnItem(msg.key)){
      LOGGER.debug("DB " + this.id + "; crit_write_request_received_for_key: " + key + "; value: " + msg.newValue + "; MSG_ID: " + msg.uuid + "; sending_invalidation");
      this.critWrites.put(msg.uuid, msg);
      InvalidationItemMsg invalidMsg=new InvalidationItemMsg(msg.key, msg.uuid);
      invalidAckTimeouts.put(msg.uuid,
              getContext().system().scheduler().scheduleOnce(
                      Duration.create(Config.TIMEOUT_DB_INVALIDATION, TimeUnit.MILLISECONDS),        // when to send the message
                      getSelf(),                                          // destination actor reference
                      new TimeoutInvalidAckMsg(invalidMsg),                                  // the message to send
                      getContext().system().dispatcher(),                 // system dispatcher
                      getSelf()                                           // source of the message (myself)
              )); //adding the uuid of the message to the list of the pending ones
      multicast(invalidMsg);
    }else{
      LOGGER.error("DB " + this.id + "; crit_write_request_received_for_key: " + key + "; value: " + msg.newValue + "; MSG_ID: " + msg.uuid + "; already_in_progress");
      CritWriteErrorMsg resp = new CritWriteErrorMsg(msg.key, msg.originator, msg.uuid);
      multicast(resp);
    }
  }

  /**
   * Service method to check if there is an ongoing {@link CritWriteReqMsg critical write} on a item with a given key.
   * @param key the key of the item we check for ongoing {@link CritWriteReqMsg critical writes}.
   * @return {@code true} if there is an ongoing {@link CritWriteReqMsg critical write} on the item, {@code false} otherwise.
   */
  private boolean isPerformingCritWriteOnItem(int key){
    boolean result = false;
    for(CritWriteReqMsg m : this.critWrites.values()){
      if(m.key==key){
        result=true;
        break;
      }
    }
    return result;
  }

  /**
   * This method is used to handle the arrival of a {@link InvalidationItemConfirmMsg} message.
   * The message means that a child of the DB has correctly invalidated the item.
   * When ALL confirmations for a given {@link InvalidationItemMsg} are arrived, the DB will update the item and will send
   * a {@link CritRefillMsg} to all its children.
   * @param msg is the {@link InvalidationItemConfirmMsg} message which confirms that the sender has marked the item as invalid.
   */
  private void onInvalidationItemConfirmMsg(InvalidationItemConfirmMsg msg){
    LOGGER.debug("DB " + this.id + "; invalidation_confirm_for_item: " + msg.key + "; from " + getSender().path().name() + "; MSG_ID: " + msg.uuid + ";");
    if(this.receivedInvalidAck.containsKey(msg.uuid)){
      this.receivedInvalidAck.get(msg.uuid).add(getSender());
    }else{
      this.receivedInvalidAck.put(msg.uuid, new HashSet<>());
      this.receivedInvalidAck.get(msg.uuid).add(getSender());
    }

    if(this.receivedInvalidAck.get(msg.uuid).size()==this.children.size()){
      this.invalidAckTimeouts.get(msg.uuid).cancel();
      CritWriteReqMsg associatedReq=this.critWrites.get(msg.uuid);
      items.put(associatedReq.key, associatedReq.newValue);
      LOGGER.debug("DB " + this.id + "; crit_write_performed_for_key: " + associatedReq.key + "; value: " + associatedReq.newValue + "; MSG_ID: " + msg.uuid + "; sending_refill");
      CritRefillMsg resp = new CritRefillMsg(associatedReq.key, associatedReq.newValue, associatedReq.originator, associatedReq.uuid);
      multicast(resp);
      this.critWrites.remove(msg.uuid);
      this.receivedInvalidAck.get(msg.uuid).clear();
      this.receivedInvalidAck.remove(msg.uuid);
      this.invalidAckTimeouts.remove(msg.uuid);
    }
  }

  /**
   * This method is used to handle the arrival of a {@link TimeoutInvalidAckMsg} message.
   * This is triggered when the database detects the crash of one of its children while waiting for a {@link InvalidationItemConfirmMsg}.
   * This means that the DB cannot ensure that all the cache will stop sending the old value to clients.
   * Thus a critical write cannot be successful. The DB will send a {@link CritWriteErrorMsg} to the originator of the
   * corresponding {@link CritWriteReqMsg critical write request}.
   * @param msg is the {@link TimeoutInvalidAckMsg} message
   */
  private void onTimeoutInvalidAckMsg(TimeoutInvalidAckMsg msg){
    UUID req = msg.awaitedMsg.uuid;
    StringBuilder sb = new StringBuilder();
    for(ActorRef child : this.children){
      if(!receivedInvalidAck.get(req).contains(child)){
        sb.append(child.path().name() + "; ");
      }
    }
    LOGGER.warn("DB " + this.id + "; invalidation_confirm_timeout_for_item: " + msg.awaitedMsg.key + "; waiting_for: " + sb + "; ");
    CritWriteReqMsg associatedReq=this.critWrites.get(msg.awaitedMsg.uuid);
    //check for akka bugs
    if(this.receivedInvalidAck.get(msg.awaitedMsg.uuid).size()==this.children.size()){
      this.invalidAckTimeouts.get(msg.awaitedMsg.uuid).cancel();
      items.put(associatedReq.key, associatedReq.newValue);
      CritRefillMsg resp = new CritRefillMsg(associatedReq.key, associatedReq.newValue, associatedReq.originator, associatedReq.uuid);
      LOGGER.debug("DB " + this.id + "; crit_write_performed_for_key: " + associatedReq.key + "; value: " + associatedReq.newValue + "; MSG_ID: " + associatedReq.uuid + ";");
      multicast(resp);
      this.critWrites.remove(msg.awaitedMsg.uuid);
      this.receivedInvalidAck.get(msg.awaitedMsg.uuid).clear();
      this.receivedInvalidAck.remove(msg.awaitedMsg.uuid);
      this.invalidAckTimeouts.remove(msg.awaitedMsg.uuid);
    }else{
      LOGGER.debug("DB " + this.id + "; crit_write_error_for_key: " + msg.awaitedMsg.key + "; MSG_ID: " + associatedReq.uuid + "; sending_error;");
      CritWriteErrorMsg resp = new CritWriteErrorMsg(associatedReq.key, associatedReq.originator, associatedReq.uuid);
      multicast(resp);
      this.critWrites.remove(msg.awaitedMsg.uuid);
      this.receivedInvalidAck.remove(msg.awaitedMsg.uuid);
      this.invalidAckTimeouts.remove(msg.awaitedMsg.uuid);
    }
  }

  /**
   * This method is used to handle the arrival of a {@link WriteReqMsg} message.
   * The DB will update the item with the new value and then will send a {@link RefillMsg} to all its children.
   * @param msg the {@link WriteReqMsg} message which contains the key of the element to be updated and the new value.
   */
  private void onWriteReqMsg(WriteReqMsg msg){
    Integer key = msg.key;
    items.put(key, msg.newValue);
    RefillMsg resp = new RefillMsg(key, msg.newValue, msg.originator, msg.uuid);
    LOGGER.debug("DB " + this.id + "; write_request_received_for_key: " + key + "; value: " + msg.newValue + "; MSG_ID: " + msg.uuid + "; write_performed");
    multicast(resp);
  }

  /* -- START OF read and write message methods ----------------------------------------------------- */



  /* -- START OF crash handling message methods ----------------------------------------------------- */

  /**
   * This method is used to add a new child to the database.
   * This usually happen when a L2 {@link Cache} detect the crash of its parent (L1 {@link Cache}) and choose the database as parent.
   * @param msg the {@link AddChildMsg} message which contains the reference to the new child to add to the list of the
   * children of the database.
   */
  private void onAddChildMsg(AddChildMsg msg) {
    if (!this.children.contains(msg.child))
      this.children.add(msg.child);
    StringBuilder sb = new StringBuilder();
    for (ActorRef c : children) {
      sb.append(c.path().name() + ";");
    }
    LOGGER.debug("DB; adding_new_child: " + msg.child.path().name() + "; new_children_list: [" + sb + "];");
  }

  /**
   * This method is used to handle the arrival of a {@link RefreshItemReqMsg} message.
   * This is triggered by a L2 {@link Cache} when its L1 parent recovers from a crash or when a L2 {@link Cache} set the
   * database as new parent.
   * The DB will create a {@link RefreshItemRespMsg response} with the value associated to the requested key.
   * The message will be sent to the child (a {@link Cache cache}) popped from responsePath object.
   * @param msg the {@link RefreshItemReqMsg} message contains the key of the element to be read from the database.
   */
  private void onRefreshItemReqMsg(RefreshItemReqMsg msg){
    ActorRef nextHop = msg.responsePath.pop();
    Integer key = msg.key;
    RefreshItemRespMsg resp = new RefreshItemRespMsg(key, this.items.get(key), msg.responsePath, msg.uuid);
    LOGGER.debug("DB " + this.id + "; refresh_request_received_from: " + nextHop.path().name() + "; key: " + key + "; refresh_response_sent;");
    sendMessage(resp, nextHop);
  }
  /* -- END OF crash handling message methods ----------------------------------------------------- */



  /* -- START OF debug message methods ----------------------------------------------------- */

  /**
   * This method is triggered when a {@link InternalStateMsg} is received from the {@link EasyCache.ProjectRunner runner}.
   * This method is used for debugging. It will print the current state of the database: the items and the list of children.
   * @param msg is the {@link InternalStateMsg} message, is an empty message used to print the internal state of the cache.
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
   * The mapping between the received message types and our actor methods in the normal behaviour.
   */
  @Override
  public Receive createReceive() {
    return receiveBuilder()
      .match(AddChildMsg.class, this::onAddChildMsg)
      .match(SetChildrenMsg.class,    this::onSetChildrenMsg)
      .match(InternalStateMsg.class,   this::onInternalStateMsg)
      .match(RefreshItemReqMsg.class,   this::onRefreshItemReqMsg)
      .match(CritReadReqMsg.class,   this::onCritReadReqMsg)
      .match(CritWriteReqMsg.class,   this::onCritWriteReqMsg)
      .match(ReadReqMsg.class,    this::onReadReqMsg)
      .match(WriteReqMsg.class,    this::onWriteReqMsg)
      .match(InvalidationItemConfirmMsg.class,   this::onInvalidationItemConfirmMsg)
      .match(TimeoutInvalidAckMsg.class,   this::onTimeoutInvalidAckMsg)
      .build();
  }
}
