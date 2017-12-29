import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
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

    public void loadFromList(List<Msg> list) {
        for(Msg m : list){
            processMsg(m);
        }
    }
///////  Synchronised calls
    public void syncPut(String key, String val) throws IOException {
        DataSent dataSent = new DataSent(Operation.PUT, key, val);
        Msg msg = new Msg(increment(), dataSent);
        msg.setSender(node);
        msg.log();
        storeManager.write(msg);
        for(String q : queues){
            send(msg, q);
        }
        db.put(key, val);
        System.out.println("[LOG] Updating '" + dataSent.getKey() + "', '" + dataSent.getVal() + "'");
    }
    public void syncRemove(String key) throws IOException {
        DataSent dataSent = new DataSent(Operation.REMOVE, key, "");
        Msg msg = new Msg(increment(), dataSent);
        msg.setSender(node);
        msg.log();
        for(String q : queues){
            send(msg, q);
        }
        db.remove(key);
        System.out.println("[LOG] Removing '" + dataSent.getKey() + "'");
    }
///////  Dirty calls
    public String dirtyRead(String key){
        return db.get(key);
    }

    public HashMap<String, String> dirtyList(){
        return db.getDb();
    }
////////

    public void send(Msg msg, String destinationQueue) throws IOException {
        byte [] byteMessage = SerializationUtils.serialize(msg);
        channel.basicPublish("", destinationQueue, null, byteMessage);
    }

    public void handleSync(Msg msg) throws VClockException {
        if(msg.getSender() == node){
         // Msg from myself, ignore
            System.out.println("ERROR - msg from myself");
        }else {
            msg.log();
            if(clock.compareTo(msg.getVclock()) <= 0){
//                increment();
                clock = mergeVClocks(msg.getVclock());
                processMsg(msg);
            }else{
                System.out.println("************************************");
                System.out.println("Discarding update - old vector clock");
                System.out.println("************************************");
            }
        }
    }

    private void processMsg(Msg msg){
     switch(msg.getData().getOperation()){
         case PUT:
             handlePut(msg.getData());
         case READ:
            // handleRead(msg);
             break;
         case REMOVE:
             handleRemove(msg.getData());
             break;
         case NONE:
             break;
     }
    }

    private void handlePut(DataSent data){
        db.put(data.getKey(), data.getVal());
    }

    private void handleRemove(DataSent data){
        db.remove(data.getKey());
    }



    public VClock mergeVClocks(VClock vc){
        VClock merged = new VClock(Main.NODES_NUMBER);
        for(int i = 0;i< Main.NODES_NUMBER;i++){
            int a = clock.get(i);
            int b = vc.get(i);
            int max = (a <= b)? b : a;
            merged.set(i, max);
        }
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
