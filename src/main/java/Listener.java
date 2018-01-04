import com.rabbitmq.client.*;
import org.apache.commons.lang3.SerializationUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.TimeoutException;

public class Listener implements Runnable {

    private final static String QUEUE_NAME = "dbchannel";
    private static VClock clock;
    private static SyncManager manager;
    private static Database mydb;
    private int node;
    private static Connection connection;
    private static Channel channel;
    private static String queue;

    Listener(VClock vc, Connection conn, SyncManager sm, Database db, int n){
        this.clock = vc;
        this.connection     = conn;
        this.manager = sm;
        this.mydb = db;
        this.node = n;
    }


    public void start() throws IOException, TimeoutException {
        channel = connection.createChannel();
        startRecivingQueues();
    }

    public void startRecivingQueues() throws IOException, TimeoutException {
                queue = ChannelUtils.createReceivingQueueName(node);
                channel.queueDeclare(queue, false, false, false, null);
//                System.out.println("Creating receiving queue " + queue);
        }

    public void stop() throws IOException, TimeoutException {
        channel.close();
        connection.close();
    }

    public static void listen() throws Exception {
//        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
            Consumer consumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                        throws IOException {
                    System.out.println("Received...");
                    Object object = SerializationUtils.deserialize(body);
                    Msg msg = (Msg) object;
                    System.out.println("Received msg " + msg.getData().getKey());
                    try {
                        manager.handleSync(msg);
                    } catch (VClockException e) {
                        e.printStackTrace();
                    }
                }
            };
            channel.basicConsume(queue, true, consumer);
    }

    public void run() {
        try {
            start();
            listen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}