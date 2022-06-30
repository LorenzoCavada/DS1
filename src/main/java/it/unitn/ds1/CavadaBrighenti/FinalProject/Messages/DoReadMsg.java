package it.unitn.ds1.CavadaBrighenti.FinalProject.Messages;

// Sent to a client to trigger the read process.
// This message specify the key of the item that the client need to read.
// This message is used mostly for debug purposes.
public class DoReadMsg extends Message{
    public final int key;   // key of item to be written

    public DoReadMsg(int key) {
        this.key=key;
    }
}
