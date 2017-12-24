import java.io.Serializable;

/**
 * Created by ludwikbukowski on 24/12/17.
 */
public class Msg implements Serializable {
    VClock vclock;
    DataSent data;
}
