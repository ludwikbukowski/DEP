Project from Distributed Enabling platforms - URL Shortener with distributed database.

You need running RabbitMQ instance on your localhost machine.
Recommended docker container:
```
$ docker run -d -p 5672:5672 -p 15672:15672  --name rabbitmq rabbitmq
```

Run java node with one integer argument which points out the id of the node.


Below simple diagram explaining connections between nodes and their RabbitMQ queues:


![alt text](https://github.com/ludwikbukowski/DEP/blob/master/Queues.png)


Each node is consumer of his own RabbitMQ queue.
When node wants to notify other nodes about some update, it puts messages to other nodes' queues.
