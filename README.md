# DEP
Distributed enabling platform project
# Project from Distributed Enabling platforms - URL Shortener with distributed database.
## [WORK IN PROGRESS]
## Prepare
Project is written in Java and use gradle and maven for dependency management.
The MVC pattern realised thanks to Spring framework.
You need RabbitMQ running on your localhost machine.
Recommended docker container:
```
$ docker run -d -p 5672:5672 -p 15672:15672  --name rabbitmq rabbitmq
```
## Run
Use maven to run spring project. Set argument as node id.
```
$ mvn -q spring-boot:run -Drun.arguments="1"
Loading data from local storage...
Starting node 1

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::        (v1.5.9.RELEASE)
 
                 (...)
```
Then visit `http://localhost:8080/url`. You should see the URL shortener main page.
In order to run another node, please change the port in `./src/main/resources/application.properties` and run node with different node id (up to 3). E.g
```
$ mvn -q spring-boot:run -Drun.arguments="2"
$ ...
```
## Architecture
Below simple diagram explaining connections between nodes and their RabbitMQ queues:


![alt text](https://github.com/ludwikbukowski/DEP/blob/master/Queues.png)


Each node is consumer of his own RabbitMQ queue.
When node wants to notify other nodes about some update, it puts messages to other nodes' queues.


Every message sent between nodes are objects of [Msg class](https://github.com/ludwikbukowski/DEP/blob/master/src/main/java/Msg.java). It contains sender and receivers ids (optional), data(key, value, operation type) and [vectorClock object](https://github.com/ludwikbukowski/DEP/blob/master/src/main/java/VClock.java)


