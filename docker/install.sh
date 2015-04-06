#!/bin/bash

set -e

SNOOP_DIR=$1
cd $SNOOP_DIR
sbt assembly
SNOOP_JAR=$(find target | grep 'snoop.*\.jar')
mv $SNOOP_JAR .
sbt clean
