package hello;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@SpringBootApplication
public class Application {
    public static SyncManager manager;
    public static void main(String[] args) throws IOException, TimeoutException {
        if(args.length < 1){
            System.out.println("Set the nodename in parameters");
        }else {
            manager = Main.run(args);
            SpringApplication.run(Application.class, args);
        }
    }

}
