package it.unitn.ds1;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

class Client extends AbstractActor {

  private Random rnd = new Random();

  private final int id;         // ID of the current actor

  private ActorRef parent; //reference for parent

  private List<ActorRef> availableL2; //all the available L2 caches for timeout

  /* -- Message types ------------------------------------------------------- */

  // Start message that informs the client about the L2 caches
  public static class JoinGroupMsg implements Serializable {
    private final List<ActorRef> listOfL2; // list of group members
    private final ActorRef parent;
    public JoinGroupMsg(List<ActorRef> listOfL2, ActorRef parent) {
      this.listOfL2 = Collections.unmodifiableList(new ArrayList<>(listOfL2));
      this.parent=parent;
    }
  }

  /* -- Actor constructor --------------------------------------------------- */

  public Client(int id) {
    this.id = id;
  }

  static public Props props(int id) {
    return Props.create(Client.class, () -> new Client(id));
  }

  /* -- Actor behaviour ----------------------------------------------------- */
  private void onJoinGroupMsg(JoinGroupMsg msg) {
    this.availableL2 = msg.listOfL2;
    this.parent=msg.parent;
    System.out.println("Client " + this.id + ";joined;parent = " + msg.parent + ";");
  }

  private void onReadRespMsg(Messages.ReadRespMsg msg) {
    System.out.println("Client " + this.id + ";ReadResp;key = " + msg.key + ";value = " + msg.value);
  }

  private void onWriteConfirmMsg(Messages.WriteConfirmMsg msg){
    System.out.println("Client " + this.id + ";WriteConfirm;key = " + msg.key + ";confirmed");
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
      .match(JoinGroupMsg.class, this::onJoinGroupMsg)
      .match(Messages.ReadRespMsg.class, this::onReadRespMsg)
      .match(Messages.WriteConfirmMsg.class, this::onWriteConfirmMsg)
      .build();
  }

  private void sendMessage(Serializable m){
    try { Thread.sleep(rnd.nextInt(10)); }
    catch (InterruptedException e) { e.printStackTrace(); }
    parent.tell(m, getSelf());
  }
}
