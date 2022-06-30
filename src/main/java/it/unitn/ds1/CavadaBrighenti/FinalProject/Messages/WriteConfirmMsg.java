package it.unitn.ds1.CavadaBrighenti.FinalProject.Messages;

// Represent the confirmation of the write operation.
// This will be originated by the L2 cache and sent to the client only if the L2 cache will see that the originator of the request is one of its children
// This message is so originated after have received a RefillMsg and found the originator in the L2 cache's children list
public class WriteConfirmMsg extends Message{
    public final int key;   // key of written item

    public WriteConfirmMsg(int key) {
        this.key=key;
    }
}
