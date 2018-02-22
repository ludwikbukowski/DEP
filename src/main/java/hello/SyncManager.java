package hello;

import com.rabbitmq.client.*;
import org.apache.commons.lang3.SerializationUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class SyncManager {
    private Connection connection;
    private Channel channel;
    private HashSet<String> queues ;
    private DiskStorageManager storeManager;
    VClock clock;
    private BlockingQueue<Msg> readingqueue;
    private Database db;
    private int node;
    private int totalcount = 0;

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
//        System.out.println("Waiting for read...");
        String queue = ChannelUtils.createReceivingQueueName(node);
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

        int distnode = ConsistentHashingUtils.getNode(key);
        // if its local node, just write locally
        if(distnode == node) {
            dataSent.setOperation(Operation.INCRPUT);
            msg.setData(dataSent);
            msg.log("Local putting");
            for(String q : queues){
                send(msg, q);
            }
            storeManager.write(msg);
            db.put(key, val);
        }else{
            msg.log("Distributed putting");
            String n = ChannelUtils.createReceivingQueueName(distnode);
            notifyAllIncr(n, Operation.INCRPUT);
            send(msg, n);
        }
        totalcount++;
//        System.out.println("[LOG] Updating '" + dataSent.getKey() + "', '" + dataSent.getVal() + "'");
    }

    private void notifyAllIncr(String except, Operation op){
        DataSent dataSent = new DataSent(op, "", "");
        Msg msg = new Msg(clock, dataSent);
        msg.setSender(node);
        HashSet<String> queues2 = new HashSet<String>(queues);
        queues2.remove(except);
        for(String q : queues2){
            try {
                send(msg, q);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void syncRemove(String key) throws IOException {
        DataSent dataSent = new DataSent(Operation.REMOVE, key, "");
        Msg msg = new Msg(increment(), dataSent);
        msg.setSender(node);
        int distnode = ConsistentHashingUtils.getNode(key);
        if(distnode == node){
            dataSent.setOperation(Operation.INCRREM);
            msg.setData(dataSent);
            msg.log("Local remove");
            for(String q : queues){
                send(msg, q);
            }
            storeManager.write(msg);
            db.remove(key);
        }else{
            msg.log("Distributed remove");
            String n = ChannelUtils.createReceivingQueueName(distnode);
            notifyAllIncr(n, Operation.INCRREM);
            send(msg, n);
        }
        totalcount--;
//        System.out.println("[LOG] Removing '" + dataSent.getKey() + "'");
    }
///////  Dirty calls
    public String syncRead(String key) throws IOException, InterruptedException {
        int distnode = ConsistentHashingUtils.getNode(key);
//        System.out.println("node is " + node + " and dist is " + distnode);
        if(distnode == node) {
            return db.get(key);
        }else {
            DataSent dataSent = new DataSent(Operation.READ, key, "");
            Msg msg = new Msg(clock, dataSent);
            msg.setSender(node);
            String n = ChannelUtils.createReceivingQueueName(distnode);
//            System.out.println("Sending read msg to " + n);
            msg.log("Sent read request");
            send(msg, n);
            return waitForResponse(msg);
        }
    }

    private String waitForResponse(Msg msg)
            throws IOException, InterruptedException {
//        System.out.println("Waiting for read response...");
        Msg res = readingqueue.poll(10, TimeUnit.SECONDS);
        readingqueue.clear();
        msg.log("Got read response from the queue");
        return res.getData().getVal();
    }

    private HashMap<String, String> waitForResponses(Msg msg, int count)
            throws IOException, InterruptedException {
        HashMap<String, String> res = new HashMap<>();
        for(int i =0;i<count;i++) {
            Msg msgres = readingqueue.poll(10, TimeUnit.SECONDS);
            res.put(msgres.getData().getKey(), msgres.getData().getVal());
            msg.log("Got read response from the queue");
        }
        readingqueue.clear();
        return res;
    }

    public HashMap<String, String> dirtyList(){
        return db.getDb();
    }

    public HashMap<String, String> syncList() throws IOException, InterruptedException {
        DataSent ds = new DataSent(Operation.READALL, "", "");
        Msg msg = new Msg(clock, ds);
        msg.setSender(node);
        msg.log("Read all request sent. Total count: " + totalcount);
        for(String q : queues){
            try {
                send(msg, q);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        int othercount = totalcount - db.size();
        HashMap<String, String> others = waitForResponses(msg, othercount);
        HashMap<String, String> res = dirtyList();
        res.putAll(others);
        return res;
    }
////////

    public void send(Msg msg, String destinationQueue) throws IOException {
        byte [] byteMessage = SerializationUtils.serialize(msg);
        channel.basicPublish("", destinationQueue, null, byteMessage);
    }

    public void handleSync(Msg msg) throws VClockException, IOException {
//        System.out.println("Got new msg");
        if(msg.getSender() == node){
         // db.Msg from myself, ignore
            System.out.println("ERROR - msg from myself");
        }else {
            if(clock.compareTo(msg.getVclock()) <= 0){
//                increment();
//                System.out.println("Compared...");
                clock = mergeVClocks(msg.getVclock());
                processMsg(msg);
                storeManager.write(msg);
            }else{
                System.out.println("************************************");
                System.out.println("Discarding update - old vector clock");
                clock.log();
                msg.getVclock().log();
                System.out.println("************************************");
            }
        }
    }

    private void processMsg(Msg msg){
//        System.out.println("Got msg to process");
     switch(msg.getData().getOperation()) {
         case PUT:
             handlePut(msg);
             break;
         case READ:
             handleRead(msg);
             break;
         case READRES:
             handleReadRes(msg);
             break;
         case READALL:
             handleReadAll(msg);
             break;
         case REMOVE:
             handleRemove(msg);
             break;
         case INCRPUT:
             justIncr(msg, 1);
             break;
         case INCRREM:
             justIncr(msg, -1);
             break;
         case NONE:
             break;
     }
    }

    private void justIncr(Msg msg, int diff){
        msg.log("Handling incr request");
        totalcount +=diff;
        clock = mergeVClocks(msg.getVclock());
    }

    private void handlePut(Msg msg){
        msg.log("Handling put request");
        totalcount++;
        db.put(msg.getData().getKey(), msg.getData().getVal());
    }

    private void handleRemove(Msg msg){
        msg.log("Handling remove request");
        totalcount--;
        db.remove(msg.getData().getKey());
    }

    private void handleRead(Msg msg){
        msg.log("Got read request");
        String key = msg.getData().getKey();
        String val = db.get(key);
        final DataSent dataSent = new DataSent(Operation.READRES, key, val);
        final Msg msg2 = new Msg(clock, dataSent);
        msg2.setSender(node);
        String dest = ChannelUtils.createReceivingQueueName(msg.getSender());
        try {
            msg2.log("Sending read value");
            send(msg2, dest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleReadAll(Msg msg){
        msg.log("Got read all request");
        HashMap<String, String> hashmap =  db.getDb();
        Iterator<String> keys = hashmap.keySet().iterator();
        Iterator<String> vals = hashmap.values().iterator();
        String dest = ChannelUtils.createReceivingQueueName(msg.getSender());
        while(keys.hasNext() && vals.hasNext()){
            String k = keys.next();
            String v = vals.next();
            DataSent ds = new DataSent(Operation.READRES, k, v);
            Msg m = new Msg(clock, ds);
            m.setSender(node);
            m.log("Sending data...");
            try {
                send(m, dest);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleReadRes(Msg msg){
        try {
            msg.log("Got read response.Putting in the queue");
            readingqueue.put(msg);
        } catch (InterruptedException e) {
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
//        System.out.println("Merging " + clock.logString() + " with " +
//                vc.logString() + " = " + merged.logString());
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

    public BlockingQueue getReadingqueue() {
        return readingqueue;
    }

    public void setReadingqueue(BlockingQueue readingqueue) {
        this.readingqueue = readingqueue;
    }
}
