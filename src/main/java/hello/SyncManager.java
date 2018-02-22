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

    public void start() throws IOException, TimeoutException {
        queues = new HashSet<String>();
        channel = connection.createChannel();
        startSendingQueues();
    }

    public void stop() throws IOException, TimeoutException {
        channel.close();
        connection.close();
    }

    public void startSendingQueues() throws IOException, TimeoutException {
        Integer count = Main.NODES_NUMBER;
        for(int i=0;i<count;i++){
            if(i!=node){
                String name = ChannelUtils.createReceivingQueueName(i);
                channel.queueDeclare(name, false, false, false, null);
                queues.add(name);
            }
        }
    }

    public VClock increment(){
        clock.update(node);
        return clock;
    }

    // disk storage disabled
    //
    //
//    public void loadFromList(List<Msg> list) throws IOException {
//        for(Msg m : list){
//            processMsg(m, true);
//            clock = m.getVclock();
//        }
//    }



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

    private String waitForResponse(Msg msg)
            throws IOException, InterruptedException {
        Msg res = readingqueue.poll(10, TimeUnit.SECONDS);
        readingqueue.clear();
        if(res==null){
            System.out.println("************************************");
            System.out.println("The destination node is down!!!!");
            System.out.println("************************************");
            return null;
        }
        msg.log("Got read response from the queue");
        return res.getData().getVal();
    }

    public HashMap<String, String> dirtyList(){
        return db.getDb();
    }

    public void send(Msg msg, String destinationQueue) throws IOException {
        byte [] byteMessage = SerializationUtils.serialize(msg);
        channel.basicPublish("", destinationQueue, null, byteMessage);
    }

    public void handleMsg(Msg msg) throws VClockException, IOException {
        if(msg.getSender() == node){
            System.out.println("ERROR - msg from myself");
        }else {
            if (msg.getVclock().isResetRequest()) {
                clock = new VClock(Main.NODES_NUMBER);
                System.out.println("************************************");
                System.out.println("Reseting vector clock");
                System.out.println("************************************");
            } else {
                if (clock.compareTo(msg.getVclock()) <= 0) {
                    clock = mergeVClocks(msg.getVclock());
                    processMsg(msg, false);

// Disk storage disabled
//                storeManager.write(msg);

                } else {
                    System.out.println("************************************");
                    System.out.println("Old vector clock");
                    clock.log();
                    msg.getVclock().log();
                    System.out.println("************************************");
                    distributeResetVclock();
                    processMsg(msg, true);
                }
            }
        }
    }

    private void distributeResetVclock(){
        DataSent ds = new DataSent(Operation.RESETCLOCK, "", "");
        ArrayList<Integer> vals = new ArrayList<>();
        vals.add(-1);
        vals.add(-1);
        vals.add(-1);
        VClock specialClock = new VClock(vals);
        Msg msg = new Msg(specialClock, ds);
        for(String q : queues){
            try {
                send(msg, q);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //TODO Use polymorphism
    private void processMsg(Msg msg, boolean failure){
     switch(msg.getData().getOperation()) {
         case PUT:
             handlePut(msg, failure);
             break;
         case READ:
             handleRead(msg, failure);
             break;
         case READRES:
             handleReadRes(msg, failure);
             break;
         case REMOVE:
             handleRemove(msg, failure);
             break;
         case INCRPUT:
             justIncr(msg, 1, failure);
             break;
         case INCRREM:
             justIncr(msg, -1, failure);
             break;
         case NONE:
             break;
     }
    }

    public VClock mergeVClocks(VClock vc){
        VClock merged = new VClock(Main.NODES_NUMBER);
        for(int i = 0;i< Main.NODES_NUMBER;i++) {
            int a = clock.get(i);
            int b = vc.get(i);
            int max = (a <= b) ? b : a;
            merged.set(i, max);
        }
        return merged;
    }

    private void justIncr(Msg msg, int diff, boolean failure){
        if(failure){
            // do nothing
        }else {
            msg.log("Handling incr request");
            totalcount += diff;
            clock = mergeVClocks(msg.getVclock());
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

    public BlockingQueue getReadingqueue() {
        return readingqueue;
    }

    public void setReadingqueue(BlockingQueue readingqueue) {
        this.readingqueue = readingqueue;
    }

/////////////// API
///////////////

public void Put(String key, String val) throws IOException {
    DataSent dataSent = new DataSent(Operation.PUT, key, val);
    Msg msg = new Msg(increment(), dataSent);
    msg.setSender(node);
    int distnode = ConsistentHashingUtils.getNode(key);
    if(distnode == node) {
        dataSent.setOperation(Operation.INCRPUT);
        msg.setData(dataSent);
        msg.log("Local putting");
        for(String q : queues){
            send(msg, q);
        }
        db.put(key, val);
    }else{
        msg.log("Distributed putting");
        String n = ChannelUtils.createReceivingQueueName(distnode);
        notifyAllIncr(n, Operation.INCRPUT);
        send(msg, n);
    }
    totalcount++;
}

    public void Remove(String key) throws IOException {
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
            db.remove(key);
        }else{
            msg.log("Distributed remove");
            String n = ChannelUtils.createReceivingQueueName(distnode);
            notifyAllIncr(n, Operation.INCRREM);
            send(msg, n);
        }
        totalcount--;
    }

public String Read(String key) throws IOException, InterruptedException {
    int distnode = ConsistentHashingUtils.getNode(key);
    if(distnode == node) {
        return db.get(key);
    }else {
        DataSent dataSent = new DataSent(Operation.READ, key, "");
        Msg msg = new Msg(clock, dataSent);
        msg.setSender(node);
        String n = ChannelUtils.createReceivingQueueName(distnode);
        msg.log("Sent read request");
        send(msg, n);
        return waitForResponse(msg);
    }
}



/////////////// Handlers
///////////////

    private void handlePut(Msg msg, boolean failure){
        if(failure){
            // Do not PUT
        }else {
            msg.log("Handling put request");
            totalcount++;
            db.put(msg.getData().getKey(), msg.getData().getVal());
        }
    }

    private void handleRemove(Msg msg, boolean failure){
        if(failure){
            // Do not remove
        }else {
            msg.log("Handling remove request");
            totalcount--;
            db.remove(msg.getData().getKey());
        }
    }

    private void handleRead(Msg msg, boolean _failure){
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

    private void handleReadRes(Msg msg, boolean _failure){
        try {
            msg.log("Got read response.Putting in the queue");
            readingqueue.put(msg);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
