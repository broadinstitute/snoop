# snoop

1. The job and workflow execution service for the Prometheus project
1. Snoop, Hit woman (executor) from *The Wire*:

![](http://i.telegraph.co.uk/multimedia/archive/01846/snoopSUM_1846833c.jpg)

## Installation

```bash
$ sbt assembly
```

## Running

```bash
$ java -jar target/scala-2.11/snoop-assembly-0.1.jar
[INFO] [02/27/2015 15:47:44.588] [on-spray-can-akka.actor.default-dispatcher-4] [akka://on-spray-can/user/IO-HTTP/listener-0] Bound to localhost/127.0.0.1:8080
```
