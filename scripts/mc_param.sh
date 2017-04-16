#!/usr/bin/env bash

if [ -z "$TFD_HOME" ]; then
 TFD_HOME=/home/iorixxx/TFD_HOME
fi

export LC_ALL=en_US.UTF-8
export LANGUAGE=en_US.UTF-8

RUNS_DIR=runs
EVALS_DIR=evals
SET=MC

for tag in NS;
do
    mkdir -p "$TFD_HOME/$SET/$EVALS_DIR/$tag/MC"
    mkdir -p "$TFD_HOME/$SET/$EVALS_DIR/$tag/MC/trec_eval"
        for k in 20 100 1000; do
            mkdir -p "$TFD_HOME/$SET/$EVALS_DIR/$tag/MC/$k"
        done

done


for tag in NS;
do

    for f in $TFD_HOME/$SET/$RUNS_DIR/$tag/MC/*.txt; do
        $TFD_HOME/scripts/trec_eval -M1000 -q $TFD_HOME/topics-and-qrels/qrelsMC.txt $f > "$TFD_HOME/$SET/$EVALS_DIR/$tag/MC/trec_eval/${f##/*/}"
        for k in 20 100 1000; do
        $TFD_HOME/scripts/gdeval.pl -k $k $TFD_HOME/topics-and-qrels/qrelsMC.txt $f > "$TFD_HOME/$SET/$EVALS_DIR/$tag/MC/$k/${f##/*/}"
        done
    done

done
