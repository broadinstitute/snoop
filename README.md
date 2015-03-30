# Snoop

1. The job and workflow execution service for the Prometheus project
1. Snoop, Hit woman (executor) from *The Wire*:

![](http://i.telegraph.co.uk/multimedia/archive/01846/snoopSUM_1846833c.jpg)

Installation
------------

```bash
$ sbt assembly
```

Running
-------

```bash
$ java -jar target/scala-2.11/snoop-assembly-0.1.jar
[INFO] [02/27/2015 15:47:44.588] [on-spray-can-akka.actor.default-dispatcher-4] [akka://on-spray-can/user/IO-HTTP/listener-0] Bound to localhost/127.0.0.1:8080
```

Testing
-------
Using in-memory db H2

```bash
$ sbt test -Denv.type=test
```
Snoop in Docker
---------------

### Build Container

The root of the repository contains a `Dockerfile` which builds off of the [baseimage](http://phusion.github.io/baseimage-docker/) Docker image.  Running a `docker build .` from this directory will create an image with the following software packages installed:

* Oracle JDK 6
* Scala 2.11.4
* Sbt 0.13.7

Snoop's codebase (i.e. this directory) is included in `/snoop`.  After the JDK/Scala/Sbt stack is installed, an `sbt assembly` command is run from `/snoop`.  The resulting JAR file is copied to `/snoop`.  See `docker/install.sh` for how this process works.

the baseimage Docker image comes with an init process that uses [runit](http://smarden.org/runit/) to manage running and supervising processes on the system.  `/etc/service/snoop` contains a file `run` which is used to launch Snoop.  This file is the same as `docker/run.sh` in this repository.

To build the container:

```bash
$ docker build .
...
Successfully built 8f0f2c9d864a
```

### Run Container

```bash
$ docker run -d -p 8080:8080 8f0f2c9d864a
b337054d150af521f9d533efdf82a6154ee13ae93b73180aa99c3c854c725b8e
```

> **Note**: this image should always be launched in the background

Get a shell to the container by launching a Bash process on the container:

```bash
$ docker exec -t -i <CONTAINER ID> bash -l
root@a309813b3de6:/#
```

### More information

For more information, like how to enable SSH to the container, look at [Running Herc in Docker](https://github.com/broadinstitute/herc/blob/master/docs/Herc-in-Docker.md).  Since they use the same base image, most of the stuff there is relevant to both projects
