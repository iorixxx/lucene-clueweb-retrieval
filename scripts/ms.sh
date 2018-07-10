#!/usr/bin/env bash

if [ -z "$TFD_HOME" ]; then
 TFD_HOME=~/TFD_HOME
fi

# Script for Model Selection (MS)

echo 'Model,k,Set,Measure,Accuracy,Anchor'> ms.csv

for tag in KStemAnchor; do
for set in MQ09 CW09A; do
    echo "starting MS with set = $set and tag = $tag ..."
    ./run.sh MS -tag "$tag" -collection "$set" -metric NDCG100  2>> ms.csv &
    ./run.sh MS -tag "$tag" -collection "$set" -metric NDCG20 2>> ms.csv &
done
wait
done