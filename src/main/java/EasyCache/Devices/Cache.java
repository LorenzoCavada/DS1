package EasyCache.Devices;

import EasyCache.Config;
import EasyCache.CrashType;
import EasyCache.Messages.*;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import EasyCache.CacheType;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import scala.concurrent.duration.Duration;

/**
 * This cache represent the behaviour of a cache.
 */
public class Cache extends AbstractActor {

  /**
   * Used to generate random value.
   */
  private Random rnd = new Random();

  /**
   * reference to the parent, may be a L1 {@link Cache} or the {@link DB}.
   */
  private ActorRef parent;
  /**
   * reference to the {@link DB}, used when changing parent to {@link DB} in a L2 {@link Cache}.
   */
  private ActorRef db;
  /**
   * the list of children, they can be or a list of L2 {@link Cache caches} (in L1 {@link Cache}) or a list of {@link Client clients} (in L2 {@link Cache}).
   */
  private List<ActorRef> children;
  /**
   * numeric ID of this cache.
   */
  private final int id;
  /**
   * type of the this cache, may be L1 or L2.
   */
  private final CacheType type;

  /**
   * map of all the pending request which are still waiting for a response, with the corresponding timer.
   */
  private Map<UUID, Cancellable> pendingReq;
  /**
  * the items saved in the cache, as a map (key, value).
  */
  private HashMap<Integer, Integer> savedItems;

  /**
   * items that are temporarily invalid while critical write is propagated.
   */
  private Set<Integer> invalidItems;
  /**
   * map of all the critical write request for which we are waiting the refill with corresponding timer.
   */
  private Map<UUID, Cancellable> pendingUpdates;
  /**
   * in L1 {@link Cache caches}, this map contains the set of children for which we have received a {@link InvalidationItemConfirmMsg}
   * for a given {@link CritWriteReqMsg critical write request}, represent by its uuid.
   */
  private Map<UUID, Set<ActorRef>> invalidConfirmations;

  /**
  * next scheduled {@link CrashType crash}.
  */
  private CrashType nextCrash;

  /**
   * in case of next scheduled {@link CrashType crash} during a multicast, this parameter will tell after how many multicast a L1 {@link Cache cache} crashes.
   */
  private int afterNMessageSent;

  /**
   * in case of custom time of recovery (through the parameter in {@link CrashMsg} or {@link CrashDuringMulticastMsg}),
   * this cache recovers after this amount of milliseconds.
   */
  private int recoveryAfter;

  private static final Logger LOGGER = LogManager.getLogger(Cache.class); //the instance for the logger
  /* -- Actor constructor --------------------------------------------------- */

  /**
   * Constructor of the Cache actor.
   * @param id the ID of constructed cache.
   * @param type the {@link CacheType type} of the constructed cache, may be L1 or L2.
   * @param db the reference to the {@link DB} actor.
   */
  public Cache(int id, CacheType type, ActorRef db) {
    this.id = id;
    this.type=type;
    this.db=db;
    this.savedItems=new HashMap<>();
    this.pendingReq= new HashMap<>();
    this.invalidItems=new HashSet<>();
    this.pendingUpdates=new HashMap<>();
    this.invalidConfirmations=new HashMap<>();
    this.nextCrash=CrashType.NONE;
    this.afterNMessageSent=Integer.MAX_VALUE;
    this.recoveryAfter=-1;
  }

  static public Props props(int id, CacheType type, ActorRef db) {
    return Props.create(Cache.class, () -> new Cache(id, type, db));
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
    getContext().system().scheduler().scheduleOnce(
            Duration.create(delay, TimeUnit.MILLISECONDS),        // when to send the message
            dest,                                          // destination actor reference
            m,                                  // the message to send
            getContext().system().dispatcher(),                 // system dispatcher
            getSelf()                                           // source of the message (myself)
    );
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

  /**
   * This method is used to crash while sending a {@link Message} to all the children of the cache.
   * Will call sendMessage to send the {@link Message} to a child.
   * @param m the {@link Message} to be sent to the children.
   */
  private void multicastAndCrash(Message m) {
    int i = 0;
    for (ActorRef p: children) {
      if(i>=this.afterNMessageSent){
        crashingOps();
        return;
      }
      sendMessage(m, p);
      i++;
    }
  }

  /* -- END of Sending message methods ----------------------------------------------------- */



  /* -- START OF configuration message methods ----------------------------------------------------- */

  /**
   * This method is used to set the children of the cache at the initialization of the tree.
   * @param msg the {@link SetChildrenMsg} message which contain the list of children to set.
   */
  private void onSetChildrenMsg(SetChildrenMsg msg) {
    this.children = msg.children;
    StringBuilder sb = new StringBuilder();
    for (ActorRef c: children) {
      sb.append(c.path().name() + ";");
    }
    LOGGER.debug("Cache " + this.id + "; children_set_to: [" + sb + "]");
  }

  /**
   * This method is used to add a new child to the cache.
   * This usually happen when a {@link Client} detect the crash of its parent (L2 cache) and choose a new L2 cache as parent.
   * @param msg the {@link AddChildMsg} message which contains the reference to the new child to add to the list of the
   * children of this cache.
   */
  private void onAddChildMsg(AddChildMsg msg) {
    if (!this.children.contains(msg.child))
      this.children.add(msg.child);
    StringBuilder sb = new StringBuilder();
    for (ActorRef c: children) {
      sb.append(c.path().name() + ";");
    }
    LOGGER.debug("Cache " + this.id + "; children_set_to: [" + sb + "]");
  }

  /**
   * This method is used to set the parent of the cache at initialization.
   * @param msg the {@link SetParentMsg} message which contains the reference to the parent to set.
   */
  private void onSetParentMsg(SetParentMsg msg) {
    this.parent = msg.parent;
    LOGGER.debug("Cache " + this.id + "; parent_set_to: " + msg.parent.path().name() + ";");
  }

  /* -- END of configuration message methods ----------------------------------------------------- */



  /* -- START OF write and read message methods ----------------------------------------------------- */

  /**
   * This method is used to handle the arrival of a {@link ReadReqMsg} message.
   * This message can come both from a L2 cache (in a L1 cache) or from a Client (in a L2 cache).
   * If this cache does not have the requested element, the message is forwarded to the parent and a timer is started to
   * check for possible crashes before receiving the associated {@link ReadRespMsg response}.
   * Otherwise this cache will respond with the saved value.
   * @param msg the {@link ReadReqMsg} message which contains the key of the element to be read.
   */
  private void onReadReqMsg(ReadReqMsg msg){
    if (invalidItems.contains(msg.key)){
      ReqErrorMsg errMsg=new ReqErrorMsg(msg);
      ActorRef nextHop = msg.responsePath.pop();
      LOGGER.error("Cache " + this.id + "; read_req_for_item: " + msg.key + "; MSG_ID: " + msg.uuid + "; invalid;");
      sendMessage(errMsg, nextHop);
    }else {
      if (savedItems.containsKey(msg.key)) {
        ActorRef nextHop = msg.responsePath.pop();
        Integer key = msg.key;
        if(this.nextCrash==CrashType.BEFORE_READ_RESP){
          crashingOps();
        }else{
          ReadRespMsg resp = new ReadRespMsg(key, this.savedItems.get(key), msg.responsePath, msg.uuid);
          LOGGER.debug("Cache " + this.id + "; read_req_for_item: " + msg.key + "; MSG_ID: " + msg.uuid + "; cached_value: " + this.savedItems.get(msg.key) + ";");
          sendMessage(resp, nextHop);
        }
      } else {
        if(this.nextCrash==CrashType.BEFORE_READ_REQ_FW){
          crashingOps();
        }else {
          LOGGER.debug("Cache " + this.id + "; read_req_for_item: " + msg.key + "; MSG_ID: " + msg.uuid + "; forward_to_parent: " + parent.path().name() + ";");
          pendingReq.put(msg.uuid,
                  getContext().system().scheduler().scheduleOnce(
                          Duration.create(Config.TIMEOUT_CACHE, TimeUnit.MILLISECONDS),        // when to send the message
                          getSelf(),                                          // destination actor reference
                          new TimeoutReqMsg(msg),                                  // the message to send
                          getContext().system().dispatcher(),                 // system dispatcher
                          getSelf()                                           // source of the message (myself)
                  )); //adding the uuid of the message to the list of the pending ones
          msg.responsePath.push(getSelf());
          if (Config.VERBOSE_LOG)
            LOGGER.debug("Cache " + this.id + "; pending_req_list: " + pendingReq.keySet() + "; adding_req_id: " + msg.uuid + ";");
          sendMessage(msg, parent);
          if(this.nextCrash==CrashType.AFTER_READ_REQ_FW){
            crashingOps();
          }
        }
      }
    }
  }



  /**
   * This method is used to handle arrival of a {@link ReadRespMsg} message.
   * This message can come both from the DB (in both types of caches) or from a L1 (in a L2 cache).
   * This cache stores the value in its memory and then will forward the message to the child in the path towards the
   * originator of the associated {@link ReadReqMsg request}.
   * The timer of the associated {@link ReadReqMsg request} is cancelled.
   * @param msg the {@link ReadRespMsg} message which contains value of the requested item.
   */
  private void onReadRespMsg(ReadRespMsg msg) {
    if(this.nextCrash==CrashType.BEFORE_READ_RESP_FW){
      crashingOps();
    }else {
      Integer key = msg.key;
      savedItems.put(key, msg.value);
      ActorRef nextHop = msg.responsePath.pop();
      LOGGER.debug("Cache " + this.id + "; read_resp_for_item = " + msg.key + "; MSG_ID: " + msg.uuid + "; forward_to " + nextHop.path().name() + "; timeout_cancelled;");
      if(pendingReq.containsKey(msg.uuid)) { // it may happen that the cache has crashed before receiving the response and so lost the list of the pending requests.
        pendingReq.get(msg.uuid).cancel();
        pendingReq.remove(msg.uuid); //removing the uuid of the message from the list of the pending ones
        if (Config.VERBOSE_LOG)
          LOGGER.debug("Cache " + this.id + "; pending_req_list: " + pendingReq.keySet() + "; remove_req_id: " + msg.uuid + ";");
      }
      sendMessage(msg, nextHop);
    }
  }

  /**
   * This method is used to handle the arrival of a {@link WriteReqMsg} message.
   * If the item to be modified is invalid because there is an ongoing {@link CritWriteReqMsg critical write}, a
   * {@link ReqErrorMsg error} is sent to the sender.
   * Otherwise the request will be forwarded to the parent because it is ultimately handled by the {@link DB database}.
   * A timer is started to check for possible crashes before receiving the associated {@link RefillMsg refill}.
   * @param msg the {@link WriteReqMsg} message which contains the key of the element to be written and the value to be written.
   */
  private void onWriteReqMsg(WriteReqMsg msg){
    if (invalidItems.contains(msg.key)){
      ReqErrorMsg errMsg=new ReqErrorMsg(msg);
      LOGGER.error("Cache " + this.id + "; write_req_for_item: " + msg.key + "; MSG_ID: " + msg.uuid + "; crit_write_is_performing;");
      sendMessage(errMsg, getSender());
    }else {
      if(this.nextCrash==CrashType.BEFORE_WRITE_REQ_FW){
        crashingOps();
      }else {
        LOGGER.debug("Cache " + this.id + "; write_req_for_item: " + msg.key + "; MSG_ID: " + msg.uuid + "; forward_to_parent: " + parent.path().name() + ";");
        if (this.type == CacheType.L2) { //if the cache is an L2 cache, the write request is associated with a timer to detect the potential crash of its parent
          pendingReq.put(msg.uuid,
                  getContext().system().scheduler().scheduleOnce(
                          Duration.create(Config.TIMEOUT_CACHE, TimeUnit.MILLISECONDS),        // when to send the message
                          getSelf(),                                          // destination actor reference
                          new TimeoutReqMsg(msg),                                  // the message to send
                          getContext().system().dispatcher(),                 // system dispatcher
                          getSelf()                                           // source of the message (myself)
                  ));
        }
        if (Config.VERBOSE_LOG)
          LOGGER.debug("Cache " + this.id + "; pending_req_list: " + pendingReq.keySet() + "; adding_req_id: " + msg.uuid + ";");
        sendMessage(msg, parent);
        if(this.nextCrash==CrashType.AFTER_WRITE_REQ_FW){
          crashingOps();
        }
      }
    }
  }

  /**
   * This method is used to handle the arrival of a {@link RefillMsg} message.
   * If the propagated item is in this cache memory, the value is updated.
   * If this cache is a L1, the message is forwarded to all its children.
   * Otherwise (the cache is L2) the method sends a {@link WriteConfirmMsg confirmation} to one of its child if it
   * is the originator of associated {@link WriteReqMsg request}.
   * The timer of the associated {@link WriteReqMsg request} is cancelled.
   * @param msg the {@link RefillMsg} message which contains the key of the updated item and the new value.
   */
  private void onRefillMsg(RefillMsg msg) {
    Integer key = msg.key;
    if(this.nextCrash==CrashType.BEFORE_REFILL){
      crashingOps();
    }else {
      if (savedItems.containsKey(key)) {
        LOGGER.debug("Cache " + this.id + "; refill_for_item: " + msg.key + "; MSG_ID: " + msg.uuid + "; value: " + msg.newValue + ";");
        savedItems.put(key, msg.newValue);
      }
      if (this.type == CacheType.L1) {
        if (Config.VERBOSE_LOG)
          LOGGER.debug("Cache " + this.id + "; pending_req_list: " + pendingReq.keySet() + "; remove_req_id: " + msg.uuid + ";");
        // is case is a L1 cache no timer needs to be removed because there is no timer associated to the request sent to the DB duo to the fact that the DB cannot crash
        pendingReq.remove(msg.uuid);
        if(this.nextCrash==CrashType.DURING_REFILL_MULTICAST){
          multicastAndCrash(msg);
        }else {
          multicast(msg);
        }
      } else if (this.type == CacheType.L2) {
        if (Config.VERBOSE_LOG)
          LOGGER.debug("Cache " + this.id + "; pending_req_list: " + pendingReq.keySet() + "; remove_req_id: " + msg.uuid + ";");
        if (pendingReq.containsKey(msg.uuid)) {
          pendingReq.get(msg.uuid).cancel();
          pendingReq.remove(msg.uuid);
        }
        ActorRef originator = msg.originator;
        if (children.contains(originator)) {
          if(this.nextCrash==CrashType.BEFORE_WRITE_CONFIRM){
            crashingOps();
          }else {
            WriteConfirmMsg resp = new WriteConfirmMsg(msg.key, msg.uuid);
            LOGGER.debug("Cache " + this.id + "; write_ack_for_item: " + msg.key + "; MSG_ID: " + msg.uuid + "; forward_to: " + msg.originator.path().name() + "; timeout_cancelled;");
            sendMessage(resp, originator);
          }
        }
      }
    }
  }

  /**
   * This method is used to handle the arrival of a {@link CritReadReqMsg} message.
   * This message can come both from a L2 cache (in a L1 cache) or from a Client (in a L2 cache).
   * The message is forwarded to the parent because it is ultimately handled by the {@link DB database}.
   * A timer is started to check for possible crashes before receiving the associated {@link CritReadRespMsg response}.
   * @param msg the {@link CritReadReqMsg} message which contains the key of the element to be read from the database.
   */
  private void onCritReadReqMsg(CritReadReqMsg msg){
    if (invalidItems.contains(msg.key)){
      ReqErrorMsg errMsg=new ReqErrorMsg(msg);
      ActorRef nextHop = msg.responsePath.pop();
      LOGGER.error("Cache " + this.id + "; crit_read_req_for_item: " + msg.key + "; MSG_ID: " + msg.uuid + "; a_crit_write_is_performing;");
      sendMessage(errMsg, nextHop);
    }else {
      if(this.nextCrash==CrashType.BEFORE_CRIT_READ_REQ_FW){
        crashingOps();
      }else {
        msg.responsePath.push(getSelf());
        LOGGER.debug("Cache " + this.id + "; crit_read_req_for_item: " + msg.key + "; MSG_ID: " + msg.uuid + "; forward_to_parent: " + parent.path().name() + ";");
        pendingReq.put(msg.uuid,
                getContext().system().scheduler().scheduleOnce(
                        Duration.create(Config.TIMEOUT_CACHE, TimeUnit.MILLISECONDS),        // when to send the message
                        getSelf(),                                          // destination actor reference
                        new TimeoutReqMsg(msg),                                  // the message to send
                        getContext().system().dispatcher(),                 // system dispatcher
                        getSelf()                                           // source of the message (myself)
                )); //adding the uuid of the message to the list of the pending ones
        if (Config.VERBOSE_LOG)
          LOGGER.debug("Cache " + this.id + "; pending_req_list: " + pendingReq.keySet() + "; adding_req_id: " + msg.uuid + ";");
        sendMessage(msg, parent);
        if(this.nextCrash==CrashType.AFTER_CRIT_READ_REQ_FW){
          crashingOps();
        }
      }
    }
  }

  /**
   * This method is used to handle arrival of a {@link CritReadRespMsg} message.
   * This message can come both from the DB (in both types of caches) or from a L1 (in a L2 cache).
   * This cache stores the value in its memory and then will forward the message to the child in the path towards the
   * originator of associated {@link CritReadReqMsg request}.
   * The timer of the associated {@link CritReadReqMsg request} is cancelled.
   * @param msg the {@link CritReadRespMsg} message which contains value of the requested item.
   */
  private void onCritReadRespMsg(CritReadRespMsg msg) {
    if(this.nextCrash==CrashType.BEFORE_CRIT_READ_RESP_FW){
      crashingOps();
    }else {
      Integer key = msg.key;
      savedItems.put(key, msg.value);
      ActorRef nextHop = msg.responsePath.pop();
      LOGGER.debug("Cache " + this.id + "; crit_read_resp_for_item = " + msg.key + "; MSG_ID: " + msg.uuid + "; forward_to " + nextHop.path().name() + "; timeout_cancelled;");
      if(pendingReq.containsKey(msg.uuid)) {
        pendingReq.get(msg.uuid).cancel();
        pendingReq.remove(msg.uuid); //removing the uuid of the message from the list of the pending ones
        if (Config.VERBOSE_LOG)
          LOGGER.debug("Cache " + this.id + "; pending_req_list: " + pendingReq.keySet() + "; remove_req_id: " + msg.uuid + ";");
      }
      sendMessage(msg, nextHop);
    }
  }

  /**
   * This method is used to handle the arrival of a {@link CritWriteReqMsg} message.
   * If the item to be modified is invalid because there is another ongoing {@link CritWriteReqMsg critical write}, a
   * {@link ReqErrorMsg error} is sent to the sender.
   * Otherwise the request will be forwarded because it is ultimately handled by the {@link DB database}.
   * A timer is started to check for possible crashes before receiving the associated {@link InvalidationItemMsg invalidation}.
   * @param msg the {@link CritWriteReqMsg} message which contains the key of the element to be written and the value to be written.
   */
  private void onCritWriteReqMsg(CritWriteReqMsg msg){
    if (invalidItems.contains(msg.key)) {
      ReqErrorMsg errMsg = new ReqErrorMsg(msg);
      LOGGER.error("Cache " + this.id + "; crit_write_req_for_item: " + msg.key + "; MSG_ID: " + msg.uuid + "; another_crit_write_is_performing;");
      sendMessage(errMsg, getSender());
    }else {
      LOGGER.debug("Cache " + this.id + "; crit_write_req_for_item: " + msg.key + "; MSG_ID: " + msg.uuid + "; forward_to_parent: " + parent.path().name() + ";");
      if (this.nextCrash == CrashType.BEFORE_CRIT_WRITE_REQ_FW) {
        crashingOps();
      } else {
        if (this.type == CacheType.L2) {
          pendingReq.put(msg.uuid,
                  getContext().system().scheduler().scheduleOnce(
                          Duration.create(Config.TIMEOUT_CACHE_CRIT_WRITE, TimeUnit.MILLISECONDS),        // when to send the message
                          getSelf(),                                          // destination actor reference
                          new TimeoutReqMsg(msg),                                  // the message to send
                          getContext().system().dispatcher(),                 // system dispatcher
                          getSelf()                                           // source of the message (myself)
                  )); //adding the uuid of the message to the list of the pending ones*/
        }
        if (Config.VERBOSE_LOG)
          LOGGER.debug("Cache " + this.id + "; pending_req_list: " + pendingReq.keySet() + "; adding_req_id: " + msg.uuid + ";");
        sendMessage(msg, parent);
        if (this.nextCrash == CrashType.AFTER_CRIT_WRITE_REQ_FW) {
          crashingOps();
        }
      }
    }
  }

  /**
   * This method is used to handle the arrival of a {@link InvalidationItemMsg} message.
   * The item is added to the a list of invalid items, so this cache will not provide the old value to upcoming requests.
   * This message can come both from the DB (in both types of caches) or from a L1 (in a L2 cache).
   * If this cache is a L1 and has 0 or 1 child, it will send to the {@link DB database} a {@link InvalidationItemConfirmMsg confirmation}.
   * If this cache is a L1 with at least 2 children, it will forward the message to its children.
   * Otherwise (this cache is an L2) it will send to its parent a {@link InvalidationItemConfirmMsg confirmation} and a
   * timer is set to check for crashes before the arrival of the associated {@link CritRefillMsg critical refill}.
   * @param msg is the {@link InvalidationItemMsg} message which contains the key of the element to be invalidated.
   */
  private void onInvalidationItemMsg(InvalidationItemMsg msg){
    if(this.nextCrash==CrashType.BEFORE_ITEM_INVALIDATION){
      crashingOps();
    }else {
      this.invalidItems.add(msg.key);
      if (this.type.equals(CacheType.L1)) {
        LOGGER.debug("Cache " + this.id + "; invalidation_for_item: " + msg.key + "; MSG_ID: " + msg.uuid + "; invalidation_sent_to_children;");
        if(children.size() <= 1){
          LOGGER.debug("Cache " + this.id + "; invalidation_confirm_for_item: " + msg.key + "; MSG_ID: " + msg.uuid + "; not_need_to_wait_confirmation_from_the_children; sending_confirmation;");
          sendInvalidationConfirmation(new InvalidationItemConfirmMsg(msg.key, msg.uuid));
        }
        if(this.nextCrash==CrashType.DURING_INVALID_ITEM_MULTICAST){
          multicastAndCrash(msg);
        }else {
          multicast(msg);
        }
      } else if (this.type.equals(CacheType.L2)) {
        if(this.nextCrash==CrashType.BEFORE_ITEM_INVALID_CONFIRM_SEND){
          crashingOps();
        }else {
          pendingUpdates.put(msg.uuid,
                  getContext().system().scheduler().scheduleOnce(
                          Duration.create(Config.TIMEOUT_CACHE_INVALIDATION, TimeUnit.MILLISECONDS),        // when to send the message
                          getSelf(),                                          // destination actor reference
                          new TimeoutUpdateCWMsg(msg),                                  // the message to send
                          getContext().system().dispatcher(),                 // system dispatcher
                          getSelf()                                           // source of the message (myself)
                  )); //adding the uuid of the message to the list of the pending ones
          InvalidationItemConfirmMsg confirmMsg = new InvalidationItemConfirmMsg(msg.key, msg.uuid);
          LOGGER.debug("Cache " + this.id + "; invalidation_for_item: " + msg.key + "; MSG_ID: " + msg.uuid + "; invalidation_confirm_send_to " + this.parent.path().name() + ";");
          sendMessage(confirmMsg, this.parent);
        }
      }
    }
  }


  private void onTimeoutUpdateCWMsg(TimeoutUpdateCWMsg msg){
    LOGGER.debug("Cache " + this.id + "; timeout_while_waiting_crit_refill_for_item: " + msg.awaitedMsg.key + "; MSG_ID: " + msg.awaitedMsg.uuid + "; removing_item_from_memory;");
    this.invalidItems.remove(msg.awaitedMsg.key);
    this.savedItems.remove(msg.awaitedMsg.key);
  }

  /**
   * This method is used to handle the arrival of a {@link InvalidationItemConfirmMsg} message.
   * This method is triggered only in a L1 cache, that collects the {@link InvalidationItemConfirmMsg confirmations} from
   * its children.
   * When this cache collects children.size() - MAX_N_CACHE_CRASH {@link InvalidationItemConfirmMsg confirmations} it sends
   * a {@link InvalidationItemConfirmMsg confirmation} to the {@link DB Database}.
   * @param msg is the {@link InvalidationItemConfirmMsg} message which confirms that the sender has marked the item as invalid.
   */
  private void onInvalidationItemConfirmMsg(InvalidationItemConfirmMsg msg){
    if(this.nextCrash==CrashType.BEFORE_ITEM_INVALID_CONFIRM_RESP){
      crashingOps();
    }else {
      LOGGER.debug("Cache " + this.id + "; invalidation_confirm_for_item: " + msg.key + "; MSG_ID: " + msg.uuid + "; from_cache: " + getSender().path().name() + ";");
      if (this.invalidConfirmations.containsKey(msg.uuid)) {
        this.invalidConfirmations.get(msg.uuid).add(getSender());
      } else {
        this.invalidConfirmations.put(msg.uuid, new HashSet<>());
        this.invalidConfirmations.get(msg.uuid).add(getSender());
      }

      //I don't care if one confirmation does not arrive, because one L2 cache can crash but the protocol will work
      // When recovers it will update its value and be consistent.
      //we use == and not >= to avoid resending of the confirmation
      if (this.invalidConfirmations.get(msg.uuid).size() == (this.children.size() - Config.MAX_N_CACHE_CRASH)) {
        sendInvalidationConfirmation(msg);
      }
    }
  }

  /**
   * Service method to be called by both onInvalidationItemConfirmMsg and onInvalidationItemMsg to send a {@link InvalidationItemConfirmMsg}
   * message.
   * @param msg is the {@link InvalidationItemConfirmMsg} message to send.
   */
  private void sendInvalidationConfirmation(InvalidationItemConfirmMsg msg){
    if(this.nextCrash==CrashType.BEFORE_ITEM_INVALID_CONFIRM_SEND){
      crashingOps();
    }else {
      LOGGER.debug("Cache " + this.id + "; all_invalidation_confirm_received_for: " + msg.key + "; MSG_ID: " + msg.uuid + "; send_to: " + this.parent.path().name() + ";");
      sendMessage(msg, this.parent);
    }
  }


  /**
   * This method is used to handle the arrival of a {@link CritRefillMsg} message.
   * If the propagated item is in this cache memory, the value is updated. Then it will mark the element as valid.
   * If this cache is a L1, the message is forwarded to all its children.
   * Otherwise (the cache is L2) the method sends a {@link CritWriteConfirmMsg confirmation} to one of its child if it
   * is the originator of associated {@link CritWriteReqMsg request}.
   * The timer of the associated {@link InvalidationItemConfirmMsg invalidation confirm} is cancelled.
   * The timer of the associated {@link CritWriteReqMsg request} is cancelled.
   * @param msg the {@link CritRefillMsg} message which contains the key of the updated item and the new value.
   */
  private void onCritRefillMsg(CritRefillMsg msg) {
    if(this.nextCrash==CrashType.BEFORE_CRIT_REFILL){
      crashingOps();
    }else {
      Integer key = msg.key;
      if (savedItems.containsKey(key)) {
        LOGGER.debug("Cache " + this.id + "; crit_refill_for_item: " + msg.key + "; MSG_ID: " + msg.uuid + "; value: " + msg.newValue + ";");
        savedItems.put(key, msg.newValue);
      }
      if (this.invalidItems.contains(key)) {
        this.invalidItems.remove(key);
      }
      if (this.type == CacheType.L1) {
        if (Config.VERBOSE_LOG)
          LOGGER.debug("Cache " + this.id + "; pending_req_list: " + pendingReq.keySet() + "; remove_req_id: " + msg.uuid + ";");
        LOGGER.debug("Cache " + this.id + "; crit_refill_for_item: " + msg.key + "; MSG_ID: " + msg.uuid + "; forward_to_children;");
        pendingReq.remove(msg.uuid); //removing the uuid of the message from the list of the pending ones
        this.invalidConfirmations.remove(msg.uuid); //removing the uuid of the message from the list of the invalidConfirmation
        if(this.nextCrash==CrashType.DURING_CRIT_REFILL_MULTICAST){
          multicastAndCrash(msg);
        }else {
          multicast(msg);
        }
      } else if (this.type == CacheType.L2) {
        if (pendingUpdates.containsKey(msg.uuid)) {
          pendingUpdates.get(msg.uuid).cancel();
          pendingUpdates.remove(msg.uuid);
          if (Config.VERBOSE_LOG)
            LOGGER.debug("Cache " + this.id + "; pending_req_list: " + pendingReq.keySet() + "; remove_req_id: " + msg.uuid + ";");
        }
        if (pendingReq.containsKey(msg.uuid)) {
          if (Config.VERBOSE_LOG)
            LOGGER.debug("Cache " + this.id + "; pending_req_list: " + pendingReq.keySet() + "; remove_req_id: " + msg.uuid + ";");
          pendingReq.get(msg.uuid).cancel();
          pendingReq.remove(msg.uuid); //removing the uuid of the message from the list of the pending ones
        }
        ActorRef originator = msg.originator;
        if (children.contains(originator)) {
          if(this.nextCrash==CrashType.BEFORE_CRIT_WRITE_CONFIRM){
            crashingOps();
          }else {
            CritWriteConfirmMsg resp = new CritWriteConfirmMsg(msg.key, msg.uuid);
            LOGGER.debug("Cache " + this.id + "; crit_write_ack_for_item: " + msg.key + "; MSG_ID: " + msg.uuid + "; forward_to: " + msg.originator.path().name() + "; timeout_cancelled;");
            sendMessage(resp, originator);
          }
        }
      }
    }
  }

  /**
   * This method is used to handle the arrival of a {@link CritWriteErrorMsg} message.
   * The item is marked as valid.
   * If this cache is a L1, the message is forwarded to all its children.
   * Otherwise (the cache is L2) the method sends the message to one of its child if it
   * is the originator of associated {@link CritWriteReqMsg request}.
   * The timer of the associated {@link InvalidationItemConfirmMsg invalidation confirm} is cancelled.
   * The timer of the associated {@link CritWriteReqMsg request} is cancelled.
   * @param msg the {@link CritWriteErrorMsg} message which contains the key of the updated item and the new value.
   */
  private void onCritWriteErrorMsg(CritWriteErrorMsg msg){
    Integer key = msg.key;
    if(this.invalidItems.contains(key)){
      this.invalidItems.remove(key);
      LOGGER.debug("Cache " + this.id + "; item: " + key + "; now_valid; MSG_ID: " + msg.uuid + ";");
    }
    if(this.type == CacheType.L1){
      if(Config.VERBOSE_LOG)
        LOGGER.debug("Cache " + this.id + "; pending_req_list: " + pendingReq.keySet() + "; remove_req_id: " + msg.uuid + ";");
      LOGGER.error("Cache " + this.id + "; crit_write_failed_for_item: " + msg.key + "; MSG_ID: " + msg.uuid + "; forward_to_children;");
      pendingReq.remove(msg.uuid); //removing the uuid of the message from the list of the pending ones
      this.invalidConfirmations.remove(msg.uuid); //removing the uuid of the message from the list of the invalidConfirmation
      if(this.nextCrash==CrashType.DURING_CRIT_WRITE_ERROR_MULTICAST){
        multicastAndCrash(msg);
      }else {
        multicast(msg);
      }
    }else if(this.type == CacheType.L2){
      if(pendingUpdates.containsKey(msg.uuid)){
        pendingUpdates.get(msg.uuid).cancel();
        pendingUpdates.remove(msg.uuid);
        if(Config.VERBOSE_LOG)
          LOGGER.debug("Cache " + this.id + "; pending_req_list: " + pendingReq.keySet() + "; remove_req_id: " + msg.uuid + ";");
      }
      if (pendingReq.containsKey(msg.uuid)) {
        if(Config.VERBOSE_LOG)
          LOGGER.debug("Cache " + this.id + "; pending_req_list: " + pendingReq.keySet() + "; remove_req_id: " + msg.uuid + ";");
        pendingReq.get(msg.uuid).cancel();
        pendingReq.remove(msg.uuid); //removing the uuid of the message from the list of the pending ones
      }
      ActorRef originator = msg.originator;
      if(children.contains(originator)) {
        LOGGER.error("Cache " + this.id + "; crit_write_failed_for_item: " + msg.key + "; MSG_ID: " + msg.uuid + "; forward_to: " + msg.originator.path().name() + "; timeout_cancelled;");
        sendMessage(msg, originator);
      }
    }
  }

  /* -- END OF write and read message methods ----------------------------------------------------- */



  /* -- START OF crash handling message methods --------------------------------------------------------- */

  /**
   * This method is used to handle the arrival of a {@link TimeoutReqMsg} message.
   * This is triggered in a L2 cache when it detects the crash of its parent while waiting for some response.
   * The cache detecting the crash will set the {@link DB database} as its new parent and will notify the database of the change.
   * Also, it will start refreshing the value of its saved items, because it could have old values.
   * The cache will also notify the {@link Client originator} that its request has failed.
   * @param msg the {@link TimeoutReqMsg} message which contains a copy of the request that has failed.
   */
  private void onTimeoutReqMsg(TimeoutReqMsg msg) {
    if (pendingReq.containsKey(msg.awaitedMsg.uuid)){
      if(msg.awaitedMsg instanceof RefreshItemReqMsg){
        LOGGER.warn("Cache " + this.id + "; timeout_while_refresh_item: " + msg.awaitedMsg.key + "; MSG_ID: " + msg.awaitedMsg.uuid);
      }else{
        LOGGER.warn("Cache " + this.id + "; timeout_while_await_item: " + msg.awaitedMsg.key + "; MSG_ID: " + msg.awaitedMsg.uuid);
      }

      this.parent=this.db;
      LOGGER.debug("Cache " + this.id + "; new_parent_selected: " + this.parent.path().name() + ";");

      pendingReq.remove(msg.awaitedMsg.uuid);

      ReqErrorMsg errMsg=new ReqErrorMsg(msg.awaitedMsg);
      if(msg.awaitedMsg instanceof CritReadReqMsg){
        ActorRef dest = ((CritReadReqMsg) msg.awaitedMsg).responsePath.pop();

        if(dest.equals(getSelf())) // the response stack of the failed request contain on the top the cache itself so we need to pop 2 times in order to get the reference of the next hop
          dest = ((CritReadReqMsg) msg.awaitedMsg).responsePath.pop();

        LOGGER.debug("Cache " + this.id + "; sending_crit_read_error_message_to: " + dest.path().name() + "; MSG_ID: " + msg.awaitedMsg.uuid + ";");
        sendMessage(errMsg, dest);
      }else if(msg.awaitedMsg instanceof CritWriteReqMsg){
        ActorRef originator = ((CritWriteReqMsg) msg.awaitedMsg).originator;
        LOGGER.debug("Cache " + this.id + "; sending_crit_write_error_message_to: " + originator.path().name() + "; MSG_ID: " + msg.awaitedMsg.uuid + ";");
        if(this.pendingUpdates.containsKey(msg.awaitedMsg.uuid)){
          this.pendingUpdates.get(msg.awaitedMsg.uuid).cancel();
          this.pendingUpdates.remove(msg.awaitedMsg.uuid);
        }
        this.invalidConfirmations.remove(msg.awaitedMsg.uuid);

        sendMessage(errMsg, originator);
      }else if(msg.awaitedMsg instanceof ReadReqMsg){
        ActorRef dest = ((ReadReqMsg) msg.awaitedMsg).responsePath.pop();

        if(dest.equals(getSelf())) // the response stack of the failed request contain on the top the cache itself so we need to pop 2 times in order to get the reference of the next hop
          dest = ((ReadReqMsg) msg.awaitedMsg).responsePath.pop();

        LOGGER.debug("Cache " + this.id + "; sending_read_error_message_to: " + dest.path().name() + "; MSG_ID: " + msg.awaitedMsg.uuid + ";");
        sendMessage(errMsg, dest);
      }else if(msg.awaitedMsg instanceof WriteReqMsg){
        LOGGER.debug("Cache " + this.id + "; sending_write_error_message_to: " + ((WriteReqMsg) msg.awaitedMsg).originator.path().name() + "; MSG_ID: " + msg.awaitedMsg.uuid + ";");
        sendMessage(errMsg, ((WriteReqMsg) msg.awaitedMsg).originator);
      }

      AddChildMsg addMeMsg=new AddChildMsg(getSelf());
      sendMessage(addMeMsg, this.parent);
      if(savedItems.size() > 0)
        refreshItems();
    }else{
      LOGGER.debug("Cache " + this.id + "; timeout_but_received_response for: " + msg.awaitedMsg.key + "; MSG_ID: " + msg.awaitedMsg.uuid + ";");
    }
  }


  /**
   * This method is used used to refresh the saved items of the cache.
   * This is triggered in a L2 cache when it set the {@link DB database} as parent or when its L1 parent recovers before
   * the this cache has noticed the crash.
   */
  private void refreshItems(){
    LOGGER.debug("Cache " + this.id + "; refreshing_cache_using_parent: " + this.parent.path().name() + ";");
    for(int i : savedItems.keySet()){
      LOGGER.debug("Cache " + this.id + "; send_refresh_req_for_item: " + i + ";");
      RefreshItemReqMsg refreshReq = new RefreshItemReqMsg(i);
      refreshReq.responsePath.push(getSelf());
      pendingReq.put(refreshReq.uuid,
              getContext().system().scheduler().scheduleOnce(
                      Duration.create(Config.TIMEOUT_CACHE, TimeUnit.MILLISECONDS),        // when to send the message
                      getSelf(),                                          // destination actor reference
                      new TimeoutReqMsg(refreshReq),                                  // the message to send
                      getContext().system().dispatcher(),                 // system dispatcher
                      getSelf()                                           // source of the message (myself)
              )); //adding the uuid of the message to the list of the pending ones*/
      sendMessage(refreshReq, this.parent);
    }
  }

  /**
   * This method is used to handle the arrival of a {@link RefreshItemReqMsg} message.
   * This is triggered in a L1 cache when a child needs to request new values for its item.
   * The message is forwarded to the {@link DB database} because we recover the last values.
   * @param msg the {@link RefreshItemReqMsg} message contains the key of the element to be read from the database.
   */
  private void onRefreshItemReqMsg(RefreshItemReqMsg msg){
    if(this.nextCrash==CrashType.DURING_REFRESH){
      crashingOps();
    }else {
      LOGGER.debug("Cache " + this.id + "; forwarding_refresh_req_for_item: " + msg.key + ";");
      msg.responsePath.push(getSelf());
      sendMessage(msg, this.parent);
    }
  }

  /**
   * This method is used to handle the arrival of a {@link RefreshItemRespMsg} message.
   * This message can come both from the DB or from a L1 (in a L2 cache).
   * This cache stores the value in its memory. If this cache is an L1, it will forward the message to the child that is the
   * originator of associated {@link RefreshItemReqMsg request}.
   * The timer of the associated {@link RefreshItemReqMsg request} is cancelled.
   * @param msg the {@link RefreshItemRespMsg} message which contains value of the requested item.
   */
  private void onRefreshItemRespMsg(RefreshItemRespMsg msg) {
    LOGGER.debug("Cache " + this.id + "; refreshing_item_in_cache: " + msg.key + "; setting_value: " + msg.value + "; refresh_completed;");
    if(this.type==CacheType.L2){
      if(pendingReq.containsKey(msg.uuid)){
        pendingReq.get(msg.uuid).cancel();
        pendingReq.remove(msg.uuid);
      }
    }
    savedItems.put(msg.key, msg.value);
    this.invalidItems.remove(msg.key); //the line does something only in L2 cache
    if (!msg.responsePath.isEmpty()) {
      ActorRef nextHop = msg.responsePath.pop();
      sendMessage(msg, nextHop);
    }
  }

  /**
   * This method is used to handle the arrival of a {@link RecoveryMsg} message.
   * The next scheduled crash is sent to NONE and will ask its children if they are still children.
   * @param msg the {@link RecoveryMsg} message which is used to recover the crashed cache.
   */
  private void onRecoveryMsg(RecoveryMsg msg) {
    LOGGER.debug("Cache " + this.id + "; recovers;");
    multicast(new IsStillParentReqMsg());
    this.nextCrash = CrashType.NONE;
    this.afterNMessageSent = Integer.MAX_VALUE;
    this.recoveryAfter=-1;
    getContext().become(createReceive());
  }

  /**
   * This method is used to handle the arrival of a {@link IsStillParentReqMsg} message.
   * This method is triggered in a L2 cache.
   * If the sender (an L1 cache) is still its parent after recovery, this cache will respond affirmatively.
   * @param msg the {@link IsStillParentReqMsg} message used to ask if a L2 cache is still the child of a L1 cache.
   */
  private void onIsStillParentReqMsg(IsStillParentReqMsg msg) {
    ActorRef sender = getSender();
    boolean response;
    if(sender.equals(this.parent)){
      response=true;
    }else{
      response=false;
    }
    sendMessage(new IsStillParentRespMsg(response, msg.uuid), sender);
  }

  /**
   * This method is used to handle the arrival of a {@link IsStillParentRespMsg} message.
   * When this method is triggered in a L2 cache, the message is coming from a {@link Client}. It is removed (or not) from
   * the list of children according to response.
   * Otherwise (the cache is a L1) the message is coming from a L2. The L2 is removed (or not) from the list of children
   * according to response. Plus, if L2 is still child of the L1, the L1 tells the L2 with a {@link StartRefreshMsg message}
   * to refresh its items.
   * @param msg the {@link IsStillParentRespMsg} message which contains the answer to the corrisponding {@link IsStillParentReqMsg request}.
   */
  private void onIsStillParentRespMsg(IsStillParentRespMsg msg){
    if (!msg.response){
      LOGGER.debug("Cache " + this.id + "; is_not_parent_of: " + getSender().path().name() + ";");
      children.remove(getSender());
    }else{
      if(this.type==CacheType.L1){
        StartRefreshMsg refreshMsg = new StartRefreshMsg();
        sendMessage(refreshMsg, getSender());
      }
    }
  }

  /**
   * This method is used to handle the arrival of a {@link StartRefreshMsg} message.
   * This method is triggered in a L2 cache. It starts to update the value of its saved items with the last value present
   * in the system. The timers of all the pending requests are cancelled. Also, it notifies its children to cancel all
   * the ongoing timers, to avoid the triggering of timeouts in clients.
   * @param msg the {@link StartRefreshMsg} message that notifies the cache to start refreshing its items.
   */
  private void onStartRefreshMsg(StartRefreshMsg msg){
    pendingReq.values().forEach(Cancellable::cancel);
    CancelTimeoutMsg cancelTimeoutMsg = new CancelTimeoutMsg(pendingReq.keySet());
    if(this.nextCrash==CrashType.DURING_CANCEL_TIMEOUT_MULTICAST){
      multicastAndCrash(cancelTimeoutMsg);
    }else{
      multicast(cancelTimeoutMsg);
      pendingReq.clear();
      if(savedItems.size() > 0){
        LOGGER.debug("Cache " + this.id + "; start_refreshing_items" + ";");
        refreshItems();
      }
    }
  }

  /**
   * This method is used handle the arrival of a {@link CrashMsg} message.
   * It schedules the next crash.
   * @param msg is the {@link CrashMsg} message which is sent by the main class to make this cache to crash.
   */
  private void onCrashMsg(CrashMsg msg){
    if(this.nextCrash == CrashType.NONE) {
      this.nextCrash = msg.type;
      this.recoveryAfter = msg.delay;
      if (this.nextCrash == CrashType.NOW) {
        crashingOps();
      }
    }
  }

  /**
   * This method is used handle the arrival of a {@link CrashDuringMulticastMsg} message.
   * It schedules the next crash that will be during a multicast.
   * @param msg is the {@link CrashDuringMulticastMsg} message which is sent by the main class to make this cache to crash.
   */
  private void onCrashDuringMulticastMsg(CrashDuringMulticastMsg msg){
    if(this.nextCrash == CrashType.NONE) {
      this.nextCrash = msg.type;
      this.afterNMessageSent = msg.afterNMessage;
      this.recoveryAfter = msg.delay;
    }
  }

  /**
   * This method is used to perform all the operations related to a crash.
   * It will clear all the instance data structures and cancel all the timers.
   * Also it schedules the recovery of this cache.
   */
  private void crashingOps(){
    LOGGER.debug("Cache " + this.id + "; is_now_crashed_for: " + this.recoveryAfter + " ms; ");
    pendingReq.values().forEach(Cancellable::cancel);
    pendingUpdates.values().forEach(Cancellable::cancel);
    pendingUpdates.clear();
    invalidItems.clear();
    invalidConfirmations.clear();
    pendingReq.clear();
    savedItems.clear();
    getContext().become(crashed());
    getContext().system().scheduler().scheduleOnce(
            Duration.create(this.recoveryAfter, TimeUnit.MILLISECONDS),        // when to send the message
            getSelf(),                                          // destination actor reference
            new RecoveryMsg(),                                  // the message to send
            getContext().system().dispatcher(),                 // system dispatcher
            getSelf()
    );                                    // source of the message (myself)
  }

  /* -- END OF crash handling message methods --------------------------------------------------------- */



  /* -- BEGIN OF debug methods --------------------------------------------------------- */

  /**
   * This method is triggered when a {@link InternalStateMsg} is received from the {@link EasyCache.ProjectRunner runner}.
   * This method is used for debugging. It will print the current state of the cache: the saved item, the list of children and the parent
   * @param msg is the {@link InternalStateMsg} message, is an empty message used to print the internal state of the cache.
   */
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
    sb.append("]; Parent: " + parent.path().name() + "; Pending request: " + pendingReq.keySet());
    LOGGER.debug(sb);
  }

  private void onSupportMsg(SupportMsg msg){
    this.nextCrash = CrashType.NONE;
  }

  /* -- END OF debug methods --------------------------------------------------------- */


  /**
   * The mapping between the received message types and our actor methods in the normal behaviour.
   */
  @Override
  public Receive createReceive() {
    return receiveBuilder()
            .match(SetChildrenMsg.class, this::onSetChildrenMsg)
            .match(AddChildMsg.class, this::onAddChildMsg)
            .match(SetParentMsg.class, this::onSetParentMsg)
            .match(CritReadReqMsg.class, this::onCritReadReqMsg)
            .match(CritReadRespMsg.class, this::onCritReadRespMsg)
            .match(CritWriteReqMsg.class, this::onCritWriteReqMsg)
            .match(CritRefillMsg.class, this::onCritRefillMsg)
            .match(RefreshItemReqMsg.class, this::onRefreshItemReqMsg)
            .match(RefreshItemRespMsg.class, this::onRefreshItemRespMsg)
            .match(ReadReqMsg.class, this::onReadReqMsg)
            .match(ReadRespMsg.class, this::onReadRespMsg)
            .match(InvalidationItemMsg.class, this::onInvalidationItemMsg)
            .match(InvalidationItemConfirmMsg.class, this::onInvalidationItemConfirmMsg)
            .match(WriteReqMsg.class, this::onWriteReqMsg)
            .match(RefillMsg.class, this::onRefillMsg)
            .match(InternalStateMsg.class, this::onInternalStateMsg)
            .match(IsStillParentReqMsg.class, this::onIsStillParentReqMsg)
            .match(IsStillParentRespMsg.class, this::onIsStillParentRespMsg)
            .match(CrashDuringMulticastMsg.class, this::onCrashDuringMulticastMsg)
            .match(CrashMsg.class, this::onCrashMsg)
            .match(TimeoutReqMsg.class, this::onTimeoutReqMsg)
            .match(StartRefreshMsg.class, this::onStartRefreshMsg)
            .match(TimeoutUpdateCWMsg.class, this::onTimeoutUpdateCWMsg)
            .match(CritWriteErrorMsg.class, this::onCritWriteErrorMsg)
            .match(SupportMsg.class, this::onSupportMsg)
            .build();
  }

  /**
   * The mapping between the received message types and our actor methods in the crashed behaviour.
   */
  final AbstractActor.Receive crashed() {
    return receiveBuilder()
            .match(RecoveryMsg.class, this::onRecoveryMsg)
            .matchAny(msg -> LOGGER.debug(getSelf().path().name() + " ignoring " + msg.getClass().getSimpleName() + " (crashed)"))
            .build();
  }
}
