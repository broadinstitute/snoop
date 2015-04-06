#!/bin/bash

set -e

java -jar $(find /snoop | grep 'snoop.*\.jar')
