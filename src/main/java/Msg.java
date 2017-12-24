import java.io.Serializable;

/**
 * Created by ludwikbukowski on 24/12/17.
 */
public class Msg implements Serializable {
    private String key = "";
    private String val = "";
    private Operation operation;

    Msg(Operation operation, String k, String v){
        this. operation = operation;
        this.key = k;
        this.val = v;
    }

    Msg(){
        this.operation = Operation.NONE;
        this.key = "";
        this.val = "";
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getVal() {
        return val;
    }

    public void setVal(String val) {
        this.val = val;
    }
}


