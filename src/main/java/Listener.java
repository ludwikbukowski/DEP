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
    private static MyDB mydb;
    private int node;
    private static Connection connection;
    private static Channel channel;
    private static HashSet<String> queues;

    Listener(VClock vc, Connection conn, SyncManager sm, MyDB db, int n){
        this.clock = vc;
        this.connection = conn;
        this.manager = sm;
        this.mydb = db;
        this.node = n;
    }


    public void start() throws IOException, TimeoutException {
        queues = new HashSet<String>();
        channel = connection.createChannel();
        startRecivingQueues();
    }

    public void startRecivingQueues() throws IOException, TimeoutException {
        Integer count = Main.NODES_NUMBER;
        for(int i=0;i<count;i++){
            if(i!=node){
                String name = ChannelUtils.createQueueName(i, node);
                queues.add(name);
                channel.queueDeclare(name, false, false, false, null);
                System.out.println("Creating receiving queue " + name);
            }
        }
    }

    public void stop() throws IOException, TimeoutException {
        channel.close();
        connection.close();
    }

    public static void listen() throws Exception {
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
        for(String q : queues) {
            Consumer consumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                        throws IOException {
                    Object object = SerializationUtils.deserialize(body);
                    Msg msg = (Msg) object;
                    manager.handleSync(msg);
                }
            };
            channel.basicConsume(q, true, consumer);
        }
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