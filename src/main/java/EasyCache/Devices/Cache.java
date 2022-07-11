package EasyCache.Devices;

import EasyCache.Config;
import EasyCache.Messages.*;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import EasyCache.CacheType;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import scala.concurrent.duration.Duration;

public class Cache extends AbstractActor {

  private Random rnd = new Random(); // Used to generate random value
  private ActorRef parent; // reference to the parent, may be a L1 Cache or the DB

  private ActorRef db;
  private List<ActorRef> children; // the list of children, they can be or a list of L2 caches or a list of Clients
  private final int id; // ID of the current actor
  private final CacheType type; // type of the current cache, may be L1 or L2
  private final Map<UUID, Cancellable> pendingReq; // map of all the pending request which are still waiting for a response, with the corresponding timer

  private HashMap<Integer, Integer> savedItems; // the items saved in the cache

  private static final Logger LOGGER = LogManager.getLogger(Cache.class);
  /* -- Actor constructor --------------------------------------------------- */

  /**
   * Constructor of the Cache actor.
   * @param id represents the ID of the current actor.
   * @param type represents the type of the current cache, may be L1 or L2.
   * @param db represents the reference to the DB actor.
   */
  public Cache(int id, CacheType type, ActorRef db) {
    this.id = id;
    this.type=type;
    this.db=db;
    this.savedItems=new HashMap<>();
    this.pendingReq= new HashMap<>();
  }

  static public Props props(int id, CacheType type, ActorRef db) {
    return Props.create(Cache.class, () -> new Cache(id, type, db));
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
      sendMessage(m, p);
    }
  }

  /* -- END of Sending message methods ----------------------------------------------------- */



  /* -- START OF configuration message methods ----------------------------------------------------- */

  /**
   * This method is used to set the children of the cache. Is triggered by a SetChildrenMsg message.
   * @param msg is the SetChildrenMsg message which contain the list of children of the cache.
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
   * This method is used to add a new children to the cache.
   * This usually happen when a client detect the crash of its L2 cache and choose a new L2 cache.
   * @param msg is the AddChildMsg message which contains the new children to add to the list of the children of the cache.
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
   * This method is used to set the parent of the cache. Is triggered by a SetParentMsg message.
   * @param msg is the SetParentMsg message which contains the reference to the parent of the cache.
   */
  private void onSetParentMsg(SetParentMsg msg) {
    this.parent = msg.parent;
    LOGGER.debug("Cache " + this.id + "; parent_set_to: " + msg.parent.path().name() + ";");
  }

  /* -- END of configuration message methods ----------------------------------------------------- */



  /* -- START OF write and read message methods ----------------------------------------------------- */

  /**
   * This method is used to handle the ReadReqMsg message that represent the read request message.
   * This message can come both by a L2 cache or from a Client.
   * If the requested element is in the cache, the value associated to the key is returned to the requester.
   * Else the message is forwarded to the parent, that could be and L1 (in case is an L2 cache) or the DB.
   * The message also contains a stack with the path followed by the message. This is used to understand where to send the response.
   * If the cache does not have the requested element, the message is forwarded to the parent and a timeout is started.
   * If the timeout is reached the cache assume the crash of its parent and a TimeoutMsg is sent to the cache itself in order to trigger the crash protocol.
   * @param msg is the ReadReqMsg message which contains the key of the element to be read.
   */
  private void onReadReqMsg(ReadReqMsg msg){
    if(savedItems.containsKey(msg.key)){
      ActorRef nextHop=msg.responsePath.pop();
      Integer key = msg.key;
      ReadRespMsg resp = new ReadRespMsg(key, this.savedItems.get(key), msg.responsePath, msg.uuid);
      LOGGER.debug("Cache " + this.id + "; read_req_for_item: " + msg.key + "; cached_value: " + this.savedItems.get(msg.key) + "; MSG_ID: " + msg.uuid + ";");
      sendMessage(resp, nextHop);
    }else{
      LOGGER.debug("Cache " + this.id + "; read_req_for_item: " + msg.key + "; forward_to_parent: " + parent.path().name() + "; MSG_ID: " + msg.uuid + ";");
      pendingReq.put(msg.uuid,
              getContext().system().scheduler().scheduleOnce(
                      Duration.create(Config.TIMEOUT_CACHE, TimeUnit.MILLISECONDS),        // when to send the message
                      getSelf(),                                          // destination actor reference
                      new TimeoutMsg(msg),                                  // the message to send
                      getContext().system().dispatcher(),                 // system dispatcher
                      getSelf()                                           // source of the message (myself)
              )); //adding the uuid of the message to the list of the pending ones
      msg.responsePath.push(getSelf());
      if(Config.VERBOSE_LOG)
        LOGGER.debug("Cache " + this.id + "; pending_req_list: " + pendingReq.keySet() + "; adding_req_id: " + msg.uuid + ";");
      sendMessage(msg, parent);
    }
  }

  /**
   * This method is used to handle the ReadRespMsg message which represent the read response message.
   * After receiving a read response the cache needs to store the value in its memory and then will forward it to its children.
   * Also, the timer connected to the request is cancelled and the request is removed from the list of the pending ones.
   * @param msg is the ReadRespMsg message which contains value of the requested item.
   */
  private void onReadRespMsg(ReadRespMsg msg) {
    Integer key = msg.key;
    savedItems.put(key, msg.value);
    ActorRef nextHop = msg.responsePath.pop();
    LOGGER.debug("Cache " + this.id + "; read_resp_for_item = " + msg.key + "; forward_to " + nextHop.path().name() + "; MSG_id: " + msg.uuid + "; timeout_cancelled;");
    pendingReq.get(msg.uuid).cancel();
    pendingReq.remove(msg.uuid); //removing the uuid of the message from the list of the pending ones
    if(Config.VERBOSE_LOG)
      LOGGER.debug("Cache " + this.id + "; pending_req_list: " + pendingReq.keySet() + "; remove_req_id: " + msg.uuid + ";");
    sendMessage(msg, nextHop);
  }

  /**
   * This method is used to handle the WriteReqMsg message which represent the write request message.
   * A cache can only forward the request to its parent till it reach the DB where the write will be applied.
   * A timeout is also started.
   * If the timeout is reached the cache assume the crash of its parent and a TimeoutMsg is sent to the cache itself in order to trigger the crash protocol.
   * @param msg is the WriteReqMsg message which contains the key of the element to be written and the value to be written.
   */
  private void onWriteReqMsg(WriteReqMsg msg){
    LOGGER.debug("Cache " + this.id + "; write_req_for_item: " + msg.key + "; forward_to_parent: " + parent.path().name() + "; MSG_id: " + msg.uuid + ";");
    pendingReq.put(msg.uuid,
            getContext().system().scheduler().scheduleOnce(
                    Duration.create(Config.TIMEOUT_CACHE, TimeUnit.MILLISECONDS),        // when to send the message
                    getSelf(),                                          // destination actor reference
                    new TimeoutMsg(msg),                                  // the message to send
                    getContext().system().dispatcher(),                 // system dispatcher
                    getSelf()                                           // source of the message (myself)
            )); //adding the uuid of the message to the list of the pending ones
    if(Config.VERBOSE_LOG)
      LOGGER.debug("Cache " + this.id + "; pending_req_list: " + pendingReq.keySet() + "; adding_req_id: " + msg.uuid + ";");
    sendMessage(msg, parent);
  }

  /**
   * This method is used to handle the RefillMsg message. The RefillMsg message represent the ack of a write request.
   * The cache will first check if the value is stored in its memory, in that case it will update it.
   * Then, if the cache is a L1 cache, it will simply forward the message to all its children.
   * If the cache is a L2 cache, it will check if the originator of the request is one of its children.
   * If yes the L2 cache will send the confirmation of the write operation to the client.
   * The request is also removed from the list of the pending ones and the timer associated to the request is cancelled.
   * @param msg is the RefillMsg message which contains the key of the updated item and the new value.
   */
  private void onRefillMsg(RefillMsg msg) {
    Integer key = msg.key;
    if(savedItems.containsKey(key)){
      LOGGER.debug("Cache " + this.id + "; refill_for_item: " + msg.key + "; value: " + msg.newValue + "; MSG_id: " + msg.uuid + ";");
      savedItems.put(key, msg.newValue);
    }

    if(this.type == CacheType.L1){
      if(Config.VERBOSE_LOG)
        LOGGER.debug("Cache " + this.id + "; pending_req_list: " + pendingReq.keySet() + "; remove_req_id: " + msg.uuid + ";");
      pendingReq.remove(msg.uuid); //removing the uuid of the message from the list of the pending ones
      multicast(msg);
    }else if(this.type == CacheType.L2){
      if(Config.VERBOSE_LOG)
        LOGGER.debug("Cache " + this.id + "; pending_req_list: " + pendingReq.keySet() + "; remove_req_id: " + msg.uuid + ";");
      if (pendingReq.containsKey(msg.uuid)) {
        pendingReq.get(msg.uuid).cancel();
        pendingReq.remove(msg.uuid); //removing the uuid of the message from the list of the pending ones
      }
      ActorRef originator = msg.originator;
      if(children.contains(originator)) {
        WriteConfirmMsg resp = new WriteConfirmMsg(msg.key, msg.uuid);
        LOGGER.debug("Cache " + this.id + "; write_ack_for_item: " + msg.key + "; forward_to: " + msg.originator.path().name() + "; MSG_id: " + msg.uuid + "; timeout_cancelled;");
        sendMessage(resp, originator);
      }
    }
  }

  /**
   * This method is used to handle the CritReadReqMsg message that represent the critical read request message.
   * This message can come both by a L2 cache or from a Client and will always be forwarded to the parent.
   * The response will follow the ResponsePath stack which will contains all the hop that the message has passed.
   * A timeout is also started.
   * If the timeout is reached the cache assume the crash of its parent and a TimeoutMsg is sent to the cache itself in order to trigger the crash protocol.
   * @param msg is the CritReadReqMsg message which contains the key of the element to be read from the database.
   */
  private void onCritReadReqMsg(CritReadReqMsg msg){
    msg.responsePath.push(getSelf());
    LOGGER.debug("Cache " + this.id + "; crit_read_req_for_item: " + msg.key + "; forward_to_parent: " + parent.path().name() + "; MSG_ID: " + msg.uuid + ";");
    pendingReq.put(msg.uuid,
            getContext().system().scheduler().scheduleOnce(
                    Duration.create(Config.TIMEOUT_CACHE, TimeUnit.MILLISECONDS),        // when to send the message
                    getSelf(),                                          // destination actor reference
                    new TimeoutMsg(msg),                                  // the message to send
                    getContext().system().dispatcher(),                 // system dispatcher
                    getSelf()                                           // source of the message (myself)
            )); //adding the uuid of the message to the list of the pending ones
    if(Config.VERBOSE_LOG)
      LOGGER.debug("Cache " + this.id + "; pending_req_list: " + pendingReq.keySet() + "; adding_req_id: " + msg.uuid + ";");
    sendMessage(msg, parent);
  }

  /**
   * This method is used to handle the CritReadRespMsg message which represent the critical read response message.
   * After receiving a critical read response the cache will also store or update the value in its memory and then will forward it to its children.
   * Also, the timer connected to the request is cancelled and the request is removed from the list of the pending ones.
   * @param msg is the CritReadRespMsg message which contains value of the requested item.
   */
  private void onCritReadRespMsg(CritReadRespMsg msg) {
    Integer key = msg.key;
    savedItems.put(key, msg.value);
    ActorRef nextHop = msg.responsePath.pop();
    LOGGER.debug("Cache " + this.id + "; crit_read_resp_for_item = " + msg.key + "; forward_to " + nextHop.path().name() + "; MSG_id: " + msg.uuid + "; timeout_cancelled;");
    pendingReq.get(msg.uuid).cancel();
    pendingReq.remove(msg.uuid); //removing the uuid of the message from the list of the pending ones
    if(Config.VERBOSE_LOG)
     LOGGER.debug("Cache " + this.id + "; pending_req_list: " + pendingReq.keySet() + "; remove_req_id: " + msg.uuid + ";");
    sendMessage(msg, nextHop);
  }

  /* -- END OF write and read message methods ----------------------------------------------------- */



  /* -- START OF crash handling message methods --------------------------------------------------------- */

  /**
   * This method is used to handle the TimeoutMsg message which represent the timeout of a message and is used to detect a crash.
   * A timeout message is sent after that a response to a request is not received in a given time. This message contains a copy of the request message.
   * Only an L2 cache can go in timeout as we assume that the database cannot crash.
   * If a L2 cache detect the crash of its L1 cache it will set the database as its new parent and will notify the databsase of the change asking to add the cache to the list of its child.
   * Then it will remove the request to the list of the pending one and will notify, throw an ReqErrorMsg to the originator of the request that the request has failed.
   * @param msg is the TimeoutMsg message which contains a copy of the request that has failed.
   */
  private void onTimeoutMsg(TimeoutMsg msg) {
    if (pendingReq.containsKey(msg.awaitedMsg.uuid)){
      LOGGER.debug("Cache " + this.id + "; timeout_while_await: " + msg.awaitedMsg.key + " msg_id: " + msg.awaitedMsg.uuid);
      this.parent=this.db;
      LOGGER.debug("Cache " + this.id + "; new_parent_selected: " + this.parent.path().name() + "; refreshing_the_cache;");

      refreshItems();

      AddChildMsg addMeMsg=new AddChildMsg(getSelf());
      sendMessage(addMeMsg, this.parent);
      pendingReq.remove(msg.awaitedMsg.uuid);

      ReqErrorMsg errMsg=new ReqErrorMsg(msg.awaitedMsg);

      if(msg.awaitedMsg instanceof ReadReqMsg){
        ActorRef dest = ((ReadReqMsg) msg.awaitedMsg).responsePath.pop();
        // POSSIBLE REFACTOR TO REMOVE THE FACT THAT THE AWAITED REQUEST IS PASSED BY REFERENCE CAUSING THE RESPONSE PATH TO HAVE THE L2 CACHE ON TOP OF THE STACK
        if(dest.equals(getSelf())) // the response stack of the failed request contain on the top the cache itself so we need to pop 2 times in order to get the reference of the next hop
          dest = ((ReadReqMsg) msg.awaitedMsg).responsePath.pop();

        LOGGER.debug("Cache " + this.id + "; sending_read_error_message_to: " + dest.path().name() + "; for_MSG_id: " + msg.awaitedMsg.uuid + ";");
        sendMessage(errMsg, dest);
      }else if(msg.awaitedMsg instanceof WriteReqMsg){
        LOGGER.debug("Cache " + this.id + "; sending_write_error_message_to: " + ((WriteReqMsg) msg.awaitedMsg).originator.path().name() + "; for_MSG_id: " + msg.awaitedMsg.uuid + ";");
        sendMessage(errMsg, ((WriteReqMsg) msg.awaitedMsg).originator);
      }
    }else{
      LOGGER.debug("Client " + this.id + "; timeout_but_received_response for: " + msg.awaitedMsg.key + ";");
    }
  }

  /**
   * This method is used to refresh the saved items of the cache.
   * This may be needed when the cache detect the crash of its L1 parent cache or when the L1 parent cache recover from a crash.
   */
  private void refreshItems(){
    LOGGER.debug("Cache " + this.id + "; refreshing_cache_using_parent: " + this.parent.path().name() + ";");
    for(int i : savedItems.keySet()){
      LOGGER.debug("Cache " + this.id + "; send_refresh_req_for_item: " + i + ";");
      RefreshItemReqMsg refreshReq = new RefreshItemReqMsg(i);
      refreshReq.responsePath.push(getSelf());
      sendMessage(refreshReq, this.parent);
    }
  }

  /**
   * This method is used to handle the RefreshItemReqMsg message which represent the request to refresh an item in the cache.
   * This message is sent from a L2 cache to a L1 cache when the latter recover from the crash.
   * After the recovery, the L1 cache will lose all its saved items and will check which L2 cache is still its children.
   * To this cache the L1 cache will ask to refresh the memory in order to ensure that they will have the latest value.
   * In this way the L1 cache will be able also to restore its memory.
   * @param msg
   */
  private void onRefreshItemReqMsg(RefreshItemReqMsg msg){
    LOGGER.debug("Cache " + this.id + "; forwarding_refresh_req_for_item: " + msg.key + ";");
    msg.responsePath.push(getSelf());
    sendMessage(msg, this.parent);
  }

  /**
   * This method is used to handle the RefreshItemRespMsg message which represent the response to a request to refresh an item in the cache.
   * This message may arrive from the DB to a L1 or L2 cache or from a L1 cache to a L2 cache.
   * In the first case the L1 cache will update its memory with the value of the item and then will forward the message to the next hop which is one of its children.
   * In the second case the L2 cache will update its memory with the value received but there won't be any next hop to forward the message.
   * @param msg
   */
  private void onRefreshItemRespMsg(RefreshItemRespMsg msg) {
    LOGGER.debug("Cache " + this.id + "; refreshing_item_in_cache: " + msg.key + "; setting_value: " + msg.value + "; refresh_completed;");
    savedItems.put(msg.key, msg.value);
    if (!msg.responsePath.isEmpty()) {
      ActorRef nextHop = msg.responsePath.pop();
      sendMessage(msg, nextHop);
    }
  }

  /**
   * This method is used to handle the RecoveryMsg message which is send by the main class of the program.
   * After recovering the Cache will remove all its cached item and all its pending requests with the associated timer.
   * Then it will check if the children are still its children, this is done by sending a IsStillParentReqMsg to each of its children.
   * The behaviour of the cache is also restored to the normal behaviour.
   * @param msg is the RecoveryMsg which is used to recover the crashed cache.
   */
  private void onRecoveryMsg(RecoveryMsg msg) {
    LOGGER.debug("Cache " + this.id + "; recovers;");
    for(ActorRef child: children){
      sendMessage(new IsStillParentReqMsg(), child);
    }
    getContext().become(createReceive());
  }

  /**
   * This method is used to handle the IsStillParentReqMsg message which is received by a L1 cache.
   * After that a L1 cache recover it needs to understand if its children are still its children or if they have detected its crash and changed their parents with the db.
   * This method is triggered only by a L2 cache and when a L1 parent cache recover.
   * If the L2 cache did not detect the cache of its parent it will notify that is still its children, otherwise it will notify that is not its children anymore.
   * @param msg is the IsStillParentReqMsg which is used to notify if the cache is still a children of a L1 cache after that it has recover.
   */
  private void onIsStillParentReqMsg(IsStillParentReqMsg msg) {
    ActorRef sender = getSender();
    boolean response;
    if(sender.equals(this.parent)){
      response=true;
    }else{
      response=false;
    }
    sendMessage(new IsStillParentRespMsg(response), sender);
  }

  /**
   * This method is used to handle IsStillParentRespMsg message which is received by a L2 cache or a client.
   * This message will notify to the cache if a node that was a child before the crash is still a child after the recovery
   * If the response is false, that means the previous child has changed its parent and so it's not a child of the cache anymore
   * If the message is true, that means the previous child is still a child (it has not detected the crash) and so we
   * tell it to refresh all it's items, because it could have missed some refill message originated by clients not in its subtree
   * @param msg is the IsStillParentRespMsg message which contains the answer to the IsStillParentReqMsg request.
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
   * This method is used to handle StartRefreshMsg message which is received by a L1 cache
   * This message will notify to a L2 cache that the L1 has recovered before L2 viewed the timeout
   * The L2 cache starts to refill all its items, because it could have missed some refills
   * @param msg is the StartRefreshMsg message.
   */
  private void onStartRefreshMsg(StartRefreshMsg msg){
    LOGGER.debug("Cache " + this.id + "; start_refreshing_items " + ";");
    refreshItems();
  }

  /**
   * This method is used to change the behaviour of the cache to crashed.
   * @param msg is the CrashMsg message which is sent by the main class and used to make crash a node.
   */
  private void onCrashMsg(CrashMsg msg){
    LOGGER.debug("Cache " + this.id + "; is_now_crashed;");
    pendingReq.values().forEach(Cancellable::cancel);
    pendingReq.clear();
    savedItems.clear();
    getContext().become(crashed());
  }

  /* -- END OF crash handling message methods --------------------------------------------------------- */



  /* -- BEGIN OF debug methods --------------------------------------------------------- */

  /**
   * This methode is trigger when a InternalStateMsg is received and is used for debug.
   * This methode will print the current state of the cache, so the saved item, the list of children and its parent
   * @param msg is the InternalStateMsg message, is an empty message used to print the internal state of the cache.
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

  /* -- END OF debug methods --------------------------------------------------------- */


  /**
   * Here we define the mapping between the received message types and our actor methods in the normal behaviour
   */
  @Override
  public Receive createReceive() {
    return receiveBuilder()
            .match(SetChildrenMsg.class, this::onSetChildrenMsg)
            .match(AddChildMsg.class, this::onAddChildMsg)
            .match(SetParentMsg.class, this::onSetParentMsg)
            .match(ReadReqMsg.class, this::onReadReqMsg)
            .match(ReadRespMsg.class, this::onReadRespMsg)
            .match(CritReadReqMsg.class, this::onCritReadReqMsg)
            .match(CritReadRespMsg.class, this::onCritReadRespMsg)
            .match(WriteReqMsg.class, this::onWriteReqMsg)
            .match(RefillMsg.class, this::onRefillMsg)
            .match(InternalStateMsg.class, this::onInternalStateMsg)
            .match(IsStillParentReqMsg.class, this::onIsStillParentReqMsg)
            .match(IsStillParentRespMsg.class, this::onIsStillParentRespMsg)
            .match(CrashMsg.class, this::onCrashMsg)
            .match(TimeoutMsg.class, this::onTimeoutMsg)
            .match(RefreshItemReqMsg.class, this::onRefreshItemReqMsg)
            .match(RefreshItemRespMsg.class, this::onRefreshItemRespMsg)
            .match(StartRefreshMsg.class, this::onStartRefreshMsg)
            .build();
  }

  /**
   * Here we define the mapping between the received message types and our actor methods in the crashed behaviour
   */
  final AbstractActor.Receive crashed() {
    return receiveBuilder()
            .match(RecoveryMsg.class, this::onRecoveryMsg)
            .matchAny(msg -> LOGGER.debug(getSelf().path().name() + " ignoring " + msg.getClass().getSimpleName() + " (crashed)"))
            .build();
  }

  /*final AbstractActor.Receive crashedL1() {
    return receiveBuilder()
            .match(RecoveryL1Msg.class, this::onRecoveryL1Msg)
            .matchAny(msg -> LOGGER.debug(getSelf().path().name() + " ignoring " + msg.getClass().getSimpleName() + " (crashed)"))
            .build();
  }*/
}
