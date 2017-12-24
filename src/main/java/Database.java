import java.util.HashSet;
import java.util.Set;

/**
 * Created by ludwikbukowski on 24/12/17.
 */
public interface Database {
    boolean put(String key, String value);
    MyData get(String Key);
    boolean remove(String key);
}
