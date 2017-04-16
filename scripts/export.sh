#!/usr/bin/env bash

if [ -z "$TFD_HOME" ]; then
 TFD_HOME=/home/iorixxx/TFD_HOME
fi

for set in MQ09 CW09A CW09B CW12B; do
     echo "creating excel files for $set ..."
    ./run.sh T2T -collection "$set"
    ./run.sh Q2Q -collection "$set"
    ./run.sh Export -collection "$set"
done