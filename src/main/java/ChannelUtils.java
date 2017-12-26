import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;

/**
 * Created by ludwikbukowski on 25/12/17.
 */
public class ChannelUtils {
    public static String createReceivingQueueName(Integer a){
        return "receive" + a;
    }

//    public static String createSendingQueueName(Integer a, Integer b){
//        return "receive" + a;
//    }

}
