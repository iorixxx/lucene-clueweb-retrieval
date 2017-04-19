#!/usr/bin/env bash

if [ -z "$TFD_HOME" ]; then
 TFD_HOME=~/TFD_HOME
fi

for set in ROB04 GOV2 MQ09 CW09A CW09B CW12B; do
for bin in 50 100 250 500; do
     echo "running Phi frequency distribution for $set with $bin number of bins..."
    ./run.sh TFDistribution -collection "$set" -task phi -numBins "$bin"
    ./run.sh TFDistribution -collection "$set" -task term -numBins "$bin"
done
done


