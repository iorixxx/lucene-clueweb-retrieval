#!/usr/bin/env bash

if [ -z "$TFD_HOME" ]; then
 TFD_HOME=/home/iorixxx/TFD_HOME
fi

for set in ROB04 GOV2 MQ09 CW09A CW09B CW12B; do

    if [ -d  "$TFD_HOME"/${set}/excels ]; then
        rm -rf  "$TFD_HOME"/${set}/excels
    fi

    if [ -d  "$TFD_HOME"/${set}/results ]; then
        rm -rf  "$TFD_HOME"/${set}/results
    fi

    mkdir -p "$TFD_HOME"/${set}/excels
done
