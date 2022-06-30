package it.unitn.ds1.CavadaBrighenti.FinalProject.Messages;

// Sent to a client to trigger the write process.
// This message specify the key of the item that the client need to write and the newValue to set to the item.
// This message is used mostly for debug purposes.
public class DoWriteMsg extends Message{
    public final int key;   // key of item to be written
    public final int newValue; // newValue to set

    public DoWriteMsg(int key, int newValue) {
        this.key=key;
        this.newValue=newValue;
    }
}
