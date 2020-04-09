#!/usr/bin/env bash

# Evaluation script for the NTCIR-13 We Want Web (WWW) Task
# http://www.thuir.cn/ntcirwww/

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

cut_off=(10 20 100 1000)

echo "starting evaluator with RUNS = $RUNS and EVALS = $EVALS ..."

 qrels[13]=qrels.www.1-100.txt
 qrels[14]=qrels.www.101-180.txt


for WWW in 13 14; do
 printf "%s\n" ${qrels[${WWW#}]}
done



for set in NTCIR; do
if [ -d "${TFD_HOME}/${set}/${RUNS}" ]; then

for tag in ${TFD_HOME}/${set}/${RUNS}/*; do
if [[ ! -d ${tag} ]]; then
    continue
fi
tag=$(basename "${tag}")
mkdir -p "$TFD_HOME/$set/${EVALS}/$tag"
for WWW in 13 14; do

    if [ ! -d "${TFD_HOME}/${set}/${RUNS}/${tag}/WWW${WWW}" ]; then
        # Control will enter here if $DIRECTORY does not exist.
        echo "${TFD_HOME}/${set}/${RUNS}/${tag}/WWW${WWW} does not exist!"
        continue
    fi

    mkdir -p "$TFD_HOME/$set/${EVALS}/$tag/WWW$WWW"
    mkdir -p "$TFD_HOME/$set/${EVALS}/$tag/WWW$WWW/trec_eval"

    for f in ${TFD_HOME}/${set}/${RUNS}/${tag}/WWW${WWW}/*.txt; do
        ${TFD_HOME}/scripts/trec_eval -M1000 -q -m all_trec ${TFD_HOME}/topics-and-qrels/${qrels[${WWW}]} ${f} > "${TFD_HOME}/${set}/${EVALS}/${tag}/WWW${WWW}/trec_eval/${f##/*/}" #all measures calculated with the standard TREC
#        ${TFD_HOME}/scripts/trec_eval -M1000 -q ${TFD_HOME}/topics-and-qrels/${qrels[${WWW}]} ${f} > "${TFD_HOME}/${set}/${EVALS}/${tag}/WWW${WWW}/trec_eval/${f##/*/}"
        for k in "${cut_off[@]}"
        do
          mkdir -p "$TFD_HOME/$set/${EVALS}/$tag/WWW$WWW/$k"
          ${TFD_HOME}/scripts/gdeval.pl -k ${k} ${TFD_HOME}/topics-and-qrels/${qrels[${WWW}]} ${f} > "${TFD_HOME}/${set}/${EVALS}/${tag}/WWW${WWW}/${k}/${f##/*/}" 
        done
    done
    wait

done
done
fi
done
wait
