
import org.junit.*;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;

public class DbTests {
    SyncManager mockedManager;
    MyDB db;

    @Before
    public void prepare(){
        mockedManager = Mockito.mock(SyncManager.class);
        db = new MyDB(mockedManager); // MyClass is tested
    }
    @Test
    public void dbPutGet() {
        // when
        String key = "someKey";
        String value = "someValue";
        // then
        db.put(key, value);
        // assert statements
        assertEquals("when I put I need to get!", value, db.get(key));
    }
    @Test
    public void dbOverride() {
        // when
        String key = "someKey";
        String value = "someValue";
        String value2 = "overriden";
        // then
        db.put(key, value);
        db.put(key, value2);
        // assert statements
        assertEquals("always override value", value2, db.get(key));
    }
    @Test
    public void dbRemove() {
        // when
        String key = "someKey";
        String value = "someValue";
        // then
        db.put(key, value);
        db.remove(key);
        // assert statements
        assertEquals("remove is remove", null, db.get(key));
    }

    @Test
    public void getNull() {
        // when
        String key = "someCrazyKey";
        // then
        //// nothing
        // assert statements
        assertEquals("return null", null, db.get(key));
    }

    @Test
    public void dbGetList() {
        // when
        String key1 = "someKey1";
        String value1 = "someValue1";

        String key2 = "someKey2";
        String value2 = "someValue2";

        String key3 = "someKey3";
        String value3 = "someValue3";
        // then
        db.put(key1, value1);
        db.put(key2, value2);
        db.put(key3, value3);
        // assert statements
        HashMap<String, String> res = db.getDb();
        Iterator<String> kit = res.keySet().iterator();
        Iterator<String> vit = res.values().iterator();

        // the order is due to HashMap implementation ...
        assertEquals("return current db list", key3, kit.next());
        assertEquals("return current db list", value3, vit.next());

        assertEquals("return current db list", key2, kit.next());
        assertEquals("return current db list", value2, vit.next());

        assertEquals("return current db list", key1, kit.next());
        assertEquals("return current db list", value1, vit.next());
    }
}