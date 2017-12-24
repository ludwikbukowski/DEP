import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.lang3.SerializationUtils;

import java.io.IOException;
import java.util.concurrent.TimeoutException;


public class SyncManager {
    private final static String QUEUE_NAME = "dbchannel";
    Connection connection;
    Channel channel;
    public void start() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        connection = factory.newConnection();
        channel = connection.createChannel();
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);

    }
    public void stop() throws IOException, TimeoutException {
        channel.close();
        connection.close();
    }

    public void syncPut(String key, String val) throws IOException {
        Msg msg = new Msg(Operation.PUT, key, val);
        byte [] byteMessage = SerializationUtils.serialize(msg);
        channel.basicPublish("", QUEUE_NAME, null, byteMessage);
        System.out.println(" [x] Updating '" + msg.getKey() + "', '" + msg.getVal() + "'");
    }

    public void handleSync(Msg msg){

    }
}
