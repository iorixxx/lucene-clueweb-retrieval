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

cut_off=(20 100)

echo "starting evaluator with RUNS = $RUNS and EVALS = $EVALS ..."

 qrels[9]=qrels.web.1-50.txt
 qrels[10]=qrels.web.51-100.txt
 qrels[11]=qrels.web.101-150.txt
 qrels[12]=qrels.web.151-200.txt
 qrels[13]=qrels.web.201-250.txt
 qrels[14]=qrels.web.251-300.txt
 qrels[15]=qrels.web.301-350.txt
 qrels[16]=qrels.web.351-400.txt

for WT in 09 10 11 12 13 14 15 16; do
 printf "%s\n" ${qrels[${WT#0}]}
done

for set in CW09A CW09B; do
if [ ! -d "${TFD_HOME}/${set}/${RUNS}" ]; then
       continue
fi
for tag in ${TFD_HOME}/${set}/${RUNS}/*; do
if [[ ! -d ${tag} ]]; then
    continue
fi
tag=$(basename "${tag}")
mkdir -p "$TFD_HOME/$set/${EVALS}/$tag"
for WT in 09 10 11 12; do

    if [ ! -d "${TFD_HOME}/${set}/${RUNS}/${tag}/WT${WT}" ]; then
        # Control will enter here if $DIRECTORY does not exist.
        echo "${TFD_HOME}/${set}/${RUNS}/${tag}/WT${WT} does not exist!"
        continue
    fi

    mkdir -p "$TFD_HOME/$set/${EVALS}/$tag/WT$WT"
    mkdir -p "$TFD_HOME/$set/${EVALS}/$tag/WT$WT/trec_eval"

    for f in ${TFD_HOME}/${set}/${RUNS}/${tag}/WT${WT}/*.txt; do
        ${TFD_HOME}/scripts/trec_eval -M1000 -q ${TFD_HOME}/topics-and-qrels/${qrels[${WT#0}]} ${f} > "${TFD_HOME}/${set}/${EVALS}/${tag}/WT${WT}/trec_eval/${f##/*/}" &
        for k in "${cut_off[@]}"
        do
          mkdir -p "$TFD_HOME/$set/${EVALS}/$tag/WT$WT/$k"
          ${TFD_HOME}/scripts/gdeval.pl -k ${k} ${TFD_HOME}/topics-and-qrels/${qrels[${WT#0}]} ${f} > "${TFD_HOME}/${set}/${EVALS}/${tag}/WT${WT}/${k}/${f##/*/}" &
        done
    done
    wait

done
done
done

set=CW12B
if [ -d "${TFD_HOME}/${set}/${RUNS}" ]; then

for tag in ${TFD_HOME}/${set}/${RUNS}/*; do
if [[ ! -d ${tag} ]]; then
    continue
fi
tag=$(basename "${tag}")
mkdir -p "$TFD_HOME/$set/${EVALS}/$tag"
for WT in 13 14 15 16; do

    if [ ! -d "${TFD_HOME}/${set}/${RUNS}/${tag}/WT${WT}" ]; then
        # Control will enter here if $DIRECTORY does not exist.
        echo "${TFD_HOME}/${set}/${RUNS}/${tag}/WT${WT} does not exist!"
        continue
    fi

    mkdir -p "$TFD_HOME/$set/${EVALS}/$tag/WT$WT"
    mkdir -p "$TFD_HOME/$set/${EVALS}/$tag/WT$WT/trec_eval"

    for f in ${TFD_HOME}/${set}/${RUNS}/${tag}/WT${WT}/*.txt; do
        ${TFD_HOME}/scripts/trec_eval -M1000 -q ${TFD_HOME}/topics-and-qrels/${qrels[${WT}]} ${f} > "${TFD_HOME}/${set}/${EVALS}/${tag}/WT${WT}/trec_eval/${f##/*/}" &
        for k in "${cut_off[@]}"
        do
          mkdir -p "$TFD_HOME/$set/${EVALS}/$tag/WT$WT/$k"
          ${TFD_HOME}/scripts/gdeval.pl -k ${k} ${TFD_HOME}/topics-and-qrels/${qrels[${WT}]} ${f} > "${TFD_HOME}/${set}/${EVALS}/${tag}/WT${WT}/${k}/${f##/*/}" &
        done
    done
    wait

done
done
fi
wait