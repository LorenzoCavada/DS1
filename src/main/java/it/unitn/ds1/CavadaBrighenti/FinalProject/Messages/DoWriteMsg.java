package it.unitn.ds1.CavadaBrighenti.FinalProject.Messages;

public class DoWriteMsg extends Message{
    public final int key;   // key of item to be written
    public final int newValue;

    public DoWriteMsg(int key, int newValue) {
        this.key=key;
        this.newValue=newValue;
    }
}
