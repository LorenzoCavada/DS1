package it.unitn.ds1.CavadaBrighenti.FinalProject.Messages;

public class DoReadMsg extends Message{
    public final int key;   // key of item to be written

    public DoReadMsg(int key) {
        this.key=key;
    }
}
