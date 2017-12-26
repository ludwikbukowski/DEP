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
    // not implementing Comparator<T> class due to exception.
    // TODO handle compare conflits!
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
                    System.out.println("Comparing " + get(i) +" vs " + vc.get(i));
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
                    System.out.println("[INTERNAL ERROR] Data conflict");
                    System.out.println("Comparing " + get(i) +" vs " + vc.get(i));
                    System.out.println("******************************");
                    throw new VClockException();
                }
            }
        }
        return res;
    }
}
