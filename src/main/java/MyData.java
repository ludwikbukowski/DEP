/**
 * Created by ludwikbukowski on 24/12/17.
 */
public class MyData {
    private String key;
    private String value;

    MyData(String k, String v){
        this.key = k;
        this.value = v;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
