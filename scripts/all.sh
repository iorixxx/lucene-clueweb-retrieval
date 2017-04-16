#!/usr/bin/env bash

# Intended to use when from scratch installation is required

if [ -z "$TFD_HOME" ]; then
 TFD_HOME=/home/iorixxx/TFD_HOME
fi

# Indexer

# ./run.sh Indexer -collection CW09B
# ./run.sh Indexer -collection CW09B -anchor

# ./run.sh Indexer -collection CW12B
# ./run.sh Indexer -collection CW12B -anchor

# Searcher


./run.sh Searcher -collection CW09A -task param
./run.sh Searcher -collection CW09B -task param
./run.sh Searcher -collection MQ09 -task param
./run.sh Searcher -collection MQE1 -task param
./run.sh Searcher -collection CW12B -task param
./run.sh Searcher -collection GOV2 -task param
./eval.sh parameter
./mq.sh parameter
./mqe1.sh parameter
./tt.sh parameter
./rob.sh parameter

./run.sh Searcher -collection CW09A
./run.sh Searcher -collection CW09B
./run.sh Searcher -collection MQ09
./run.sh Searcher -collection MQE1
./run.sh Searcher -collection CW12B
./run.sh Searcher -collection GOV2
./eval.sh
./mq.sh
./mqe1.sh
./tt.sh
./rob.sh

./run.sh Spam -collection CW09A
./run.sh Spam -collection CW09B
./run.sh Spam -collection MQ09
./run.sh Spam -collection MQE1
./run.sh Spam -collection CW12B

./spam.sh

#./run.sh TFDistribution
#./run.sh StopWord
#./run.sh Stats
#./run.sh VerboseTFDumper
#./run.sh Export
