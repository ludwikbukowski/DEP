import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.apache.commons.lang3.SerializationUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.TimeoutException;


public class SyncManager {
    private Connection connection;
    private Channel channel;
    private HashSet<String> queues ;
    VClock clock;
    private int node;

    SyncManager(VClock clock){
        this.clock = clock;
    }

    public VClock increment(){
        clock.update(node);
        return clock;
    }

    public void start() throws IOException, TimeoutException {
        queues = new HashSet<String>();
        channel = connection.createChannel();
        startSendingQueues();
    }

    public void startSendingQueues() throws IOException, TimeoutException {
        Integer count = Main.NODES_NUMBER;
        for(int i=0;i<count;i++){
            if(i!=node){
                String name = ChannelUtils.createQueueName(node, i);
                channel.queueDeclare(name, false, false, false, null);
                queues.add(name);
                System.out.println("Creating sending queue " + name);
            }
        }
    }

    public void stop() throws IOException, TimeoutException {
        channel.close();
        connection.close();
    }

    public void syncPut(String key, String val) throws IOException {
        DataSent dataSent = new DataSent(Operation.PUT, key, val);
        Msg msg = new Msg(increment(), dataSent);
        msg.setSender(node);
        msg.log();
        for(String q : queues){
            System.out.println("sending msg to " + q);
            send(msg, q);
        }
        System.out.println(" [x] Updating '" + dataSent.getKey() + "', '" + dataSent.getVal() + "'");
    }

    public void send(Msg msg, String destinationQueue) throws IOException {
        byte [] byteMessage = SerializationUtils.serialize(msg);
        channel.basicPublish("", destinationQueue, null, byteMessage);
    }

    public void handleSync(Msg msg){
        if(msg.getSender() == node){
         // Msg from myself, ignore
        }else {
            System.out.println("Receive " + msg.getData().getKey() + ":" + msg.getData().getVal());
            increment();
            msg.log();
        }
    }

    public int getNode() {
        return node;
    }

    public void setNode(int node) {
        this.node = node;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }
}
