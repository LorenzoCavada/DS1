package EasyCache.Devices;

import EasyCache.CacheType;
import EasyCache.Config;
import EasyCache.Messages.*;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Cancellable;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import scala.concurrent.duration.Duration;

/**
 * This cache represent the behaviour of a client.
 */
public class Client extends AbstractActor {

  /**
   * Used to generate random value.
   */
  private Random rnd = new Random();

  /**
   * numeric ID of this client.
   */
  private final int id;

  /**
   * reference to the parent, a L2 {@link Cache}.
   */
  private ActorRef parent;
  /**
   * list of all the available L2 {@link Cache caches}. When a request goes in timeout, this client change parent through
   * pick a L2 {@link Cache} at random from this list.
   */
  private List<ActorRef> availableL2;

  /**
   * map of all the pending request which are still waiting for a response, with the corresponding timer.
   */
  private final Map<UUID, Cancellable> pendingReq;

  /**
   * Queue of all messages scheduled by the {@link EasyCache.ProjectRunner runner}, because a client could perform a new
   * request only when the previous one has finished.
   */
  private Queue<IdMessage> waitingReqs;


  private static final Logger LOGGER = LogManager.getLogger(Client.class);

  /* -- Actor constructor --------------------------------------------------- */

  /**
   * Constructor of the Client actor.
   * @param id the ID of constructed client.
   */
  public Client(int id) {
    this.id = id;
    this.availableL2=new CopyOnWriteArrayList<>();
    this.pendingReq=new HashMap<>();
    this.waitingReqs=new LinkedList<>();
  }

  static public Props props(int id) {
    return Props.create(Client.class, () -> new Client(id));
  }

  /* -- Actor behaviour ----------------------------------------------------- */


  /* -- START OF Sending message methods ----------------------------------------------------- */

  /**
   * This method is used to send a {@link Message} to the parent, a L2 {@link Cache}, simulating also a network delay.
   * @param m the {@link Message} to send.
   */
  private void sendMessage(Message m){
    int delay = rnd.nextInt(Config.SEND_MAX_DELAY);
    getContext().system().scheduler().scheduleOnce(
            Duration.create(delay, TimeUnit.MILLISECONDS),        // when to send the message
            parent,                                          // destination actor reference
            m,                                  // the message to send
            getContext().system().dispatcher(),                 // system dispatcher
            getSelf()                                           // source of the message (myself)
    );
  }

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

  /* -- END OF Sending message methods ----------------------------------------------------- */



  /* -- START OF Configuration message methods ----------------------------------------------------- */

  /**
   * This method is used to set the parent of the client at initialization.
   * @param msg the {@link SetParentMsg} message which contains the reference to the L2 {@link Cache} to set.
   */
  private void onSetParentMsg(SetParentMsg msg) {
    this.parent=msg.parent;
    LOGGER.debug("Client " + this.id + "; parent_set_to: " + msg.parent.path().name() + ";");
  }


  /**
   * This method is used to set the list of available L2 {@link Cache caches}, at initialization.
   * @param msg the {@link SetAvailableL2Msg} message which contains the {@link Set} of available L2 {@link Cache caches}.
   */
  private void onSetAvailL2Msg(SetAvailableL2Msg msg) {
    this.availableL2 = msg.availL2;
    StringBuilder sb = new StringBuilder();
    sb.append("Client ").append(this.id).append("; available_L2_set_to: [");
    for(ActorRef l2 : msg.availL2){
      sb.append(l2.path().name() +  ", ");
    }
    sb.append("];");
    LOGGER.debug(sb);
  }

  /* -- END OF Configuration message methods ----------------------------------------------------- */



  /* -- START OF read and write message methods ----------------------------------------------------- */

  /**
   * This method is used to handle the arrival of a {@link DoReadMsg} message.
   * The message is sent by the {@link EasyCache.ProjectRunner runner} to schedule a new {@link ReadReqMsg}.
   * If there is another ongoing request, this message is appended in a queue. Otherwise a {@link ReadReqMsg} is immediately
   * performed.
   * @param msg the {@link DoReadMsg} message which contains the key of the item to read.
   */
  private void onDoReadMsg(DoReadMsg msg){
    if (this.pendingReq.size()>0){
      this.waitingReqs.add(msg);
    }else{
      doReadReq(msg);
    }
  }

  /**
   * This method will perform the actual {@link ReadReqMsg read operation}.
   * First the client will create a {@link ReadReqMsg}, pushing itself in the responsePath stack. Then it sends the
   * {@link ReadReqMsg} to its parent.
   * It add this request to pendingReq list, setting a timer to check for timeouts of upper layers nodes.
   * By adding to the pendingReq list, it will not send new request until a corresponding {@link ReadRespMsg} or {@link TimeoutReqMsg}
   * is received.
   * @param msg the {@link DoReadMsg} message which contains the key of the item to read.
   */
  private void doReadReq(DoReadMsg msg) {
    ReadReqMsg msgToSend = new ReadReqMsg(msg.key, msg.uuid);
    msgToSend.responsePath.push(getSelf());
    sendMessage(msgToSend);
    LOGGER.debug("Client " + this.id + "; starting_read_request_for_item: " + msgToSend.key + "; MSG_ID: " + msgToSend.uuid + ";");
    pendingReq.put(msgToSend.uuid,
            getContext().system().scheduler().scheduleOnce(
                    Duration.create(Config.TIMEOUT_CLIENT, TimeUnit.MILLISECONDS),        // when to send the message
                    getSelf(),                                          // destination actor reference
                    new TimeoutReqMsg(msg),                                  // the message to send
                    getContext().system().dispatcher(),                 // system dispatcher
                    getSelf()                                           // source of the message (myself)
            ));

  }

  /**
   * This method is used to handle arrival of a {@link ReadRespMsg} message.
   * The method prints the result (the value) of a {@link ReadReqMsg read request}.
   * The timer of the associated {@link ReadReqMsg request} is cancelled.
   * The method will also schedule the next request, if there are any in the waitingReqs list.
   * @param msg the {@link ReadRespMsg} message which contains value of the requested item.
   */
  private void onReadRespMsg(ReadRespMsg msg) {
    if(pendingReq.containsKey(msg.uuid)){
      pendingReq.get(msg.uuid).cancel();
      pendingReq.remove(msg.uuid);
      LOGGER.debug("Client " + this.id + "; read_response_for_item: " + msg.key + " = " + msg.value +"; read_confirmed; MSG_id: " + msg.uuid + "; timeout_cancelled;");
    }
    if (!this.waitingReqs.isEmpty()){
      IdMessage nextMsg=this.waitingReqs.remove();
      doNext(nextMsg);
    }
  }

  /**
   * This method is used to handle the arrival of a {@link DoCritReadMsg} message.
   * The message is sent by the {@link EasyCache.ProjectRunner runner} to schedule a new {@link CritReadReqMsg}.
   * If there is another ongoing request, this message is appended in a queue. Otherwise a {@link CritReadReqMsg} is immediately
   * performed.
   * @param msg the {@link DoCritReadMsg} message which contains the key of the item to read critically.
   */
  private void onDoCritReadMsg(DoCritReadMsg msg){
    if (this.pendingReq.size()>0){
      this.waitingReqs.add(msg);
    }else{
      doCritRead(msg);
    }
  }

  /**
   * This method will perform the actual {@link CritReadReqMsg critical read operation}.
   * First the client will create a {@link CritReadReqMsg}, pushing itself in the responsePath stack. Then it sends the
   * {@link CritReadReqMsg} to its parent.
   * It add this request to pendingReq list, setting a timer to check for timeouts of upper layers nodes.
   * By adding to the pendingReq list, it will not send new request until a corresponding {@link CritReadRespMsg} or {@link TimeoutReqMsg}
   * is received.
   * @param msg the {@link DoCritReadMsg} message which contains the key of the item to read critically.
   */
  private void doCritRead(DoCritReadMsg msg) {
    CritReadReqMsg msgToSend = new CritReadReqMsg(msg.key, msg.uuid);
    msgToSend.responsePath.push(getSelf());
    sendMessage(msgToSend);
    LOGGER.debug("Client " + this.id + "; starting_critical_read_request_for_item: " + msgToSend.key + "; MSG_ID: " + msgToSend.uuid + ";");
    pendingReq.put(msgToSend.uuid,
            getContext().system().scheduler().scheduleOnce(
                    Duration.create(Config.TIMEOUT_CLIENT, TimeUnit.MILLISECONDS),        // when to send the message
                    getSelf(),                                          // destination actor reference
                    new TimeoutReqMsg(msg),                                  // the message to send
                    getContext().system().dispatcher(),                 // system dispatcher
                    getSelf()                                           // source of the message (myself)
            ));

  }

  /**
   * This method is used to handle arrival of a {@link CritReadRespMsg} message.
   * The method prints the result (the value) of a {@link CritReadRespMsg critical read request}.
   * The timer of the associated {@link CritReadReqMsg request} is cancelled.
   * The method will also schedule the next request, if there are any in the waitingReqs list.
   * @param msg the {@link CritReadRespMsg} message which contains value of the requested item.
   */
  private void onCritReadRespMsg(CritReadRespMsg msg) {
    if(pendingReq.containsKey(msg.uuid)) {
      pendingReq.get(msg.uuid).cancel();
      pendingReq.remove(msg.uuid);
      LOGGER.debug("Client " + this.id + "; critical_read_response_for_item: " + msg.key + " = " + msg.value + "; read_confirmed; MSG_id: " + msg.uuid + "; timeout_cancelled;");
    }
    if (!this.waitingReqs.isEmpty()){
      IdMessage nextMsg=this.waitingReqs.remove();
      doNext(nextMsg);
    }
  }

  /**
   * This method is used to handle the arrival of a {@link DoWriteMsg} message.
   * The message is sent by the {@link EasyCache.ProjectRunner runner} to schedule a new {@link WriteReqMsg}.
   * If there is another ongoing request, this message is appended in a queue. Otherwise a {@link WriteReqMsg} is immediately
   * performed.
   * @param msg the {@link DoWriteMsg} message which contains the key of the item to read and the new value to set.
   */
  private void onDoWriteMsg(DoWriteMsg msg){
    if(this.pendingReq.size()>0){
      this.waitingReqs.add(msg);
    }else{
      doWriteReq(msg);
    }
  }

  /**
   * This method will perform the actual {@link WriteReqMsg write operation}.
   * First the client will create a {@link WriteReqMsg}, pushing itself in the responsePath stack. Then it sends the
   * {@link WriteReqMsg} to its parent.
   * It add this request to pendingReq list, setting a timer to check for timeouts of upper layers nodes.
   * By adding to the pendingReq list, it will not send new request until a corresponding {@link WriteConfirmMsg} or {@link TimeoutReqMsg}
   * is received.
   * @param msg the {@link DoWriteMsg} message which contains the key of the item to write and the new value to set.
   */
  private void doWriteReq(DoWriteMsg msg){
    WriteReqMsg msgToSend = new WriteReqMsg(msg.key, msg.uuid, msg.newValue, getSelf());
    sendMessage(msgToSend);
    LOGGER.debug("Client " + this.id + "; starting_write_request_for_item: " + msgToSend.key + " newValue: "+msgToSend.newValue + " msg_id: " + msg.uuid);
    pendingReq.put(msgToSend.uuid,
            getContext().system().scheduler().scheduleOnce(
                    Duration.create(Config.TIMEOUT_CLIENT, TimeUnit.MILLISECONDS),        // when to send the message
                    getSelf(),                                          // destination actor reference
                    new TimeoutReqMsg(msg),                                  // the message to send
                    getContext().system().dispatcher(),                 // system dispatcher
                    getSelf()                                           // source of the message (myself)
            ));
  }

  /**
   * This method is used to handle arrival of a {@link WriteConfirmMsg} message.
   * The method prints the acknowledgment of a successful {@link WriteReqMsg write request}.
   * The timer of the associated {@link WriteReqMsg request} is cancelled.
   * The method will also schedule the next request, if there are any in the waitingReqs list.
   * @param msg the {@link WriteConfirmMsg} acknowledgment message of a successful {@link WriteReqMsg write request}.
   */
  private void onWriteConfirmMsg(WriteConfirmMsg msg){
    if(pendingReq.containsKey(msg.uuid)) {
      pendingReq.get(msg.uuid).cancel();
      pendingReq.remove(msg.uuid);
      LOGGER.debug("Client " + this.id + "; write_response_for_item: " + msg.key + "; write_confirmed; MSG_ID: " + msg.uuid + "; timeout_canceled;");
    }
    if (!this.waitingReqs.isEmpty()){
      IdMessage nextMsg=this.waitingReqs.remove();
      doNext(nextMsg);
    }
  }

  /**
   * This method is used to handle the arrival of a {@link DoCritWriteMsg} message.
   * The message is sent by the {@link EasyCache.ProjectRunner runner} to schedule a new {@link CritWriteReqMsg}.
   * If there is another ongoing request, this message is appended in a queue. Otherwise a {@link CritWriteReqMsg} is immediately
   * performed.
   * @param msg the {@link DoCritWriteMsg} message which contains the key of the item to read and the new value to set.
   */
  private void onDoCritWriteMsg(DoCritWriteMsg msg){
    if(this.pendingReq.size()>0){
      this.waitingReqs.add(msg);
    }else{
      doCritWriteReq(msg);
    }
  }

  /**
   * This method will perform the actual {@link CritWriteReqMsg write operation}.
   * First the client will create a {@link CritWriteReqMsg}, pushing itself in the responsePath stack. Then it sends the
   * {@link CritWriteReqMsg} to its parent.
   * It add this request to pendingReq list, setting a timer to check for timeouts of upper layers nodes.
   * By adding to the pendingReq list, it will not send new request until a corresponding {@link WriteConfirmMsg} or {@link TimeoutReqMsg}
   * or {@link CritWriteErrorMsg} is received.
   * @param msg the {@link DoCritWriteMsg} message which contains the key of the item to write and the new value to set.
   */
  private void doCritWriteReq(DoCritWriteMsg msg){
    CritWriteReqMsg msgToSend = new CritWriteReqMsg(msg.key, msg.uuid, msg.newValue, getSelf());
    sendMessage(msgToSend);
    LOGGER.debug("Client " + this.id + "; starting_crit_write_request_for_item: " + msgToSend.key + "; newValue: "+msgToSend.newValue + " MSG_ID: " + msg.uuid + ";");
    pendingReq.put(msgToSend.uuid,
            getContext().system().scheduler().scheduleOnce(
                    Duration.create(Config.TIMEOUT_CLIENT_CRIT_WRITE, TimeUnit.MILLISECONDS),        // when to send the message
                    getSelf(),                                          // destination actor reference
                    new TimeoutReqMsg(msg),                                  // the message to send
                    getContext().system().dispatcher(),                 // system dispatcher
                    getSelf()                                           // source of the message (myself)
            ));
  }

  /**
   * This method is used to handle arrival of a {@link CritWriteConfirmMsg} message.
   * The method prints the acknowledgment of a successful {@link CritWriteReqMsg critical write request}.
   * The timer of the associated {@link CritWriteReqMsg request} is cancelled.
   * The method will also schedule the next request, if there are any in the waitingReqs list.
   * @param msg the {@link CritWriteConfirmMsg} acknowledgment message of a successful {@link CritWriteReqMsg critical write request}.
   */
  private void onCritWriteConfirmMsg(CritWriteConfirmMsg msg){
    if(pendingReq.containsKey(msg.uuid)) {
      pendingReq.get(msg.uuid).cancel();
      pendingReq.remove(msg.uuid);
      LOGGER.debug("Client " + this.id + "; crit_write_response_for_item: " + msg.key + "; critical_write_confirmed; timeout_canceled;" + " msg_id: " + msg.uuid);
    }
    if (!this.waitingReqs.isEmpty()){
      IdMessage nextMsg=this.waitingReqs.remove();
      doNext(nextMsg);
    }
  }

  /**
   * This method is used to handle arrival of a {@link CritWriteErrorMsg} message.
   * The method prints the error associated to an unsuccessful {@link CritWriteReqMsg critical write request}.
   * The timer of the associated {@link CritWriteReqMsg request} is cancelled.
   * The method will also schedule the next request, if there are any in the waitingReqs list.
   * @param msg the {@link CritWriteErrorMsg} error message of an unsuccessful {@link CritWriteReqMsg critical write request}.
   */
  private void onCritWriteErrorMsg(CritWriteErrorMsg msg){
    if(pendingReq.containsKey(msg.uuid)) {
      LOGGER.error("Client " + this.id + "; crit_write_response_for_item: " + msg.key + "; crit_write_error; timeout_cancelled;");
      pendingReq.get(msg.uuid).cancel();
      pendingReq.remove(msg.uuid);
    }
    if (!this.waitingReqs.isEmpty()){
      IdMessage nextMsg=this.waitingReqs.remove();
      doNext(nextMsg);
    }
  }


  /**
   * This service method is used to perform the next request in the waiting list.
   * The message passed need to be casted to the right message type before be sent. Due to the inheritance of
   * this project, the order in which we check the instanceof is relevant.
   * For example, if we switch the order of the instanceof, a {@link DoCritReadMsg} could be seen as a {@link DoReadMsg}
   * and then the client will perform a {@link ReadReqMsg read request} instead of a {@link CritReadReqMsg critical read}.
   * @param msg the {@link IdMessage doMessage} of the next request.
   */
  private void doNext(IdMessage msg){
    if(msg instanceof DoCritReadMsg){
      doCritRead((DoCritReadMsg) msg);
    }else if(msg instanceof DoCritWriteMsg){
      doCritWriteReq((DoCritWriteMsg) msg);
    }else if(msg instanceof DoReadMsg) {
      doReadReq((DoReadMsg) msg);
    }else if (msg instanceof DoWriteMsg){
    doWriteReq((DoWriteMsg) msg);
    }
  }

  /**
   * This method is used to handle arrival of a {@link ReqErrorMsg} message.
   * This message is sent by the parent L2 cache when the request made from the client fail for the crash of an upper L1 {@link Cache}
   * or a request is performed for an item marked as invalid.
   * The method prints the an appropriate error message.
   * The timer of the associated request is cancelled.
   * The method will also schedule the next request, if there are any in the waitingReqs list.
   * @param msg the {@link ReqErrorMsg} message sent by the parent L2 cache. It contains the uuid of the failed request.
   */
  private void onReqErrorMsg(ReqErrorMsg msg) {
    if(pendingReq.containsKey(msg.awaitedMsg.uuid)) {
      pendingReq.get(msg.awaitedMsg.uuid).cancel();
      pendingReq.remove(msg.awaitedMsg.uuid);
    }
    if (msg.awaitedMsg instanceof CritReadReqMsg) {
      LOGGER.error("Client " + this.id + "; error_in_crit_read_req: " + msg.awaitedMsg.uuid + "; for key: " + msg.awaitedMsg.key);
    } else if (msg.awaitedMsg instanceof CritWriteReqMsg) {
      LOGGER.error("Client " + this.id + "; error_in_crit_write_req: " + msg.awaitedMsg.uuid + "; for key: " + msg.awaitedMsg.key + "; value: " + ((CritWriteReqMsg) msg.awaitedMsg).newValue);
    } else if (msg.awaitedMsg instanceof ReadReqMsg) {
      LOGGER.error("Client " + this.id + "; error_in_read_req: " + msg.awaitedMsg.uuid + "; for key: " + msg.awaitedMsg.key);
    } else if (msg.awaitedMsg instanceof WriteReqMsg) {
      LOGGER.error("Client " + this.id + "; error_in_write_req: " + msg.awaitedMsg.uuid + "; for key: " + msg.awaitedMsg.key + "; value: " + ((WriteReqMsg) msg.awaitedMsg).newValue);
    }
    if (!this.waitingReqs.isEmpty()){
      IdMessage nextMsg=this.waitingReqs.remove();
      doNext(nextMsg);
    }
  }

  /* -- END OF read and write message methods ----------------------------------------------------- */



  /* -- START OF crash handling message methods ----------------------------------------------------- */

  /**
   * This method is used to handle the arrival of a {@link TimeoutReqMsg} message.
   * This is triggered when this client detects the crash of its parent while waiting for some response.
   * The client detecting the crash will set another L2 {@link Cache} as its new parent and will notify L2 {@link Cache} of the change.
   * The method will also schedule the next request, if there are any in the waitingReqs list.
   * @param msg the {@link TimeoutReqMsg} message which contains a copy of the request that has failed.
   */
  private void onTimeoutReqMsg(TimeoutReqMsg msg) {
    if (pendingReq.containsKey(msg.awaitedMsg.uuid)){
      LOGGER.warn("Client " + this.id + "; timeout_while_await: " + msg.awaitedMsg.key + "; MSG_ID: " + msg.awaitedMsg.uuid + "; ");
      int newParentIdx= rnd.nextInt(availableL2.size());
      while(availableL2.get(newParentIdx).equals(parent)){
        newParentIdx= rnd.nextInt(availableL2.size());
      }
      LOGGER.debug("Client " + this.id + "; new_parent_selected: " + availableL2.get(newParentIdx).path().name());
      this.parent=availableL2.get(newParentIdx);
      AddChildMsg addMeMsg=new AddChildMsg(getSelf());
      sendMessage(addMeMsg);
      pendingReq.remove(msg.awaitedMsg.uuid);
      if (!this.waitingReqs.isEmpty()){
        IdMessage nextMsg=this.waitingReqs.remove();
        doNext(nextMsg);
      }
    }else{
      LOGGER.warn("Client " + this.id + "; timeout_but_received_response for: " + msg.awaitedMsg.key);
    }
  }

  /**
   * This method is used to handle the arrival of a {@link IsStillParentReqMsg} message.
   * If the sender (a L2 {@link Cache cache}) is still its parent after recovery, this client will respond affirmatively.
   * @param msg the {@link IsStillParentReqMsg} message used to ask if this client is still the child of a L2 {@link Cache cache}.
   */
  private void onIsStillParentReqMsg(IsStillParentReqMsg msg) {
    ActorRef sender = getSender();
    boolean response;
    if(sender.equals(this.parent)){
      response=true;
    }else{
      response=false;
    }
    sendMessage(new IsStillParentRespMsg(response, msg.uuid), getSender());
  }

  /**
   * This method is used to handle the arrival of a {@link CancelTimeoutMsg} message.
   * This message arrivies when an upper L1 {@link Cache} crashes and recovers before its child (L2 {@link Cache}) notices the crash.
   * Removing the timers from the pendingReq list at client is needed to avoid a timeout of client (with subsequent change of parent)
   * even if the parental L2 {@link Cache} has not crashed.
   * @param msg the {@link CancelTimeoutMsg} message that contains the list of uuids of timers to cancel.
   */
  private void onCancelTimeoutMsg(CancelTimeoutMsg msg) {
    for(UUID uuid : msg.uuids){
      if(pendingReq.containsKey(uuid)){
        pendingReq.get(uuid).cancel();
        pendingReq.remove(uuid);
        LOGGER.error("Client " + this.id + "; error_in_req: " + uuid + "; timeout_cancelled");
      }
    }
  }

  /* -- END OF crash handling message methods ----------------------------------------------------- */



  /* -- START OF debug message methods ----------------------------------------------------- */

  /**
   * This method is triggered when a {@link InternalStateMsg} is received from the the {@link EasyCache.ProjectRunner runner}.
   * This method is used for debugging. It will print the current state of the client: its id and its parent.
   * @param msg is the {@link InternalStateMsg} message, is an empty message used to print the internal state of the cache.
   */
  private void onInternalStateMsg(InternalStateMsg msg) {
    LOGGER.debug("Client " + this.id + "; parent: " + this.parent.path().name());
  }

  /* -- END OF debug message methods ----------------------------------------------------- */

  /**
   * The mapping between the received message types and our actor methods in the normal behaviour.
   */
  @Override
  public Receive createReceive() {
    return receiveBuilder()
      .match(SetParentMsg.class, this::onSetParentMsg)
      .match(SetAvailableL2Msg.class, this::onSetAvailL2Msg)
      .match(DoCritReadMsg.class, this::onDoCritReadMsg)
      .match(DoCritWriteMsg.class, this::onDoCritWriteMsg)
      .match(CritReadRespMsg.class, this::onCritReadRespMsg)
      .match(CritWriteConfirmMsg.class, this::onCritWriteConfirmMsg)
      .match(ReadRespMsg.class, this::onReadRespMsg)
      .match(WriteConfirmMsg.class, this::onWriteConfirmMsg)
      .match(DoReadMsg.class, this::onDoReadMsg)
      .match(DoWriteMsg.class, this::onDoWriteMsg)
      .match(IsStillParentReqMsg.class, this::onIsStillParentReqMsg)
      .match(TimeoutReqMsg.class, this::onTimeoutReqMsg)
      .match(InternalStateMsg.class, this::onInternalStateMsg)
      .match(ReqErrorMsg.class, this::onReqErrorMsg)
      .match(CritWriteErrorMsg.class, this::onCritWriteErrorMsg)
      .match(CancelTimeoutMsg.class, this::onCancelTimeoutMsg)
      .build();
  }
}
