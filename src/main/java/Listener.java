import com.rabbitmq.client.*;
import org.apache.commons.lang3.SerializationUtils;

import java.io.IOException;

public class Listener {

    private final static String QUEUE_NAME = "hello";
    private static SyncManager manager = new SyncManager();
    private static MyDB mydb = new MyDB(manager);

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                    throws IOException {
                Object object =    SerializationUtils.deserialize(body);
                DataSent dataSent = (DataSent) object;
                manager.handleSync(dataSent);
            }
        };
        channel.basicConsume(QUEUE_NAME, true, consumer);
    }
}