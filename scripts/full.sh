#!/usr/bin/env bash

if [ -z "$TFD_HOME" ]; then
 TFD_HOME=/home/iorixxx/TFD_HOME
fi

for set in ROB04 MQ09 CW09A CW09B CW12B; do

    if [ -d  "$TFD_HOME"/${set}/excels ]; then
        rm -rf  "$TFD_HOME"/${set}/excels
    fi

    mkdir -p "$TFD_HOME"/${set}/excels
done



for optimize in NDCG20 NDCG100 ERR20 ERR100 MAP; do
  for report in NDCG20 NDCG100 ERR20 ERR100 MAP; do

 for test in ROB04 MQ09 CW09A CW09B CW12B; do
for train in ROB04 MQ09 CW09A CW09B CW12B; do
        ./run.sh X -test "$test" -train "$train" -optimize "$optimize" -report "$report" &
    done
    wait
done

done
done