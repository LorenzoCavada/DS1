package it.unitn.ds1;

import akka.actor.ActorRef;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Messages {

    // represent the type of the cache, Level 1 or Level 2
    public enum typeCache{L1, L2};


    public static class SetChildrenMsg implements Serializable{
        public List<ActorRef> children;   // key of requested item
        public SetChildrenMsg(List<ActorRef> children) {
            this.children=new ArrayList<>(children);
        }
    }

    public static class SetAvailableL2Msg implements Serializable{
        public List<ActorRef> availL2;   // key of requested item
        public SetAvailableL2Msg(List<ActorRef> availL2) {
            this.availL2 =new ArrayList<>(availL2);
        }
    }

    public static class SetParentMsg implements Serializable{
        public ActorRef parent;
        public SetParentMsg(ActorRef parent) {
            this.parent=parent;
        }
    }

    // Represent the request of reading the value of the element identify by the key.
    // The responsePath is a stack of actors that represent the path that the request has followed.
    // This message will be originated by a client which will push its actorRef into the stack and than send the message to the L2 cache.
    // The L2 cache will push its actorRef into the stack and then send the message to the L1 cache.
    // The L1 cache will push its actorRef into the stack and then send the message to the DB.
    public static class ReadReqMsg implements Serializable {
        public final int key;   // key of requested item
        public Stack<ActorRef> responsePath;

        public ReadReqMsg(int key) {
            this.key=key;
            this.responsePath=new Stack<>();
        }
    }
    // Represent the response of a reading request.
    // The response is sent to the actor identified by the responsePath and contain both the key of the requested item and the value of the requested item
    // The responsePath stack will contain the whole chain of actors that the request has crossed.
    // The DB will get by popping the first element of the stack the actorRef to the L1 cache to which the request has been made
    // This L1 cache will pop again the first element of the stack and will get the L2 cache which received the read request from the client
    // This L2 cache will pop again the first element of the stack and will get the client that made the request so will be able to forward the response to him
    public static class ReadRespMsg implements Serializable {
        public final int key;   // key of requested item
        public final int value; //value of requested item
        public Stack<ActorRef> responsePath;

        public ReadRespMsg(int key, int value, Stack<ActorRef> stack) {
            this.key=key;
            this.value=value;
            this.responsePath= (Stack<ActorRef>) stack.clone();
        }
    }

    // Represent the request of writing a new value in the element identify by the key
    // Is also included the originator of the request, this is for sending the confirmation of the write operation
    // This message will be originated by a client and sent to a L2 cache, then will be forwarded to a L1 cache and finally to the DB
    public static class WriteReqMsg implements Serializable {
        public final int key;   // key of item to be written
        public final int newValue; //new value of item
        public ActorRef originator; //originator of request

        public WriteReqMsg(int key, int newValue, ActorRef originator) {
            this.key=key;
            this.newValue=newValue;
            this.originator=originator;
        }
    }

    // Represent the confirmation of the write operation.
    // This will be originated by the L2 cache and sent to the client only if the L2 cache will see that the originator of the request is one of its children
    // This message is so originated after have received a RefillMsg and found the originator in the L2 cache's children list
    public static class WriteConfirmMsg implements Serializable {
        public final int key;   // key of written item

        public WriteConfirmMsg(int key) {
            this.key=key;
        }
    }

    // Represent the request of refilling the cache with a new element. Is initially sent by the server to the L1 cache after a write operation
    // Each L1 cache will then update its cache if the element is already saved in its cache, the L1 cache will then multicast the message to its children
    // Each L2 cache will then update its cache if the element is already saved in its cache, if the originator is one of its children, the L2 cache will send a confirmation to the originator
    public static class RefillMsg implements Serializable {
        public final int key;   // key of item to be written
        public final int newValue; //new value of item
        public ActorRef originator;

        public RefillMsg(int key, int newValue, ActorRef originator) {
            this.key=key;
            this.newValue=newValue;
            this.originator=originator;
        }
    }

    public static class DoReadMsg implements Serializable {
        public final int key;   // key of item to be written

        public DoReadMsg(int key) {
            this.key=key;
        }
    }

    public static class DoWriteMsg implements Serializable {
        public final int key;   // key of item to be written
        public final int newValue;

        public DoWriteMsg(int key, int newValue) {
            this.key=key;
            this.newValue=newValue;
        }
    }

    public static class InternalStateMsg implements Serializable {
        public InternalStateMsg() {}
    }

}
