#!/usr/bin/env bash

if [ -z "$TFD_HOME" ]; then
 TFD_HOME=~/TFD_HOME
fi

for set in ROB04 GOV2 MQ09 CW09A CW09B CW12B; do

    if [ -d  "$TFD_HOME"/${set}/results ]; then
        rm -rf  "$TFD_HOME"/${set}/results
    fi

    mkdir -p "$TFD_HOME"/${set}/results

    if [ -d  "$TFD_HOME"/${set}/excels ]; then
        rm -rf  "$TFD_HOME"/${set}/excels
    fi

    mkdir -p "$TFD_HOME"/${set}/excels
done

# ClueWeb (CW) collections
for set in CW09A CW09B CW12B;
do
    for tag in KStemAnalyzer KStemAnalyzerAnchor;
    do
        for op in OR;
        do
            ./run.sh KNN -collection "$set" -op "$op" -tag "$tag" -k 1000 -metric MAP > "$TFD_HOME/"${set}/results/"$tag"MAP"$op".txt
            for k in 20 100;
            do
                for metric in NDCG;
                do
                    ./run.sh KNN -collection "$set" -op "$op" -tag "$tag" -k "$k" -metric "$metric" > "$TFD_HOME/"${set}/results/"$tag$metric$k$op".txt
                done
            done
        done
    done
done


# MQ09 collection
for tag in KStemAnalyzer KStemAnalyzerAnchor;
do
    for op in OR;
    do
        ./run.sh KNN -collection MQ09 -op "$op" -tag "$tag" -k 1000 -metric statAP > "$TFD_HOME/"MQ09/results/"$tag"statAP"$op".txt
    done
done

# GOV2 collection
for op in OR;
do
    ./run.sh KNN -collection GOV2 -op "$op" -tag KStemAnalyzer -k 1000 -metric MAP > "$TFD_HOME/"GOV2/results/KStemAnalyzerMAP"$op".txt
    for k in 20 100;
    do
        for metric in NDCG;
        do
            ./run.sh KNN -collection GOV2 -op "$op" -tag KStemAnalyzer -k "$k" -metric "$metric" > "$TFD_HOME/"GOV2/results/KStemAnalyzer"$metric$k$op".txt
        done
    done
done

# ROB04 collection
for op in OR;
do
    ./run.sh KNN -collection ROB04 -op "$op" -tag KStemAnalyzer -k 1000 -metric MAP > "$TFD_HOME/"ROB04/results/KStemAnalyzerMAP"$op".txt
    for k in 20 100;
    do
        for metric in NDCG;
        do
            ./run.sh KNN -collection ROB04 -op "$op" -tag KStemAnalyzer -k "$k" -metric "$metric" > "$TFD_HOME/"ROB04/results/KStemAnalyzer"$metric$k$op".txt
        done
    done
done
