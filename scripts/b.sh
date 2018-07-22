#!/usr/bin/env bash

if [ -z "$TFD_HOME" ]; then
 TFD_HOME=~/TFD_HOME
fi


for bin in 5 10 20 30 40 50 100 250 500 750 1000; do
#./run.sh TFDistribution -collection MQ09 -task term -numBins "$bin"
./run.sh Y -tag KStemAnchor -collection MQ09 -optimize NDCG20 -var 1 -spam 10 -numBins "$bin"
done