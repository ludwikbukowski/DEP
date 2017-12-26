import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;


public class Main {

    private static VClock clock;
    private static SyncManager manager;
    private static MyDB mydb = new MyDB(manager);
    private static Connection connection;
    public final static Integer NODES_NUMBER = 3;

    public static void start() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        clock = new VClock(NODES_NUMBER);
        manager = new SyncManager(clock, mydb);
        factory.setHost("localhost");
        connection = factory.newConnection();
    }

    public static void main(String[] args) throws IOException, TimeoutException {
        int node = Integer.parseInt(args[0]);
        start();
        manager.setNode(node);
        manager.setConnection(connection);
        manager.start();
        System.out.println("Starting node " + node);
        // Start listening on specific channels
        new Listener(clock, connection, manager, mydb, node).run();
        while(true) {
            System.out.println("Run command...");
            Scanner scanner = new Scanner(System.in);
            String input = scanner.nextLine();
            parseCommand(input);
        }
    }
    static void parseCommand(String input) throws IOException {
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
            String val = manager.dirtyRead(key);
            System.out.println("Value for " + key + " is " + val);
        }
    }

}
