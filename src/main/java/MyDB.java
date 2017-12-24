import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;


public class MyDB implements Database {
    HashMap<String, String> database = new HashMap<String, String>();
    SyncManager manager;

    MyDB(SyncManager manager){
        this.manager = manager;
    }

    public boolean put(String key, String value) {
        database.put(key, value);

        // Sync with the rest of the nodes
        return true;
    }

    public String get(String key) {
        return database.get(key);
    }

    public boolean remove(String key) {
        database.remove(key);
        // Sync with the rest of the nodes
        return true;
    }
}
