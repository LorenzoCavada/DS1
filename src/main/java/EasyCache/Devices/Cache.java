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

  private void multicast(Serializable m) {

    // multicast to all the children of the cache (do not send any message to self)
    for (ActorRef p: children) {
      p.tell(m, getSelf());
      // simulate network delays using sleep
      try { Thread.sleep(rnd.nextInt(10)); }
      catch (InterruptedException e) { e.printStackTrace(); }

    }
  }

  // This method is used to set the children of the cache. Is triggered by a SetChildrenMsg message.
  private void onSetChildrenMsg(SetChildrenMsg msg) {
    this.children = msg.children;
    StringBuilder sb = new StringBuilder();
    for (ActorRef c: children) {
      sb.append(c.path().name() + ";");
    }
    LOGGER.debug("Cache " + this.id + "; children_set_to: [" + sb + "]");
  }

  private void onAddChildMsg(AddChildMsg msg) {
    if (!this.children.contains(msg.child))
      this.children.add(msg.child);
    StringBuilder sb = new StringBuilder();
    for (ActorRef c: children) {
      sb.append(c.path().name() + ";");
    }
    LOGGER.debug("Cache " + this.id + "; children_set_to: [" + sb + "]");
  }

  // This method is used to set the parent of the cache. Is triggered by a SetParentMsg message.
  private void onSetParentMsg(SetParentMsg msg) {
    this.parent = msg.parent;
    LOGGER.debug("Cache " + this.id + "; parent_set_to: " + msg.parent.path().name() + ";");
  }

  // This method is used to handle the ReadReqMsg message that represent the read request message.
  // This message can come both by a L2 cache or from a Client
  // If the requested element is in the cache, the value associated to the key is returned to the requester
  // Else the message is forwarded to the parent, that could be and L1 (in case is an L2 cache) or the DB
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
      LOGGER.debug("Cache " + this.id + "; pending_req_list: " + pendingReq.keySet() + "; adding_req_id: " + msg.uuid + ";");
      sendMessage(msg, parent);
    }
  }

  // This method is used to handle the CritReadReqMsg message that represent the critical read request message.
  // This message can come both by a L2 cache or from a Client
  // We message is forwarded to the parent, that could be and L1 (in case is an L2 cache) or the DB
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
    LOGGER.debug("Cache " + this.id + "; pending_req_list: " + pendingReq + "; adding_req_id: " + msg.uuid + ";");
    sendMessage(msg, parent);

  }

  // This method is used to handle the WriteReqMsg message which represent the write request message.
  // A cache can only forward the request to its parent till it reach the DB.
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
    LOGGER.debug("Cache " + this.id + "; pending_req_list: " + pendingReq + "; adding_req_id: " + msg.uuid + ";");
    sendMessage(msg, parent);
  }


  // This method is used to handle the ReadRespMsg message which represent the read response message.
  // After a read the cache needs to store the value in its memory and then forward it to its children
  private void onReadRespMsg(ReadRespMsg msg) {
    Integer key = msg.key;
    savedItems.put(key, msg.value);
    ActorRef nextHop = msg.responsePath.pop();
    LOGGER.debug("Cache " + this.id + "; read_resp_for_item = " + msg.key + "; forward_to " + nextHop.path().name() + "; MSG_id: " + msg.uuid + "; timeout_cancelled;");
    pendingReq.get(msg.uuid).cancel();
    pendingReq.remove(msg.uuid); //removing the uuid of the message from the list of the pending ones
    LOGGER.debug("Cache " + this.id + "; pending_req_list: " + pendingReq + "; remove_req_id: " + msg.uuid + ";");
    sendMessage(msg, nextHop);
  }

  private void onCritReadRespMsg(CritReadRespMsg msg) {
    Integer key = msg.key;
    savedItems.put(key, msg.value);
    ActorRef nextHop = msg.responsePath.pop();
    LOGGER.debug("Cache " + this.id + "; crit_read_resp_for_item = " + msg.key + "; forward_to " + nextHop.path().name() + "; MSG_id: " + msg.uuid + "; timeout_cancelled;");
    pendingReq.get(msg.uuid).cancel();
    pendingReq.remove(msg.uuid); //removing the uuid of the message from the list of the pending ones
    LOGGER.debug("Cache " + this.id + "; pending_req_list: " + pendingReq + "; remove_req_id: " + msg.uuid + ";");
    sendMessage(msg, nextHop);
  }

  // This method is used to handle the RefillMsg message. The RefillMsg message represent the ack of a write request.
  // The cache will first check if the value is stored in its memory, in that case will update it
  // Then, if the cache is a L1 cache, it will simply forward the message to all its children
  // If the cache is a L2 cache, it will check if the originator of the request is one of its children
  // If yes the L2 cache will send the confirmation of the write operation to the client
  private void onRefillMsg(RefillMsg msg) {
    Integer key = msg.key;
    LOGGER.debug("Cache " + this.id + "; pending_req_list: " + pendingReq + "; remove_req_id: " + msg.uuid + ";");
    if(savedItems.containsKey(key)){
      LOGGER.debug("Cache " + this.id + "; refill_for_item: " + msg.key + "; value: " + msg.newValue + "; MSG_id: " + msg.uuid + ";");
      savedItems.put(key, msg.newValue);
    }
    if(this.type == CacheType.L1){
      pendingReq.remove(msg.uuid); //removing the uuid of the message from the list of the pending ones
      multicast(msg);
    }else if(this.type == CacheType.L2){
      pendingReq.get(msg.uuid).cancel();
      pendingReq.remove(msg.uuid); //removing the uuid of the message from the list of the pending ones
      ActorRef originator = msg.originator;
      if(children.contains(originator)) {
        WriteConfirmMsg resp = new WriteConfirmMsg(msg.key, msg.uuid);
        LOGGER.debug("Cache " + this.id + "; write_ack_for_item: " + msg.key + "; forward_to: " + msg.originator.path().name() + "; MSG_id: " + msg.uuid + ";");
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
    sb.append("]; Parent: " + parent.path().name() + "; Pending request: " + pendingReq);
    LOGGER.debug(sb);
  }

  // This method is used to send a message to a given actor, is needed to simulate the network delays
  private void sendMessage(Serializable m, ActorRef dest){
    try { Thread.sleep(rnd.nextInt(10)); }
    catch (InterruptedException e) { e.printStackTrace(); }
    dest.tell(m, getSelf());
  }

  private void onIsStillParentReqMsg(IsStillParentReqMsg msg) {
    ActorRef sender = getSender();
    boolean response;
    if(sender.equals(this.parent)){
      response=true;
    }else{
      response=false;
    }
    sender.tell(new IsStillParentRespMsg(response), getSelf());
  }

  private void onRecoveryMsg(RecoveryMsg msg) {
    LOGGER.debug("Cache " + this.id + "; recovers;");
    pendingReq.values().forEach(Cancellable::cancel);
    pendingReq.clear();
    savedItems.clear();
    for(ActorRef child: children){
      child.tell(new IsStillParentReqMsg(), getSelf());
    }

    if(this.type==CacheType.L1){
      //TODO
    }
    getContext().become(createReceive());
  }

  private void onIsStillParentRespMsg(IsStillParentRespMsg msg){
    if (!msg.response){
      LOGGER.debug("Cache " + this.id + "; is_not_parent_of: " + getSender().path().name() + ";");
      children.remove(getSender());
    }
  }

  //this method is used to change the behaviour of the cache to crashed
  private void onCrashMsg(CrashMsg msg){
      LOGGER.debug("Cache " + this.id + "; is_now_crashed;");
      getContext().become(crashed());
  }

  private void onTimeoutMsg(TimeoutMsg msg) {
    if (pendingReq.containsKey(msg.awaitedMsg.uuid)){
      LOGGER.debug("Cache " + this.id + "; timeout_while_await: " + msg.awaitedMsg.key + " msg_id: " + msg.awaitedMsg.uuid);
      this.parent=this.db;

      LOGGER.debug("Cache " + this.id + "; new_parent_selected: " + this.parent.path().name());
      AddChildMsg addMeMsg=new AddChildMsg(getSelf());
      this.parent.tell(addMeMsg, getSelf());
      pendingReq.remove(msg.awaitedMsg.uuid);

      ReqErrorMsg errMsg=new ReqErrorMsg(msg.awaitedMsg);
      if(msg.awaitedMsg instanceof ReadReqMsg){
        ActorRef dest = ((ReadReqMsg) msg.awaitedMsg).responsePath.pop();
        dest.tell(errMsg, getSelf());
      }else if(msg.awaitedMsg instanceof WriteReqMsg){
        ((WriteReqMsg) msg.awaitedMsg).originator.tell(errMsg, getSelf());
      }
    }else{
      LOGGER.debug("Client " + this.id + "; timeout_but_received_response for: " + msg.awaitedMsg.key);
    }
  }

  // Here we define the mapping between the received message types and our actor methods
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
            .build();
  }

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
