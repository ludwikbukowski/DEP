package hello;

import java.io.Serializable;

/**
 * Created by ludwikbukowski on 24/12/17.
 */
public class Msg implements Serializable {
    private final VClock clock;
    private DataSent data;
    private int sender = -1;
    private int receiver = -1;

    public Msg(final VClock c, DataSent data) {
        this.clock = c;
        this.data = data;
    }

    public VClock getVclock() {
        return clock;
    }

    public DataSent getData() {
        return data;
    }

    public void setData(DataSent data) {
        this.data = data;
    }

    // For development use only

    void log(){
        System.out.print("[LOG] db.VClock: [");
        for(int i =0;i< Main.NODES_NUMBER; i++){
            System.out.print(clock.clocks[i] + ";");
        }
        System.out.println("], Sender: " + sender +", data sent : " + data.getKey() + ":"+data.getVal());
    }

    public int getSender() {
        return sender;
    }

    public void setSender(int sender) {
        this.sender = sender;
    }

    public int getReceiver() {
        return receiver;
    }

    public void setReceiver(int receiver) {
        this.receiver = receiver;
    }

    public boolean equals(Msg msg2) throws VClockException {
        if(getVclock().compareTo(msg2.getVclock()) != 0)
            return false;
        if(getData().equals(msg2.getData())){
            if(getSender() == msg2.getSender() && getReceiver() == msg2.getReceiver())
                return true;
        }
        return false;
    }
}
