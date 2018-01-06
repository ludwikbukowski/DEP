import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by ludwikbukowski on 24/12/17.
 */
public interface Database {
    boolean put(String key, String value);
    String get(String Key);
    boolean remove(String key);
    boolean clear();
    HashMap<String, String> getDb();
}
