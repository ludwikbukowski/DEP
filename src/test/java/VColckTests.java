import org.junit.*;
import org.mockito.Mockito;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;


public class VColckTests {


    @Test
    public void simpleCreate(){
        VClock vclock = new VClock(3);
        assertEquals("simple case", 0, (long)vclock.get(0));
        assertEquals(0, (long )vclock.get(1));
        assertEquals("simple case", 0, (long )vclock.get(2));
    }

    @Test
    public void simpleUpdate(){
        VClock vclock = new VClock(3);
        vclock.update(2);
        vclock.update(2);
        vclock.update(0);
        assertEquals("simple case", 1, (long)vclock.get(0));
        assertEquals(0, (long )vclock.get(1));
        assertEquals("simple case", 2, (long )vclock.get(2));
    }

    @Test
    public void simpleCompare() throws VClockException {
        VClock vclock1 = new VClock(3);
        vclock1.update(2);
        vclock1.update(2);
        vclock1.update(0);
        VClock vclock2 = new VClock(3);
        assertTrue("compare [0,0,2] vs [0,0,0]", vclock1.compareTo(vclock2) == 1);
    }

    @Test
    public void advancedCompare() throws VClockException {
        VClock vclock1 = new VClock(3);
        vclock1.update(0);
        vclock1.update(0);
        vclock1.update(0);
        vclock1.update(1);
        vclock1.update(1);
        vclock1.update(2);
        VClock vclock2 = new VClock(3);
        vclock2.update(0);
        vclock2.update(0);
        vclock2.update(0);
        vclock2.update(1);
        vclock2.update(1);
        vclock2.update(1);
        vclock2.update(1);
        vclock2.update(2);
        vclock2.update(2);
        assertTrue("compare [3,2,1] vs [3,3,2]", vclock1.compareTo(vclock2) == -1);
    }

    @Test
    public void compareEquals() throws VClockException {
        VClock vclock1 = new VClock(3);
        vclock1.update(0);
        vclock1.update(1);
        vclock1.update(1);
        vclock1.update(2);
        VClock vclock2 = new VClock(3);
        vclock2.update(0);
        vclock2.update(1);
        vclock2.update(1);
        vclock2.update(2);
        assertTrue("compare [1,2, 1] vs [1,2,1]", vclock1.compareTo(vclock2) == 0);
    }

    @Test(expected = VClockException.class)
    public void compareError() throws VClockException {
        VClock vclock1 = new VClock(3);
        vclock1.update(0);
        vclock1.update(0);
        vclock1.update(1);
        vclock1.update(1);
        VClock vclock2 = new VClock(3);
        vclock2.update(0);
        vclock2.update(0);
        vclock2.update(0);
        vclock2.update(1);
        vclock2.update(2);
        vclock2.update(2);
        vclock1.compareTo(vclock2);
    }
}
