import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by ludwikbukowski on 24/12/17.
 */
public class VClock implements Serializable {
    private HashMap<String, Integer> clocks = new HashMap<String, Integer>();

    VClock(Integer nodes){
        for(int i=0; i< nodes; i++){
            clocks.put("node"+i, 0);
        }
    }
    public void update(String node){
        clocks.put(node, clocks.get(node) + 1);
    }

    public void update(Integer index){
        update("node"+index);
    }
}
