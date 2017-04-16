#!/usr/bin/env bash

if [ -z "$TFD_HOME" ]; then
    TFD_HOME=/home/iorixxx/TFD_HOME
fi

export LC_ALL=en_US.UTF-8
export LANGUAGE=en_US.UTF-8

if [ -z "$1" ]; then
	RUNS=runs
    EVALS=evals
else
	RUNS="$1_runs"
    EVALS="$1_evals"
fi

echo "starting MQ09 evaluator with RUNS = $RUNS and EVALS = $EVALS ..."

# TREC 2009 Million Query (1MQ) Track
# http://ir.cis.udel.edu/million/data.html
set=MQ09
for tag in KStemAnalyzer KStemAnalyzerAnchor;
 do
    mkdir -p ${TFD_HOME}/${set}/${EVALS}/${tag}/MQ09
    for f in ${TFD_HOME}/${set}/${RUNS}/${tag}/MQ09/*.txt;
    do
    ${TFD_HOME}/scripts/statAP_MQ_eval_v4.pl -q ${TFD_HOME}/topics-and-qrels/prels.20001-60000 ${f} > ${TFD_HOME}/${set}/${EVALS}/${tag}/MQ09/${f##/*/}
    done
 done
