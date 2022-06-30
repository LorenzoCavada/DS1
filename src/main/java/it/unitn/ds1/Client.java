package it.unitn.ds1;

import akka.actor.AbstractActor;
import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.Props;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

class Client extends AbstractActor {

  private Random rnd = new Random();

  private final int id; // ID of the current actor

  private ActorRef parent; //reference for parent

  private List<ActorRef> availableL2; //all the available L2 caches for timeout

  /* -- Actor constructor --------------------------------------------------- */

  public Client(int id) {
    this.id = id;
  }

  static public Props props(int id) {
    return Props.create(Client.class, () -> new Client(id));
  }

  /* -- Actor behaviour ----------------------------------------------------- */
  private void onSetParentMsg(Messages.SetParentMsg msg) {
    this.parent=msg.parent;
    System.out.println("Client " + this.id + ";setParent;parent = " + msg.parent + ";");
  }

  private void onSetAvailL2Msg(Messages.SetAvailableL2Msg msg) {
    this.availableL2=msg.availL2;
    System.out.println("Client " + this.id + ";setListL2;list = " + msg.availL2 + ";");
  }

  private void onReadRespMsg(Messages.ReadRespMsg msg) {
    System.out.println("Client " + this.id + ";ReadResp;key = " + msg.key + ";value = " + msg.value);
  }

  private void onWriteConfirmMsg(Messages.WriteConfirmMsg msg){
    System.out.println("Client " + this.id + ";WriteConfirm;key = " + msg.key + ";confirmed");
  }

  private void onDoReadMsg(Messages.DoReadMsg msg){
    doReadReq(msg.key);
  }

  private void onDoWriteMsg(Messages.DoWriteMsg msg){
    doWriteReq(msg.key, msg.newValue);
  }

  private void doReadReq(Integer key){
    Messages.ReadReqMsg msg = new Messages.ReadReqMsg(key);
    msg.responsePath.push(getSelf());
    sendMessage(msg);
    System.out.println("Client " + this.id + ";ReadReq;key = " + msg.key + ";");
  }

  private void doWriteReq(Integer key, Integer value){
    Messages.WriteReqMsg msg = new Messages.WriteReqMsg(key, value, getSelf());
    sendMessage(msg);
    System.out.println("Client " + this.id + ";WriteReq;key = " + msg.key + ";value="+msg.newValue);
  }


  // Here we define the mapping between the received message types
  // and our actor methods
  @Override
  public Receive createReceive() {
    return receiveBuilder()
      .match(Messages.SetParentMsg.class, this::onSetParentMsg)
      .match(Messages.SetAvailableL2Msg.class, this::onSetAvailL2Msg)
      .match(Messages.ReadRespMsg.class, this::onReadRespMsg)
      .match(Messages.WriteConfirmMsg.class, this::onWriteConfirmMsg)
      .match(Messages.DoReadMsg.class, this::onDoReadMsg)
      .match(Messages.DoWriteMsg.class, this::onDoWriteMsg)
      .build();
  }

  private void sendMessage(Serializable m){
    try { Thread.sleep(rnd.nextInt(10)); }
    catch (InterruptedException e) { e.printStackTrace(); }
    parent.tell(m, getSelf());
  }

  // DEBUG
  public ActorRef getParent(){
    return parent;
  }
}
