package hello;

import java.io.Serializable;
import java.util.ArrayList;

public class VClock implements Serializable{
    public Integer [] clocks = new Integer[Main.NODES_NUMBER];

    public void log(){
        System.out.println(logString());
    }
    public String logString(){
        StringBuilder b = new StringBuilder();
        b.append("[db.VClock]: [");
        for(int i =0;i<Main.NODES_NUMBER;i++)
            b.append(clocks[i] + ";");
        b.append("]");
        return b.toString();
    }
    public VClock(Integer nodes){
        for(int i=0; i< nodes; i++){
            clocks[i] = 0;
        }
    }
    public VClock(ArrayList<Integer> vals){
        for(int i=0; i< vals.size(); i++){
            clocks[i] = vals.get(i);
        }
    }
    synchronized public boolean update(int index){
        if(clocks[index] != null) {
            clocks[index] = clocks[index] + 1;
            return true;
        }
        else
            return false;
    }

    public boolean isResetRequest(){
        for(Integer i : clocks){
            if(i>0){
                return false;
            }
        }
        return true;
    }

    public Integer get(int index){
        return clocks[index];
    }

    synchronized public boolean set(int index, int value){
        if(clocks[index] != null) {
            clocks[index] = value;
            return true;
        }
        else
            return false;
    }
    // not implementing Comparator<T> class due to exception.
    // TODO handle conflits!
    public int compareTo(VClock vc) throws VClockException{
        int res = 0;
        for(int i = 0;i < Main.NODES_NUMBER;i++){
            if(get(i) < vc.get(i)){
                if(res == -1){
                    // continue
                }else if(res == 0){
                    res = -1;
                }else {
                    System.out.println("******************************");
                    System.out.println("[INTERNAL ERROR] Data conflict");
                    System.out.println("[COMPARING] " + logString() + " with " + vc.logString());
                    System.out.println("******************************");
                    throw new VClockException();
                }
            }else if(get(i) > vc.get(i)){
                if(res == 1){
                    // continue
                }else if(res == 0){
                    res = 1;
                }else {
                    System.out.println("******************************");
                    System.out.println("[INTERNAL ERROR] Data conflict.");
                    System.out.println("[COMPARING] " + logString() + " with " + vc.logString());
                    System.out.println("******************************");
                    throw new VClockException();
                }
            }
        }
        return res;
    }
}
