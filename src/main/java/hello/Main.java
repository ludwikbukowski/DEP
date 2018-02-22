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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeoutException;


public class Main {

    private static VClock clock;
    private static SyncManager manager;
    private static Database mydb = new MyDB(manager);
    private static Connection connection;
    public final static Integer NODES_NUMBER = 3;
    private static DiskStorageManager disk = new DiskStorageManager();
    private static BlockingQueue readingqueue = new ArrayBlockingQueue<Msg>(1);

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
        manager.setReadingqueue(readingqueue);
        manager.start();
        disk.setNode(node);

        //
        // Disk storage disabled
        //

//        try {
//            System.out.println("Loading data from local storage...");
//            List<Msg> list = disk.read(node);
////            manager.loadFromList(list);
//        }catch(EOFException e){
////             No storage to read
//        }
        System.out.println("Starting node " + node);
        // Start listening on specific channels

        new Listener(clock, connection, manager, mydb, node).run();
        return manager;
    }

}
