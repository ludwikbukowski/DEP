package hello;

import org.junit.Before;
import org.junit.Test;

import java.io.EOFException;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by ludwikbukowski on 04/01/18.
 */
public class StorageTests {

    DiskStorageManager storage;
    Integer testInt = 100;

    @Before
    public void prepare(){
        storage = new DiskStorageManager();
        storage.setNode(testInt);
    }

    @Test
    public void writeSimple() throws IOException, VClockException {
        // when
        VClock clock = new VClock(3);
        DataSent data1 = new DataSent(Operation.PUT, "key1", "value1");
        DataSent data2 = new DataSent(Operation.PUT, "key2", "value2");
        clock.update(0);
        Msg msg1 = new Msg(clock, data1);
        Msg msg2 = new Msg(clock, data2);
        storage.write(msg1);
        storage.write(msg2);
        // now read
        List<Msg> list = storage.read(testInt);
        assertEquals("when I put I need to get!", true, list.get(0).equals(msg1));
        assertEquals("when I put I need to get!", false, list.get(0).equals(msg2));
        assertEquals("when I put I need to get!", true, list.get(1).equals(msg2));
    }

    @Test(expected = EOFException.class)
    public void clearTest() throws IOException, VClockException {
        // when
        VClock clock = new VClock(3);
        DataSent data1 = new DataSent(Operation.PUT, "key3", "value3");
        DataSent data2 = new DataSent(Operation.PUT, "key4", "value4");
        clock.update(0);
        Msg msg1 = new Msg(clock, data1);
        Msg msg2 = new Msg(clock, data2);
        storage.write(msg1);
        storage.write(msg2);
        // now read
        List<Msg> list = storage.read(testInt);
        assertEquals("its not empty", false, list.isEmpty());
        storage.clear();
        List<Msg> list2 = storage.read(testInt);
    }
    @Test
    public void advancedReadWrite() throws IOException, VClockException {
        // when
        storage.clear();
        VClock clock = new VClock(3);
        DataSent data1 = new DataSent(Operation.PUT, "key11", "value11");
        DataSent data2 = new DataSent(Operation.PUT, "key22", "value22");
        DataSent data3 = new DataSent(Operation.REMOVE, "key33", "value33");
        DataSent data4 = new DataSent(Operation.READ, "key44", "value44");
        //..
        DataSent data5 = new DataSent(Operation.READ, "key22", "value22");
        //
        clock.update(0);
        Msg msg1 = new Msg(clock, data1);
        storage.write(msg1);
        //
        clock.update(0);
        Msg msg2 = new Msg(clock, data2);
        storage.write(msg2);
        //
        clock.update(0);
        Msg msg3 = new Msg(clock, data3);
        storage.write(msg3);
        //
        clock.update(0);
        Msg msg4 = new Msg(clock, data4);
        storage.write(msg4);
        // now read
        List<Msg> list = storage.read(testInt);
        assertEquals("the size is correct", 4, list.size());
        //
        clock.update(0);
        Msg msg5 = new Msg(clock, data4);
        storage.write(msg5);
        assertEquals("the size is still correct", 5, list.size());
    }

}
