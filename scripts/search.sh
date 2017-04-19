#!/usr/bin/env bash

if [ -z "$TFD_HOME" ]; then
 TFD_HOME=~/TFD_HOME
fi

for set in ROB04 GOV2 MQ09 CW09A CW09B CW12B; do
    ./run.sh Searcher -collection "$set"
done

./eval.sh
./mq.sh
./tt.sh
./rob.sh

for set in MQ09 CW09A CW09B CW12B; do
    ./run.sh Spam -collection "$set"
done

./spam.sh