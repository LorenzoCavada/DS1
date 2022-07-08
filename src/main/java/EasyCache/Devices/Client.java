package EasyCache.Devices;

import EasyCache.Config;
import EasyCache.Messages.*;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Cancellable;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import scala.concurrent.duration.Duration;

public class Client extends AbstractActor {

  private Random rnd = new Random();

  private final int id; // ID of the current actor

  private ActorRef parent; //reference for parent

  private List<ActorRef> availableL2; //all the available L2 caches for timeout

  private final Map<UUID, Cancellable> pendingReq; // list of all the pending request which are still waiting for a response

  private Queue<ReqMessage> waitingReqs;


  private static final Logger LOGGER = LogManager.getLogger(Client.class);

  /* -- Actor constructor --------------------------------------------------- */

  public Client(int id) {
    this.id = id;
    this.availableL2=new ArrayList<>();
    this.pendingReq=new HashMap<>();
    this.waitingReqs=new LinkedList<>();
  }

  static public Props props(int id) {
    return Props.create(Client.class, () -> new Client(id));
  }

  /* -- Actor behaviour ----------------------------------------------------- */


  /* -- START OF Sending message methods ----------------------------------------------------- */

  /**
   * This method is used to perform the actual send.
   * The client can only communicate with its parent and the sleep function is used to simulate the network delay.
   * @param m is the message to be sent to the parent
   */
  private void sendMessage(Serializable m){
    try { Thread.sleep(rnd.nextInt(10)); }
    catch (InterruptedException e) { e.printStackTrace(); }
    parent.tell(m, getSelf());
  }

  private void sendMessage(Serializable m, ActorRef dest){
    try { Thread.sleep(rnd.nextInt(10)); }
    catch (InterruptedException e) { e.printStackTrace(); }
    dest.tell(m, getSelf());
  }

  /* -- END OF Sending message methods ----------------------------------------------------- */



  /* -- START OF Configuration message methods ----------------------------------------------------- */

  /**
   * This method is called when a SetParentMsg is received.
   * It is used to set the parent of the client.
   * @param msg is the SetParentMsg message which contains the L2 parent cache associated with the clients.
   */
  private void onSetParentMsg(SetParentMsg msg) {
    this.parent=msg.parent;
    LOGGER.debug("Client " + this.id + "; parent_set_to: " + msg.parent.path().name() + ";");
  }

  /**
   * This method is called when a SetAvailableL2Msg is received.
   * It is used to set the available L2 caches of the client.
   * This list will be used to choose a new L2 cache when the client detects the crash of the current one.
   * @param msg is the SetAvailableL2Msg message which contains the available L2 caches.
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
   * This method is called when a DoReadMsg is received.
   * It is used to trigger the read process by providing the key of the item to read.
   * Is used for debug purposes.
   * First will check if there are some pending requests, if so it will add the request to the waiting list.
   * If there are not pending requests, it will trigger the read request.
   * @param msg is the DoReadMsg message which contains the key of the item to read.
   */
  private void onDoReadMsg(DoReadMsg msg){
    if (this.pendingReq.size()>0){
      this.waitingReqs.add(msg);
    }else{
      doReadReq(msg);
    }
  }

  /**
   * This method will perform the actual read operation.
   * First the client will create a ReadReqMsg and will push himself into the responsePath.
   * Then it will send the ReadReqMsg to the parent node.
   * The client will also add this request to the list of the pending one. in this way it will not send new request until this is finished.
   * Also it will start a timer. If the timer ends and no response has been received, the client will send to himself a TimeoutMsg
   * This TimeoutMsg contains a copy of the request which did not receive a response in time.
   * This TimeoutMsg will be used to handle the detection of the crash of the L2 cache.
   * @param msg is the DoReadMsg message which contains the key of the item to read.
   */
  private void doReadReq(DoReadMsg msg) {
    ReadReqMsg msgToSend = new ReadReqMsg(msg.key, msg.uuid);
    msgToSend.responsePath.push(getSelf());
    sendMessage(msgToSend);
    LOGGER.debug("Client " + this.id + "; starting_read_request_for_item: " + msgToSend.key + "; MSG_id: " + msgToSend.uuid + ";");
    pendingReq.put(msgToSend.uuid,
            getContext().system().scheduler().scheduleOnce(
                    Duration.create(Config.TIMEOUT_CLIENT, TimeUnit.MILLISECONDS),        // when to send the message
                    getSelf(),                                          // destination actor reference
                    new TimeoutMsg(msg),                                  // the message to send
                    getContext().system().dispatcher(),                 // system dispatcher
                    getSelf()                                           // source of the message (myself)
            ));

  }

  /**
   * This method is called when a ReadRespMsg is received.
   * It is used to print the result of a read request.
   * When receiving a response the client will also remove the timer associated with the request and will remove the request from the pending list.
   * After printing the result, the client will check if there are some requests which are waiting to be sent, in this case it will send the next request.
   * This is done to ensure that a client will send a request only when the previous one is finished.
   * @param msg ReadRespMsg message which contains the result of the read request.
   */
  private void onReadRespMsg(ReadRespMsg msg) {
    pendingReq.get(msg.uuid).cancel();
    pendingReq.remove(msg.uuid);
    LOGGER.debug("Client " + this.id + "; read_response_for_item: " + msg.key + " = " + msg.value +"; read_confirmed; MSG_id: " + msg.uuid + "; timeout_cancelled;");
    if (!this.waitingReqs.isEmpty()){
      ReqMessage nextMsg=this.waitingReqs.remove();
      doNext(nextMsg);
    }
  }

  /**
   * This method is called when a DoWriteMsg is received.
   * It is used to trigger the write process by providing the key of the item to update and the newValue.
   * Is used for debug purposes.
   * First will check if there are some pending requests, if so it will add the request to the waiting list.
   * If there are not pending requests, it will trigger the read request.
   * @param msg is the DoReadMsg message which contains the key of the item to update and the newValue.
   */
  private void onDoWriteMsg(DoWriteMsg msg){
    if(this.pendingReq.size()>0){
      this.waitingReqs.add(msg);
    }else{
      doWriteReq(msg);
    }
  }

  /**
   * This method will perform the actual write operation.
   * First the client will create a WriteReqMsg specifying himself as originator.
   * Then it will send the WriteReqMsg to the parent node.
   * The client will also add this request to the list of the pending one. In this way it will not send new request until this is finished.
   * Also it will start a timer. If the timer ends and no response has been received, the client will send to himself a TimeoutMsg
   * This TimeoutMsg contains a copy of the request which did not receive a response in time.
   * This TimeoutMsg will be used to handle the detection of the crash of the L2 cache.
   * @param msg is the DoWriteMsg message which contains the key of the item to write and the new value to set.
   */
  private void doWriteReq(DoWriteMsg msg){
    WriteReqMsg msgToSend = new WriteReqMsg(msg.key, msg.uuid, msg.newValue, getSelf());
    sendMessage(msgToSend);
    LOGGER.debug("Client " + this.id + "; starting_write_request_for_item: " + msgToSend.key + " newValue: "+msgToSend.newValue + " msg_id: " + msg.uuid);
    pendingReq.put(msgToSend.uuid,
            getContext().system().scheduler().scheduleOnce(
                    Duration.create(Config.TIMEOUT_CLIENT, TimeUnit.MILLISECONDS),        // when to send the message
                    getSelf(),                                          // destination actor reference
                    new TimeoutMsg(msg),                                  // the message to send
                    getContext().system().dispatcher(),                 // system dispatcher
                    getSelf()                                           // source of the message (myself)
            ));
  }

  /**
   * This method is called when a onWriteConfirmMsg is received.
   * It is used to print the result of a write request.
   * When receiving a response the client will also remove the timer associated with the request and will remove the request from the pending list.
   * After printing the confirmation, the client will check if there are some requests which are waiting to be sent, in this case it will send the next request.
   * This is done to ensure that a client will send a request only when the previous one is finished.
   * @param msg is the WriteConfirmMsg message which contains the result of the write request.
   */
  private void onWriteConfirmMsg(WriteConfirmMsg msg){
    pendingReq.get(msg.uuid).cancel();
    pendingReq.remove(msg.uuid);
    LOGGER.debug("Client " + this.id + "; write_response_for_item: " + msg.key + "; write_confirmed; timeout_canceled;");
    if (!this.waitingReqs.isEmpty()){
      ReqMessage nextMsg=this.waitingReqs.remove();
      doNext(nextMsg);
    }
  }

  /**
   * This method is used to perform the next request in the waiting list.
   * The message passed need to be casted in the right message in order to be sent.
   * @param msg is a ReqMessage which can be a DoReadMsg or a DoWriteMsg. This is why a cast is needed.
   */
  private void doNext(ReqMessage msg){
    if (msg instanceof DoReadMsg){
      doReadReq((DoReadMsg) msg);
    }else if (msg instanceof DoWriteMsg){
      doWriteReq((DoWriteMsg) msg);
    }
  }

  /**
   * This method is used to handle the ReqErrMsg message.
   * This message is sent by the parent L2 cache when the request made from the client fail for some reason (i.e. crash of the parent of the L2 cache).
   * After receiving this request the client will stop the timer associated to the request and remove it from the pending list.
   * This is done because the parent of the client is still available so there is no reason to change the parent of the client.
   * @param msg is the ReqErrMsg message sent by the parent L2 cache. It contains the uuid of the failed request
   */
  private void onReqErrorMsg(ReqErrorMsg msg) {
    pendingReq.get(msg.awaitedMsg.uuid).cancel();
    pendingReq.remove(msg.awaitedMsg.uuid);
    if(msg.awaitedMsg instanceof ReadReqMsg){
      LOGGER.error("Client " + this.id + "; error_in_read_req: " + msg.awaitedMsg.uuid + "; for key: "+ msg.awaitedMsg.key);
    }else if(msg.awaitedMsg instanceof WriteReqMsg){
      LOGGER.error("Client " + this.id + "; error_in_write_req: " + msg.awaitedMsg.uuid + "; for key: "+ msg.awaitedMsg.key + "; value: " + ((WriteReqMsg) msg.awaitedMsg).newValue);
    }
  }

  /* -- END OF read and write message methods ----------------------------------------------------- */



  /* -- START OF crash handling message methods ----------------------------------------------------- */

  /**
   * This method is called when client goes in timeout.
   * First the client will check if the request is still in the pending list, this is done to avoid some problem with Akka.
   * If so it means that the response did not come in time and that means that the parent L2 cache can be considered crashed.
   * First the client will choose a new parent L2 cache from the list of available ones.
   * After choosing a new parent L2 cache, it will send a new message to the new parent to ask to be added as a child.
   * @param msg is the TimeoutMsg message which contains the request that has gone in timeout.
   */
  private void onTimeoutMsg(TimeoutMsg msg) {
    if (pendingReq.containsKey(msg.awaitedMsg.uuid)){
      LOGGER.debug("Client " + this.id + "; timeout_while_await: " + msg.awaitedMsg.key);
      int newParentIdx= rnd.nextInt(availableL2.size());
      while(availableL2.get(newParentIdx).equals(parent)){
        newParentIdx= rnd.nextInt(availableL2.size());
      }
      LOGGER.debug("Client " + this.id + "; new_parent_selected: " + availableL2.get(newParentIdx).path().name());
      this.parent=availableL2.get(newParentIdx);
      AddChildMsg addMeMsg=new AddChildMsg(getSelf());
      sendMessage(addMeMsg);
      pendingReq.remove(msg.awaitedMsg.uuid);
      doNext(msg.awaitedMsg);
    }else{
      LOGGER.debug("Client " + this.id + "; timeout_but_received_response for: " + msg.awaitedMsg.key);
    }
  }

  /**
   * This method is called when a IsStillParentReqMsg is received.
   * the client will respond saying whether the sender is still its parent or not.
   * This is done because the client may or may not have detected the crash of the L2 cache and so may or may not choose a different L2 cache.
   * @param msg is the onIsStillParentReqMsg sent by the parent L2 cache after it recover from the crash.
   */
  private void onIsStillParentReqMsg(IsStillParentReqMsg msg) {
    ActorRef sender = getSender();
    boolean response;
    if(sender.equals(this.parent)){
      response=true;
    }else{
      response=false;
    }
    sendMessage(new IsStillParentRespMsg(response), getSender());
  }

  /* -- END OF crash handling message methods ----------------------------------------------------- */



  /* -- START OF debug message methods ----------------------------------------------------- */

  /**
   * This method is used only for debug to print the internal state of the client.
   * It will print the id of the client and its parent.
   * @param msg it is an empty message used only for debug.
   */
  private void onInternalStateMsg(InternalStateMsg msg) {
    LOGGER.debug("Client " + this.id + "; parent: " + this.parent.path().name());
  }

  /* -- END OF debug message methods ----------------------------------------------------- */

  /**
   * Here we define the mapping between the received message types and our actor methods
   */
  @Override
  public Receive createReceive() {
    return receiveBuilder()
      .match(SetParentMsg.class, this::onSetParentMsg)
      .match(SetAvailableL2Msg.class, this::onSetAvailL2Msg)
      .match(ReadRespMsg.class, this::onReadRespMsg)
      .match(WriteConfirmMsg.class, this::onWriteConfirmMsg)
      .match(DoReadMsg.class, this::onDoReadMsg)
      .match(DoWriteMsg.class, this::onDoWriteMsg)
      .match(IsStillParentReqMsg.class, this::onIsStillParentReqMsg)
      .match(TimeoutMsg.class, this::onTimeoutMsg)
      .match(InternalStateMsg.class, this::onInternalStateMsg)
      .match(ReqErrorMsg.class, this::onReqErrorMsg)
      .build();
  }
}
