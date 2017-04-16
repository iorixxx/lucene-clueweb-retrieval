#!/usr/bin/env bash

if [ -z "$TFD_HOME" ]; then
 TFD_HOME=/home/iorixxx/TFD_HOME
fi

cat /dev/null > "$TFD_HOME"/excels/tables.tex

for tag in KStemAnalyzerAnchor KStemAnalyzer; do
for set in CW09A_CW12B MQ09 CW09A CW09B CW12B; do
#   ./run.sh Y -tag "$tag" -collection "$set" -optimize NDCG20
    ./run.sh Y -tag "$tag" -collection "$set" -optimize NDCG100
    ./run.sh Y -tag "$tag" -collection "$set" -optimize MAP
done
done
