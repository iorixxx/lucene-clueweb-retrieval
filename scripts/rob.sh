#!/usr/bin/env bash

if [ -z "$TFD_HOME" ]; then
  TFD_HOME=~/TFD_HOME
fi

if [ "$1" = "parameter" ]; then
	RUNS=parameter_runs
    EVALS=parameter_evals
else
	RUNS=runs
    EVALS=evals
fi

echo "Starting Robust Track (ROB04) evaluator with RUNS = $RUNS and EVALS = $EVALS ..."

qrels[4]=qrels.robust2004.txt


for T in 04; do
 printf "%s\n" ${qrels[${T#0}]}
done


for set in ROB04; do
if [ ! -d "${TFD_HOME}/${set}/${RUNS}" ]; then
       continue
fi
for tag in ${TFD_HOME}/${set}/${RUNS}/*; do
if [[ ! -d ${tag} ]]; then
    continue
fi
tag=$(basename "${tag}")
mkdir -p "$TFD_HOME/$set/${EVALS}/$tag"
# TREC Robust Track 2004
for T in 04; do

    if [ ! -d "${TFD_HOME}/${set}/${RUNS}/${tag}/ROB${T}" ]; then
        # Control will enter here if $DIRECTORY does not exist.
        echo "${TFD_HOME}/${set}/${RUNS}/${tag}/ROB${T} does not exist!"
        continue
    fi

     mkdir -p "$TFD_HOME/$set/${EVALS}/$tag/ROB$T"
     mkdir -p "$TFD_HOME/$set/${EVALS}/$tag/ROB$T/trec_eval"

    for f in ${TFD_HOME}/${set}/${RUNS}/${tag}/ROB${T}/*.txt; do
        ${TFD_HOME}/scripts/trec_eval -M1000 -q ${TFD_HOME}/topics-and-qrels/${qrels[${T#0}]} ${f} > "${TFD_HOME}/${set}/${EVALS}/${tag}/ROB${T}/trec_eval/${f##/*/}" &
        for k in 20 100 1000; do
          mkdir -p "$TFD_HOME/$set/${EVALS}/$tag/ROB$T/$k"
          ${TFD_HOME}/scripts/gdeval.pl -k ${k} ${TFD_HOME}/topics-and-qrels/${qrels[${T#0}]} ${f} > "${TFD_HOME}/${set}/${EVALS}/${tag}/ROB${T}/${k}/${f##/*/}" &
        done
    done
    wait

done
done
done