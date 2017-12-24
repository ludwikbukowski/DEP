import java.util.HashSet;
import java.util.Iterator;


public class MyDB implements Database {
    HashSet<MyData> database = new HashSet<MyData>();
    SyncManager manager;

    MyDB(SyncManager manager){
        this.manager = manager;
    }

    public boolean put(String key, String value) {
        MyData mydata = new MyData(key, value);
        return database.add(mydata);
        // Sync with the rest of the nodes
    }

    public MyData get(String key) {
        Iterator<MyData> it = database.iterator();
        while(it.hasNext()){
            MyData e = it.next();
            if(e.getKey().equals(key)){
                return e;
            }
        }
        // Sync with the rest of the nodes
        return null;
    }

    public boolean remove(String key) {
        Iterator<MyData> it = database.iterator();
        while(it.hasNext()){
            MyData e = it.next();
            if(e.getKey().equals(key)){
                database.remove(e);
                return true;
            }
        }
        // Sync with the rest of the nodes
        return false;
    }
}
