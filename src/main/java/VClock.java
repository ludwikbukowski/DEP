import java.io.Serializable;

public class VClock implements Serializable{
    public Integer [] clocks = new Integer[Main.NODES_NUMBER];

    public void log(){
        System.out.print("[VClock]: [");
        for(int i =0;i<Main.NODES_NUMBER;i++)
            System.out.print(clocks[i] + ";");
        System.out.println("]");
    }
    VClock(Integer nodes){
        for(int i=0; i< nodes; i++){
            clocks[i] = 0;
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
}
