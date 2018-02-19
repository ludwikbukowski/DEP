package hello;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;


public class Main {

    private static VClock clock;
    private static SyncManager manager;
    private static Database mydb = new MyDB(manager);
    private static Connection connection;
    public final static Integer NODES_NUMBER = 3;
    private static DiskStorageManager disk = new DiskStorageManager();

    public static int node = -1;

    @Value("${rabbit.host}")
    private static String rabbitHost;

    public static void start() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        clock = new VClock(NODES_NUMBER);
        manager = new SyncManager(clock, mydb, disk);
        factory.setHost(rabbitHost);
        connection = factory.newConnection();
    }

    public static SyncManager getManager(){
        return manager;
    }

    public static SyncManager run(String[] args) throws IOException, TimeoutException {
        node = Integer.parseInt(args[0]);
        start();
        manager.setNode(node);
        manager.setConnection(connection);
        manager.start();
        disk.setNode(node);
        try {
            System.out.println("Loading data from local storage...");
            List<Msg> list = disk.read(node);
            manager.loadFromList(list);
        }catch(EOFException e){
            // No storage to read
        }catch(FileNotFoundException e2){
            // no storage to read
        }
        System.out.println("Starting node " + node);
        // Start listening on specific channels
        new Listener(clock, connection, manager, mydb, node).run();


        while(true) {
            System.out.println("Run command...");
            Scanner scanner = new Scanner(System.in);
            String input = scanner.nextLine();
            parseCommand(input);
        }

//        return manager;
    }
    static void parseCommand(String input) throws IOException, TimeoutException {
        String [] args = input.split("\\s+");
        if(args[0].equals("add") && args.length == 3){
            String key = args[1];
            String val = args[2];
            System.out.println("Adding " + key  +" : " + val);
            manager.syncPut(key, val);
        }else if (args[0].equals("remove")){
            String key = args[1];
            manager.syncRemove(key);
            System.out.println("Removing " + key + " from db");
        }else if (args[0].equals("list")){
            HashMap<String, String> wholeDb =  manager.dirtyList();
            Iterator<String> keys = wholeDb.keySet().iterator();
            Iterator<String> vals = wholeDb.values().iterator();
            System.out.println("Listing all database:");
            while(keys.hasNext() && vals.hasNext()){
                System.out.println(keys.next() + ":" + vals.next());
            }
        }else if(args[0].equals("get") && args.length == 2){
            String key = args[1];
            String val = null;
            try {
                val = manager.syncRead(key);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Value for " + key + " is " + val);
        }else if(args[0].equals("stop")){
            manager.stop();
            System.out.println("Stopping...");
        }else if(args[0].equals("clear")){
            disk.clear();
            mydb.clear();
            clock = new VClock(NODES_NUMBER);
            System.out.println("Clearing disk storage and RAM storage locally...");
        }
    }

}
