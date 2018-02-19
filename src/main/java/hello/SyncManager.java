package hello;

import com.rabbitmq.client.*;
import org.apache.commons.lang3.SerializationUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeoutException;


public class SyncManager {
    private Connection connection;
    private Channel channel;
    private HashSet<String> queues ;
    private DiskStorageManager storeManager;
    VClock clock;
    private Database db;
    private int node;

    SyncManager(VClock clock, Database mydb, DiskStorageManager manager){
        this.clock = clock;
        this.db = mydb;
        this.storeManager = manager;
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
                String name = ChannelUtils.createReceivingQueueName(i);
                channel.queueDeclare(name, false, false, false, null);
                queues.add(name);
//                System.out.println("Creating sending queue " + name);
            }
        }
    }

    public void stop() throws IOException, TimeoutException {
        channel.close();
        connection.close();
    }

    public void loadFromList(List<Msg> list) throws IOException {
        for(Msg m : list){
            processMsg(m);
            clock = m.getVclock();
        }
    }
///////  Synchronised calls
    public void syncPut(String key, String val) throws IOException {
        DataSent dataSent = new DataSent(Operation.PUT, key, val);
        Msg msg = new Msg(increment(), dataSent);
        msg.setSender(node);
        msg.log();
        int distnode = ConsistentHashingUtils.getNode(key);
        // if its local node, just write locally
        if(distnode == node) {
            dataSent.setOperation(Operation.INCR);
            msg.setData(dataSent);
            for(String q : queues){
                send(msg, q);
            }
            storeManager.write(msg);
            db.put(key, val);
        }else{
            String n = ChannelUtils.createReceivingQueueName(distnode);
            System.out.println("Sending put msg to " + n);
            send(msg, n);
        }
        System.out.println("[LOG] Updating '" + dataSent.getKey() + "', '" + dataSent.getVal() + "'");
    }
    public void syncRemove(String key) throws IOException {
        DataSent dataSent = new DataSent(Operation.REMOVE, key, "");
        Msg msg = new Msg(increment(), dataSent);
        msg.setSender(node);
        msg.log();
        int distnode = ConsistentHashingUtils.getNode(key);
        if(distnode == node){
            dataSent.setOperation(Operation.INCR);
            msg.setData(dataSent);
            for(String q : queues){
                send(msg, q);
            }
            storeManager.write(msg);
            db.remove(key);
        }else{
            String n = ChannelUtils.createReceivingQueueName(distnode);
            send(msg, n);
        }
        System.out.println("[LOG] Removing '" + dataSent.getKey() + "'");
    }
///////  Dirty calls
    public String syncRead(String key) throws IOException, InterruptedException {
        int distnode = ConsistentHashingUtils.getNode(key);
        System.out.println("node is " + node + " and dist is " + distnode);
        if(distnode == node) {
            return db.get(key);
        }else {
            DataSent dataSent = new DataSent(Operation.READ, key, "");
            Msg msg = new Msg(increment(), dataSent);
            msg.setSender(node);
            String n = ChannelUtils.createReceivingQueueName(distnode);
            System.out.println("Sending read msg to " + n);
            send(msg, n);
            return waitForResponse(msg);
        }
    }

    private String waitForResponse(Msg msg)
            throws IOException, InterruptedException {
        String queue = ChannelUtils.createReceivingQueueName(node);
        Msg res = null;
        QueueingConsumer consumer = new QueueingConsumer(channel);
        int i = 1;
        System.out.println("Waiting for read...");
        channel.basicConsume(queue+"res",true,consumer);
        while (i < 2) {
            QueueingConsumer.Delivery delivery = consumer.nextDelivery();
            res = SerializationUtils.deserialize(delivery.getBody());
            System.out.println("Res is " + res.getData().getVal());
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            i++;
        }

        return res.getData().getVal();
    }

    public HashMap<String, String> dirtyList(){
        return db.getDb();
    }
////////

    public void send(Msg msg, String destinationQueue) throws IOException {
        byte [] byteMessage = SerializationUtils.serialize(msg);
        channel.basicPublish("", destinationQueue, null, byteMessage);
    }

    public void handleSync(Msg msg) throws VClockException, IOException {
        System.out.println("Got new msg");
        if(msg.getSender() == node){
         // db.Msg from myself, ignore
            System.out.println("ERROR - msg from myself");
        }else {
            msg.log();
            if(clock.compareTo(msg.getVclock()) <= 0){
//                increment();
                System.out.println("Compared...");
                clock = mergeVClocks(msg.getVclock());
                processMsg(msg);
                storeManager.write(msg);
            }else{
                System.out.println("************************************");
                System.out.println("Discarding update - old vector clock");
                System.out.println("************************************");
            }
        }
    }

    private void processMsg(Msg msg){
        System.out.println("Got msg to process");
     switch(msg.getData().getOperation()) {
         case PUT:
             handlePut(msg.getData());
         case READ:
             handleRead(msg);
             break;
         case REMOVE:
             handleRemove(msg.getData());
             break;
         case INCR:
             justIncr(msg);
             break;
         case NONE:
             break;
     }
    }

    private void justIncr(Msg m){
        clock = mergeVClocks(m.getVclock());
    }

    private void handlePut(DataSent data){
        System.out.println("Putting " + data.getKey() + " with " + data.getVal());
        db.put(data.getKey(), data.getVal());
    }

    private void handleRemove(DataSent data){
        db.remove(data.getKey());
    }

    private void handleRead(Msg msg){
        String key = msg.getData().getKey();
        String val = db.get(key);
        System.out.println("Handle read called for key " + key + " and val " + val);
        DataSent dataSent = new DataSent(Operation.READ, key, val);
        Msg msg2 = new Msg(increment(), dataSent);
        msg2.setSender(node);
        String dest = ChannelUtils.createReceivingQueueName(msg.getSender());
        try {
            send(msg2, dest+"res");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }



    public VClock mergeVClocks(VClock vc){

        VClock merged = new VClock(Main.NODES_NUMBER);
        for(int i = 0;i< Main.NODES_NUMBER;i++){
            int a = clock.get(i);
            int b = vc.get(i);
            int max = (a <= b)? b : a;
            merged.set(i, max);
        }
        System.out.println("Merging " + clock.logString() + " with " +
                vc.logString() + " = " + merged.logString());
        return merged;
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
