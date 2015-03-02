#!/bin/bash

SNOOP_DIR=$1

cd $SNOOP_DIR
sbt assembly
