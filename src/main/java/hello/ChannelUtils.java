package hello;

/**
 * Created by ludwikbukowski on 25/12/17.
 */
public class ChannelUtils {
    public static String createReceivingQueueName(Integer a){
        return "receivequeue" + a;
    }

//    public static String createSendingQueueName(Integer a, Integer b){
//        return "receive" + a;
//    }

}
