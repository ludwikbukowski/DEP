# Project from Distributed Enabling platforms - URL Shortener with distributed database.
## [WORK IN PROGRESS]
## Prepare
Project is written in Java and use maven for dependency management.
You need RabbitMQ running on your localhost machine.
Recommended docker container:
```
$ docker run -d -p 5672:5672 -p 15672:15672  --name rabbitmq rabbitmq
```
## Run
Run java node with `Main` as a main class with one integer argument which points out the id of the node.
In console you should see prompt request for a command:
```
Creating sending queue receive0
Creating sending queue receive1
Starting node 2
Creating receiving queue receive2
 [*] Waiting for messages. To exit press CTRL+C
Run command...
$
```
Possible commands:
* add KEY VALUE - adds key value pair to database
* get KEY - reads value for specified key. If no record found, null returned
* remove KEY - removes key-value pair from the db  
* list - lists all keys and corresponding values from the db

`add` and `remove` commands are synchronised - the update is broadcasted to the other nodes.
`get` and `list` are reads just from local memory (not synchronised with other nodes)

## Architecture
Below simple diagram explaining connections between nodes and their RabbitMQ queues:


![alt text](https://github.com/ludwikbukowski/DEP/blob/master/Queues.png)


Each node is consumer of his own RabbitMQ queue.
When node wants to notify other nodes about some update, it puts messages to other nodes' queues.


Every message sent between nodes are objects of [Msg class](https://github.com/ludwikbukowski/DEP/blob/master/src/main/java/Msg.java). It contains sender and receivers ids (optional), data(key, value, operation type) and [vectorClock object](https://github.com/ludwikbukowski/DEP/blob/master/src/main/java/VClock.java)


