import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;

/**
 * Created by ludwikbukowski on 25/12/17.
 */
public class ChannelUtils {
    public static String createQueueName(Integer a, Integer b){
        return "node" + a + "+" + b;
    }

}
