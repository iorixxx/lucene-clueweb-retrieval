#!/usr/bin/env bash

if [ -z "$TFD_HOME" ]; then
    TFD_HOME=~/TFD_HOME
fi

if [ -z "$1" ]; then
	RUNS=runs
    EVALS=evals
else
	RUNS="$1_runs"
    EVALS="$1_evals"
fi

echo "starting MQ09 evaluator with RUNS = $RUNS and EVALS = $EVALS ..."

prels[9]=prels.catA.1-50


for T in 09; do
 printf "%s\n" ${prels[${T#0}]}
done


for T in 09; do
if [ ! -d "${TFD_HOME}/MQ${T}/${RUNS}" ]; then
       continue
fi
for tag in ${TFD_HOME}/MQ${T}/${RUNS}/*; do
if [[ ! -d ${tag} ]]; then
    continue
fi
tag=$(basename "${tag}")

mkdir -p "$TFD_HOME/MQ${T}/${EVALS}/$tag"




    if [ ! -d "${TFD_HOME}/MQ${T}/${RUNS}/${tag}/MQ${T}" ]; then
        # Control will enter here if $DIRECTORY does not exist.
        echo "${TFD_HOME}/MQ${T}/${RUNS}/${tag}/MQ${T} does not exist!"
        continue
    fi

     mkdir -p "$TFD_HOME/MQ${T}/${EVALS}/$tag/MQ$T"

     for f in ${TFD_HOME}/MQ${T}/${RUNS}/${tag}/MQ${T}/input*;
     do
        ${TFD_HOME}/scripts/statAP_MQ_eval_v4.pl -q ${TFD_HOME}/topics-and-qrels/${prels[${T#0}]} ${f} > "${TFD_HOME}/MQ${T}/${EVALS}/${tag}/MQ${T}/${f##/*/}" &
     done

     wait

done
done