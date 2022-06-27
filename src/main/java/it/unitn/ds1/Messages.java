package it.unitn.ds1;

import akka.actor.ActorRef;

import java.io.Serializable;
import java.util.Stack;

public class Messages {

    public enum typeCache{L1, L2};
    public static class ReadReqMsg implements Serializable {
        public final int key;   // key of requested item
        public Stack<ActorRef> responsePath;

        public ReadReqMsg(int key) {
            this.key=key;
            this.responsePath=new Stack<>();
        }
    }

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

    public static class WriteReqMsg implements Serializable {
        public final int key;   // key of item to be written
        public final int newValue; //new value of item
        public ActorRef originator; //originato of request

        public WriteReqMsg(int key, int newValue, ActorRef originator) {
            this.key=key;
            this.newValue=newValue;
            this.originator=originator;
        }
    }

    public static class WriteConfirmMsg implements Serializable {
        public final int key;   // key of written item

        public WriteConfirmMsg(int key) {
            this.key=key;
        }
    }

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

}
