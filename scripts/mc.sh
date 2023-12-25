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

echo "Starting Milliyet Collection evaluator with RUNS = $RUNS and EVALS = $EVALS ..."

qrels[0]=qrelsMC.txt
#qrels[1]=qrelsUBE.txt

declare -a arr=("MC" "UBE")

for set in MC; do
if [ ! -d "${TFD_HOME}/${set}/${RUNS}" ]; then
       continue
fi
for tag in ${TFD_HOME}/${set}/${RUNS}/*; do
if [[ ! -d ${tag} ]]; then
    continue
fi
tag=$(basename "${tag}")
mkdir -p "${TFD_HOME}/$set/${EVALS}/$tag"

for i in 0; do

if [ ! -d "${TFD_HOME}/${set}/${RUNS}/${tag}/${arr[${i}]}" ]; then
        # Control will enter here if $DIRECTORY does not exist.
        echo "${TFD_HOME}/${set}/${RUNS}/${tag}/MC does not exist!"
        continue
    fi

     mkdir -p "${TFD_HOME}/${set}/${EVALS}/$tag/${arr[${i}]}"
     mkdir -p "${TFD_HOME}/${set}/${EVALS}/$tag/${arr[${i}]}/trec_eval"

    for f in ${TFD_HOME}/${set}/${RUNS}/${tag}/${arr[$i]}/*.txt; do
        ${TFD_HOME}/scripts/trec_eval -M1000 -q ${TFD_HOME}/topics-and-qrels/${qrels[${i}]} ${f} > "${TFD_HOME}/${set}/${EVALS}/${tag}/${arr[$i]}/trec_eval/${f##/*/}" &
        for k in 20 100 1000; do
          mkdir -p "${TFD_HOME}/${set}/${EVALS}/$tag/${arr[$i]}/$k"
          ${TFD_HOME}/scripts/gdeval.pl -k $k ${TFD_HOME}/topics-and-qrels/${qrels[${i}]} $f > "${TFD_HOME}/${set}/${EVALS}/${tag}/${arr[$i]}/$k/${f##/*/}" &
        done
    done
    wait

done
done
done