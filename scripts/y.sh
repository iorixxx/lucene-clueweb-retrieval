#!/usr/bin/env bash

if [ -z "$TFD_HOME" ]; then
 TFD_HOME=~/TFD_HOME
fi

mkdir -p "$TFD_HOME"/excels
touch "$TFD_HOME"/excels/tables.tex
cat /dev/null > "$TFD_HOME"/excels/tables.tex

for tag in KStemAnchor; do
for set in MQ09 CW09A; do
#   ./run.sh Y -tag "$tag" -collection "$set" -optimize NDCG20 -var 1 -sigma0
    ./run.sh Y -tag "$tag" -collection "$set" -optimize NDCG100 -var 1 -sigma0
    ./run.sh Y -tag "$tag" -collection "$set" -optimize MAP -var 1 -sigma0
done
done
