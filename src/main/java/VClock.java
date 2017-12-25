import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by ludwikbukowski on 24/12/17.
 */
public class VClock implements Serializable {
    public HashMap<String, Integer> clocks = new HashMap<String, Integer>();

    VClock(Integer nodes){
        for(int i=0; i< nodes; i++){
            clocks.put("node"+i, 0);
        }
    }
    public boolean update(String node){
        if(clocks.get(node) != null) {
            clocks.put(node, clocks.get(node) + 1);
            return true;
        }
        else
            return false;
    }

    public boolean addNew(Integer index){
        String key = "node"+index;
        if(clocks.get(key) == null){
            clocks.put(key, 0);
            return true;
        }else
            return false;
    }

    public void update(Integer index){
        update("node"+index);
    }
}
