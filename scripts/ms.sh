#!/usr/bin/env bash

if [ -z "$TFD_HOME" ]; then
 TFD_HOME=/home/iorixxx/TFD_HOME
fi

# Script for Model Selection (MS)

echo 'Model,k,Set,Measure,Accuracy,Anchor'> ms.csv

for tag in KStemAnalyzerAnchor KStemAnalyzer; do
for set in MQ09 CW09A CW09B CW12B; do
    echo "starting MS with set = $set and tag = $tag ..."
    ./run.sh MS -tag "$tag" -collection "$set" -metric NDCG100  2>> ms.csv &
    ./run.sh MS -tag "$tag" -collection "$set" -metric MAP 2>> ms.csv &
done
wait
done