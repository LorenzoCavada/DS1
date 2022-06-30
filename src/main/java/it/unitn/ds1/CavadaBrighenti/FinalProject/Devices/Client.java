package it.unitn.ds1.CavadaBrighenti.FinalProject.Devices;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import it.unitn.ds1.CavadaBrighenti.FinalProject.Messages.*;

import java.io.Serializable;
import java.util.List;
import java.util.Random;

public class Client extends AbstractActor {

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

  // This method is called when a SetParentMsg is received.
  // It is used to set the parent of the client.
  private void onSetParentMsg(SetParentMsg msg) {
    this.parent=msg.parent;
    System.out.println("Client " + this.id + ";setParent;parent = " + msg.parent + ";");
  }

  // This method is called when a SetAvailableL2Msg is received.
  // It is used to set the available L2 caches of the client.
  // This list will become useful when the crash will be introduced.
  private void onSetAvailL2Msg(SetAvailableL2Msg msg) {
    this.availableL2=msg.availL2;
    System.out.println("Client " + this.id + ";setListL2;list = " + msg.availL2 + ";");
  }

  // This method is called when a ReadRespMsg is received.
  // It is used to print the result of a read request.
  private void onReadRespMsg(ReadRespMsg msg) {
    System.out.println("Client " + this.id + ";ReadResp;key = " + msg.key + ";value = " + msg.value);
  }

  // This method is called when a onWriteConfirmMsg is received.
  // It is used to print the result of a write request.
  private void onWriteConfirmMsg(WriteConfirmMsg msg){
    System.out.println("Client " + this.id + ";WriteConfirm;key = " + msg.key + ";confirmed");
  }

  // This method is called when a onDoReadMsg is received.
  // It is used to trigger the read process by providing the key of the item to read.
  // Is used for debug purposes.
  private void onDoReadMsg(DoReadMsg msg){
    doReadReq(msg.key);
  }

  // This method is called when a DoWriteMsg is received.
  // It is used to trigger the write process by providing the key of the item to update and the newValue.
  // Is used for debug purposes.
  private void onDoWriteMsg(DoWriteMsg msg){
    doWriteReq(msg.key, msg.newValue);
  }

  // This method will perform the actual read operation.
  // First the client will create a ReadReqMsg and will push himself into the responsePath.
  // Then it will send the ReadReqMsg to the parent node.
  private void doReadReq(Integer key){
    ReadReqMsg msg = new ReadReqMsg(key);
    msg.responsePath.push(getSelf());
    sendMessage(msg);
    System.out.println("Client " + this.id + ";ReadReq;key = " + msg.key + ";");
  }

  // This method will perform the actual write operation.
  // First the client will create a WriteReqMsg specifying himself as originator.
  // Then it will send the WriteReqMsg to the parent node.
  private void doWriteReq(Integer key, Integer value){
    WriteReqMsg msg = new WriteReqMsg(key, value, getSelf());
    sendMessage(msg);
    System.out.println("Client " + this.id + ";WriteReq;key = " + msg.key + ";value="+msg.newValue);
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
