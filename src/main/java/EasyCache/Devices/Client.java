package EasyCache.Devices;

import EasyCache.Config;
import EasyCache.Messages.*;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;

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

  private final List<UUID> pendingReq; // list of all the pending request which are still waiting for a response

  private Queue<ReqMessage> waitingReqs;


  private static final Logger LOGGER = LogManager.getLogger(Client.class);

  /* -- Actor constructor --------------------------------------------------- */

  public Client(int id) {
    this.id = id;
    this.availableL2=new ArrayList<>();
    this.pendingReq=new ArrayList<>();
    this.waitingReqs=new LinkedList<>();
  }

  static public Props props(int id) {
    return Props.create(Client.class, () -> new Client(id));
  }

  /* -- Actor behaviour ----------------------------------------------------- */

  // This method is called when a SetParentMsg is received.
  // It is used to set the parent of the client.
  private void onSetParentMsg(SetParentMsg msg) {
    this.parent=msg.parent;
    LOGGER.debug("Client " + this.id + "; parent_set_to: " + msg.parent.path().name() + ";");
  }

  // This method is called when a SetAvailableL2Msg is received.
  // It is used to set the available L2 caches of the client.
  // This list will become useful when the crash will be introduced.
  private void onSetAvailL2Msg(SetAvailableL2Msg msg) {
    this.availableL2=msg.availL2;
    LOGGER.debug("Client " + this.id + "; available_L2_set_to: " + msg.availL2 + ";");
  }

  // This method is called when a ReadRespMsg is received.
  // It is used to print the result of a read request.
  private void onReadRespMsg(ReadRespMsg msg) {
    pendingReq.remove(msg.uuid);
    LOGGER.debug("Client " + this.id + "; read_response_for_item: " + msg.key + " = " + msg.value +"; read_confirmed; MSG_id: " + msg.uuid + ";");
    if (!this.waitingReqs.isEmpty()){
      ReqMessage nextMsg=this.waitingReqs.remove();
      doNext(nextMsg);
    }
  }

  // This method is called when a onWriteConfirmMsg is received.
  // It is used to print the result of a write request.
  private void onWriteConfirmMsg(WriteConfirmMsg msg){
    pendingReq.remove(msg.uuid);
    LOGGER.debug("Client " + this.id + "; write_response_for_item: " + msg.key + "; write_confirmed;");
    if (!this.waitingReqs.isEmpty()){
      ReqMessage nextMsg=this.waitingReqs.remove();
      doNext(nextMsg);
    }
  }

  private void doNext(ReqMessage msg){
    if (msg instanceof DoReadMsg){
      doReadReq((DoReadMsg) msg);
    }else if (msg instanceof DoWriteMsg){
      doWriteReq((DoWriteMsg) msg);
    }
  }

  // This method is called when a onDoReadMsg is received.
  // It is used to trigger the read process by providing the key of the item to read.
  // Is used for debug purposes.
  private void onDoReadMsg(DoReadMsg msg){
    if (this.pendingReq.size()>0){
      this.waitingReqs.add(msg);
    }else{
      doReadReq(msg);
    }
  }

  // This method is called when a DoWriteMsg is received.
  // It is used to trigger the write process by providing the key of the item to update and the newValue.
  // Is used for debug purposes.
  private void onDoWriteMsg(DoWriteMsg msg){
    if(this.pendingReq.size()>0){
      this.waitingReqs.add(msg);
    }else{
      doWriteReq(msg);
    }

  }

  // This method will perform the actual read operation.
  // First the client will create a ReadReqMsg and will push himself into the responsePath.
  // Then it will send the ReadReqMsg to the parent node.
  private void doReadReq(DoReadMsg msg) {
    ReadReqMsg msgToSend = new ReadReqMsg(msg.key, msg.uuid);
    msgToSend.responsePath.push(getSelf());
    sendMessage(msgToSend);
    LOGGER.debug("Client " + this.id + "; starting_read_request_for_item: " + msgToSend.key + "; MSG_id: " + msgToSend.uuid + ";");
    pendingReq.add(msgToSend.uuid);

    getContext().system().scheduler().scheduleOnce(
            Duration.create(Config.TIMEOUT_CLIENT, TimeUnit.MILLISECONDS),        // when to send the message
            getSelf(),                                          // destination actor reference
            new TimeoutMsg(msgToSend),                                  // the message to send
            getContext().system().dispatcher(),                 // system dispatcher
            getSelf()                                           // source of the message (myself)
    );

  }

  // This method is called when client goes in timeout
  private void onTimeoutMsg(TimeoutMsg msg) {
    if (pendingReq.contains(msg.awaitedMsg.uuid)){
      LOGGER.debug("Client " + this.id + "; timeout_while_await: " + msg.awaitedMsg.key);
      int newParentIdx= rnd.nextInt(availableL2.size());
      while(!availableL2.get(newParentIdx).equals(parent)){
        newParentIdx= rnd.nextInt(availableL2.size());
      }
      this.parent=availableL2.get(newParentIdx);
      pendingReq.remove(msg.awaitedMsg.uuid);
      doNext(msg.awaitedMsg);
    }else{
      LOGGER.debug("Client " + this.id + "; timeout_but_received_response for: " + ((ReadReqMsg) msg.awaitedMsg).key);
    }
  }

  // This method will perform the actual write operation.
  // First the client will create a WriteReqMsg specifying himself as originator.
  // Then it will send the WriteReqMsg to the parent node.
  private void doWriteReq(DoWriteMsg msg){
    WriteReqMsg msgToSend = new WriteReqMsg(msg.key, msg.uuid, msg.newValue, getSelf());
    sendMessage(msgToSend);
    LOGGER.debug("Client " + this.id + "; starting_write_request_for_item: " + msgToSend.key + " newValue: "+msgToSend.newValue);
    pendingReq.add(msgToSend.uuid);
  }

  // This method is called when a IsStillParentReqMsg is received.
  // We respond saying whether the sender is our parent
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


  // Here we define the mapping between the received message types
  // and our actor methods
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
      .build();
  }

  // This method is used to perform the actual send.
  // The client can only communicate with its parent and the sleep function is used to simulate the network delay.
  private void sendMessage(Serializable m){
    try { Thread.sleep(rnd.nextInt(10)); }
    catch (InterruptedException e) { e.printStackTrace(); }
    parent.tell(m, getSelf());
  }

}
