import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;


public class Main {

    private static VClock clock = new VClock(2);
    private static SyncManager manager = new SyncManager(clock);
    private static MyDB mydb = new MyDB(manager);
    private static Connection connection;
    public final static Integer NODES_NUMBER = 3;

    public static void start() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
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
        }else if (args[0].equals("show")){
            System.out.println("Listing all db:");
        }else if(args[0].equals("get") && args.length == 2){
            String key = args[1];
            System.out.println("Value for " + key + " is ");
        }
    }

}
