import java.io.Serializable;

/**
 * Created by ludwikbukowski on 24/12/17.
 */
public class DataSent implements Serializable {
    private String key = "";
    private String val = "";
    private Operation operation;

    DataSent(Operation operation, String k, String v){
        this. operation = operation;
        this.key = k;
        this.val = v;
    }

    DataSent(){
        this.operation = Operation.NONE;
        this.key = "";
        this.val = "";
    }

    public boolean equals(DataSent dt) throws VClockException{
        if(key.equals(dt.getKey()) && val.equals(dt.getVal())){
            if(operation.equals(dt.getOperation()))
                return true;
        }
        return false;
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

    public Operation getOperation() {
        return operation;
    }
}


