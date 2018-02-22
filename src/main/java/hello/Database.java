package hello;

import java.util.HashMap;

/**
 * Created by ludwikbukowski on 24/12/17.
 */
public interface Database {
    boolean put(String key, String value);
    String get(String Key);
    boolean remove(String key);
    int size();
    boolean clear();
    HashMap<String, String> getDb();
}
