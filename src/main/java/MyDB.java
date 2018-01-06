import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;


public class MyDB implements Database {
    HashMap<String, String> database = new HashMap<String, String>();
    SyncManager manager;

    MyDB(SyncManager manager){
        this.manager = manager;
    }

    public synchronized boolean put(String key, String value) {
        database.put(key, value);
        return true;
    }

    public synchronized String get(String key) {
        return database.get(key);
    }

    public boolean remove(String key) {
        database.remove(key);
        return true;
    }

    public boolean clear() {
        database.clear();
        return true;
    }

    synchronized public HashMap<String, String> getDb(){
        return database;
    }
}
